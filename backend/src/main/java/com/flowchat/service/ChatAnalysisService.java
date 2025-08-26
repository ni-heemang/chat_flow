package com.flowchat.service;

import com.flowchat.entity.AnalysisResult;
import com.flowchat.entity.ChatMessage;
import com.flowchat.event.MessageReceivedEvent;
import com.flowchat.repository.AnalysisResultRepository;
import com.flowchat.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAnalysisService.class);

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TopicClassificationService topicClassificationService;

    @Autowired
    private ConversationFlowService conversationFlowService;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private LlmAnalysisService llmAnalysisService;

    private AnalysisNotificationService analysisNotificationService;

    // 실시간 분석 데이터 저장 (메모리 기반)
    private final Map<Long, Map<String, Integer>> roomKeywordStats = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, AtomicInteger>> roomUserMessageCount = new ConcurrentHashMap<>(); // nickname 기반
    private final Map<Long, Map<Integer, AtomicInteger>> roomHourlyStats = new ConcurrentHashMap<>();

    // 한국어 불용어 목록
    private static final Set<String> STOP_WORDS = Set.of(
        "이", "가", "을", "를", "에", "의", "는", "은", "와", "과", "로", "으로", "에서", "부터", "까지", "한테", "께",
        "그", "그것", "저", "저것", "이것", "것", "수", "때", "곳", "데", "점", "등",
        "하다", "되다", "있다", "없다", "같다", "다르다", "크다", "작다", "좋다", "나쁘다",
        "네", "예", "아니", "말", "정말", "진짜", "아", "어", "음", "흠", "오", "우",
        "그런데", "그러나", "하지만", "그리고", "또", "또한", "그래서", "따라서", "즉", "즉시", "바로"
    );

    // 단어 분리 패턴 (공백, 구두점 등)
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\s\\p{Punct}]+");

    @EventListener
    @Async
    public void analyzeMessage(MessageReceivedEvent event) {
        try {
            ChatMessage message = event.getMessage();
            Long roomId = event.getRoomId();
            String username = event.getUsername();
            String nickname = message.getName(); // nickname 사용

            logger.debug("LLM 기반 메시지 분석 시작: roomId={}, username={}, nickname={}, content={}", 
                        roomId, username, nickname, message.getContent());

            // 시스템 메시지는 분석하지 않음
            if (message.getMessageType() == ChatMessage.MessageType.SYSTEM) {
                return;
            }

            // LLM 기반 종합 분석 (비동기)
            llmAnalysisService.analyzeMessage(message.getContent())
                .thenAccept(analysisResult -> {
                    try {
                        // LLM 분석 결과에서 키워드 추출
                        @SuppressWarnings("unchecked")
                        List<String> keywords = (List<String>) analysisResult.getOrDefault("keywords", List.of());
                        updateKeywordStats(roomId, keywords);

                        // 참여도 분석 (nickname 사용)
                        updateUserParticipation(roomId, nickname != null ? nickname : username);

                        // 시간대별 분석
                        updateHourlyStats(roomId, message.getTimestamp());

                        // LLM 분석 결과에서 주제/감정 추출
                        String topic = (String) analysisResult.getOrDefault("topic", "기타");
                        String emotion = (String) analysisResult.getOrDefault("emotion", "중립");
                        
                        // 대화 흐름 분석
                        conversationFlowService.analyzeConversationFlow(roomId, message, username);

                        // LLM 기반 분석 결과 저장
                        if (!keywords.isEmpty()) {
                            saveLlmAnalysisResult(roomId, message, analysisResult, keywords, topic, emotion);
                        }

                        // 실시간 분석 결과 알림 전송
                        getAnalysisNotificationService().onMessageReceived(roomId);

                        // 캐시 무효화
                        evictAnalysisCache(roomId);

                        logger.debug("LLM 메시지 분석 완료: roomId={}, keywords={}, topic={}, emotion={}", 
                                    roomId, keywords, topic, emotion);
                    } catch (Exception e) {
                        logger.error("LLM 분석 결과 처리 실패: roomId={}, error={}", roomId, e.getMessage(), e);
                        // 폴백: 기존 정적 분석 수행
                        performFallbackAnalysis(roomId, message, username);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("LLM 분석 실패, 폴백 분석 수행: roomId={}, error={}", roomId, throwable.getMessage());
                    // 폴백: 기존 정적 분석 수행
                    performFallbackAnalysis(roomId, message, username);
                    return null;
                });

        } catch (Exception e) {
            logger.error("메시지 분석 중 오류 발생: roomId={}, error={}", 
                        event.getRoomId(), e.getMessage(), e);
        }
    }

    /**
     * 키워드 추출 로직 (폴백용 - 기존 정적 방식)
     */
    private List<String> extractKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }

        // 텍스트를 단어로 분리
        String[] words = WORD_PATTERN.split(content.toLowerCase().trim());
        
        // 단어 빈도 계산
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (String word : words) {
            word = word.trim();
            
            // 빈 문자열, 한 글자, 불용어 제외
            if (word.length() < 2 || STOP_WORDS.contains(word)) {
                continue;
            }
            
            // 숫자만으로 구성된 단어 제외
            if (word.matches("\\d+")) {
                continue;
            }
            
            wordCount.merge(word, 1, Integer::sum);
        }

        // 상위 10개 키워드 추출 (빈도순)
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * LLM 기반 키워드 추출 (새로운 방식)
     */
    public CompletableFuture<List<String>> extractKeywordsWithLlm(String content) {
        return llmAnalysisService.extractKeywords(content)
                .exceptionally(throwable -> {
                    logger.warn("LLM 키워드 추출 실패, 폴백 사용: {}", throwable.getMessage());
                    return extractKeywords(content); // 폴백
                });
    }

    /**
     * 키워드 추출 및 카운트 (기간별 분석용)
     */
    private void extractAndCountKeywords(String content, Map<String, Integer> keywordStats) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 텍스트를 단어로 분리
        String[] words = WORD_PATTERN.split(content.toLowerCase().trim());
        
        for (String word : words) {
            word = word.trim();
            
            // 빈 문자열, 한 글자, 불용어 제외
            if (word.length() < 2 || STOP_WORDS.contains(word)) {
                continue;
            }
            
            // 숫자만으로 구성된 단어 제외
            if (word.matches("\\d+")) {
                continue;
            }
            
            keywordStats.merge(word, 1, Integer::sum);
        }
    }

    /**
     * LLM 기반 주제 분류
     */
    public CompletableFuture<String> classifyTopicWithLlm(String content) {
        return llmAnalysisService.classifyTopic(content)
                .exceptionally(throwable -> {
                    logger.warn("LLM 주제 분류 실패, 폴백 사용: {}", throwable.getMessage());
                    return topicClassificationService.classifyTopic(content); // 폴백
                });
    }

    /**
     * LLM 기반 감정 분석
     */
    public CompletableFuture<Map<String, Object>> analyzeEmotionWithLlm(String content) {
        return llmAnalysisService.analyzeEmotion(content)
                .exceptionally(throwable -> {
                    logger.warn("LLM 감정 분석 실패, 폴백 사용: {}", throwable.getMessage());
                    String emotion = topicClassificationService.analyzeEmotion(content);
                    return Map.of(
                        "emotion", emotion,
                        "intensity", "보통",
                        "score", 0.0
                    ); // 폴백
                });
    }

    /**
     * 채팅방별 키워드 통계 업데이트
     */
    private void updateKeywordStats(Long roomId, List<String> keywords) {
        Map<String, Integer> keywordStats = roomKeywordStats.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        
        for (String keyword : keywords) {
            keywordStats.merge(keyword, 1, Integer::sum);
        }
    }

    /**
     * 사용자별 참여도 통계 업데이트
     */
    private void updateUserParticipation(Long roomId, String username) {
        Map<String, AtomicInteger> userStats = roomUserMessageCount.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        userStats.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 시간대별 통계 업데이트
     */
    private void updateHourlyStats(Long roomId, LocalDateTime createdAt) {
        Map<Integer, AtomicInteger> hourlyStats = roomHourlyStats.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        int hour = createdAt.getHour();
        hourlyStats.computeIfAbsent(hour, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 폴백 분석 수행 (LLM 실패 시)
     */
    private void performFallbackAnalysis(Long roomId, ChatMessage message, String username) {
        try {
            String nickname = message.getName(); // nickname 사용
            
            // 기존 정적 분석 방식 수행
            List<String> keywords = extractKeywords(message.getContent());
            updateKeywordStats(roomId, keywords);
            updateUserParticipation(roomId, nickname != null ? nickname : username);
            updateHourlyStats(roomId, message.getTimestamp());

            String topic = topicClassificationService.classifyTopic(message.getContent());
            String emotion = topicClassificationService.analyzeEmotion(message.getContent());
            
            conversationFlowService.analyzeConversationFlow(roomId, message, username);

            if (!keywords.isEmpty()) {
                saveAdvancedAnalysisResult(roomId, message, keywords, topic, emotion);
            }

            getAnalysisNotificationService().onMessageReceived(roomId);
            evictAnalysisCache(roomId);
            
            logger.debug("폴백 분석 완료: roomId={}, nickname={}, keywords={}", roomId, nickname, keywords);
        } catch (Exception e) {
            logger.error("폴백 분석 실패: roomId={}, error={}", roomId, e.getMessage(), e);
        }
    }

    /**
     * LLM 기반 분석 결과 데이터베이스 저장
     */
    private void saveLlmAnalysisResult(Long roomId, ChatMessage message, Map<String, Object> analysisResult,
                                      List<String> keywords, String topic, String emotion) {
        try {
            // LLM 분석 결과를 포함한 JSON 생성
            String llmAnalysisJson = String.format(
                "{\"keywords\":[%s],\"topic\":\"%s\",\"emotion\":\"%s\",\"messageId\":%d,\"llm_analysis\":%s}",
                keywords.stream()
                    .map(keyword -> "\"" + keyword + "\"")
                    .collect(Collectors.joining(",")),
                topic,
                emotion,
                message.getId(),
                convertAnalysisResultToJson(analysisResult)
            );
            
            LocalDateTime now = LocalDateTime.now();
            
            // LLM 분석 결과 저장
            AnalysisResult llmResult = AnalysisResult.createTopicAnalysis(
                roomId,
                llmAnalysisJson,
                now.minusMinutes(1),
                now
            );

            analysisResultRepository.save(llmResult);
            
            logger.debug("LLM 분석 결과 저장 완료: roomId={}, topic={}, emotion={}", 
                        roomId, topic, emotion);
            
        } catch (Exception e) {
            logger.error("LLM 분석 결과 저장 실패: roomId={}, messageId={}, error={}", 
                        roomId, message.getId(), e.getMessage());
        }
    }

    /**
     * LLM 분석 결과를 JSON으로 변환
     */
    private String convertAnalysisResultToJson(Map<String, Object> analysisResult) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(analysisResult);
        } catch (Exception e) {
            logger.warn("LLM 분석 결과 JSON 변환 실패: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 분석 결과 데이터베이스 저장 (기존 방식 - 폴백용)
     */
    private void saveAnalysisResult(Long roomId, ChatMessage message, List<String> keywords) {
        try {
            // JSON 형식으로 키워드 저장
            String keywordsJson = "{\"keywords\":[" + 
                keywords.stream()
                    .map(keyword -> "\"" + keyword + "\"")
                    .collect(Collectors.joining(",")) + 
                "]}";
            
            LocalDateTime now = LocalDateTime.now();
            
            AnalysisResult result = AnalysisResult.createKeywordAnalysis(
                roomId, 
                keywordsJson, 
                1, // 단일 메시지 분석
                1, // 단일 사용자 분석
                now.minusMinutes(1), // 분석 시작 시간
                now  // 분석 종료 시간
            );

            analysisResultRepository.save(result);
            
        } catch (Exception e) {
            logger.error("분석 결과 저장 실패: roomId={}, messageId={}, error={}", 
                        roomId, message.getId(), e.getMessage());
        }
    }

    /**
     * 심화 분석 결과 데이터베이스 저장
     */
    private void saveAdvancedAnalysisResult(Long roomId, ChatMessage message, List<String> keywords, String topic, String emotion) {
        try {
            // 종합 분석 데이터를 JSON으로 저장
            String analysisJson = String.format(
                "{\"keywords\":[%s],\"topic\":\"%s\",\"emotion\":\"%s\",\"messageId\":%d}",
                keywords.stream()
                    .map(keyword -> "\"" + keyword + "\"")
                    .collect(Collectors.joining(",")),
                topic,
                emotion,
                message.getId()
            );
            
            LocalDateTime now = LocalDateTime.now();
            
            // 주제 분류 결과 저장
            AnalysisResult topicResult = AnalysisResult.createTopicAnalysis(
                roomId,
                analysisJson,
                now.minusMinutes(1),
                now
            );

            analysisResultRepository.save(topicResult);
            
            logger.debug("심화 분석 결과 저장 완료: roomId={}, topic={}, emotion={}", 
                        roomId, topic, emotion);
            
        } catch (Exception e) {
            logger.error("심화 분석 결과 저장 실패: roomId={}, messageId={}, error={}", 
                        roomId, message.getId(), e.getMessage());
        }
    }

    /**
     * 채팅방의 실시간 키워드 통계 조회 (캐싱 적용)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roomKeywordStats", key = "#roomId")
    public Map<String, Object> getRoomKeywordStats(Long roomId) {
        Map<String, Integer> keywordStats = roomKeywordStats.getOrDefault(roomId, new HashMap<>());
        
        // 상위 10개 키워드 추출
        List<Map<String, Object>> topKeywords = keywordStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> keywordInfo = new HashMap<>();
                    keywordInfo.put("keyword", entry.getKey());
                    keywordInfo.put("count", entry.getValue());
                    return keywordInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("topKeywords", topKeywords);
        result.put("totalKeywords", keywordStats.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 채팅방의 사용자 참여도 통계 조회 (캐싱 적용)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roomParticipationStats", key = "#roomId")
    public Map<String, Object> getRoomUserParticipation(Long roomId) {
        Map<String, AtomicInteger> userStats = roomUserMessageCount.getOrDefault(roomId, new HashMap<>());
        
        List<Map<String, Object>> userParticipation = userStats.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                    (a, b) -> Integer.compare(b.get(), a.get())))
                .map(entry -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("username", entry.getKey()); // 실제로는 nickname이 저장됨
                    userInfo.put("messageCount", entry.getValue().get());
                    return userInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("userParticipation", userParticipation);
        result.put("totalUsers", userStats.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 채팅방의 시간대별 활동 통계 조회 (캐싱 적용)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roomHourlyStats", key = "#roomId")
    public Map<String, Object> getRoomHourlyStats(Long roomId) {
        Map<Integer, AtomicInteger> hourlyStats = roomHourlyStats.getOrDefault(roomId, new HashMap<>());
        
        List<Map<String, Object>> hourlyActivity = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourInfo = new HashMap<>();
            hourInfo.put("hour", hour);
            hourInfo.put("messageCount", hourlyStats.getOrDefault(hour, new AtomicInteger(0)).get());
            hourlyActivity.add(hourInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("hourlyActivity", hourlyActivity);
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 기간별 키워드 분석 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomKeywordStatsByPeriod(Long roomId, Integer days) {
        LocalDateTime cutoffTime = days != null ? LocalDateTime.now().minusDays(days) : null;
        
        logger.debug("기간별 키워드 분석: roomId={}, days={}, cutoffTime={}", roomId, days, cutoffTime);
        
        // 데이터베이스에서 기간에 해당하는 메시지들 조회
        List<ChatMessage> messages;
        if (cutoffTime != null) {
            messages = chatMessageRepository.findByRoomIdAndTimestampAfterAndIsDeletedFalse(roomId, cutoffTime);
        } else {
            messages = chatMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
        }
        
        // 키워드 분석 수행
        Map<String, Integer> keywordStats = new HashMap<>();
        for (ChatMessage message : messages) {
            if (message.getMessageType() == ChatMessage.MessageType.TEXT) {
                extractAndCountKeywords(message.getContent(), keywordStats);
            }
        }
        
        // 상위 10개 키워드 추출
        List<Map<String, Object>> topKeywords = keywordStats.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> keywordInfo = new HashMap<>();
                    keywordInfo.put("keyword", entry.getKey());
                    keywordInfo.put("count", entry.getValue());
                    return keywordInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("topKeywords", topKeywords);
        result.put("totalKeywords", keywordStats.size());
        result.put("period", days != null ? days + "일" : "전체 기간");
        result.put("messageCount", messages.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 기간별 참여도 분석 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomUserParticipationByPeriod(Long roomId, Integer days) {
        LocalDateTime cutoffTime = days != null ? LocalDateTime.now().minusDays(days) : null;
        
        logger.debug("기간별 참여도 분석: roomId={}, days={}, cutoffTime={}", roomId, days, cutoffTime);
        
        // 데이터베이스에서 기간에 해당하는 메시지들 조회
        List<ChatMessage> messages;
        if (cutoffTime != null) {
            messages = chatMessageRepository.findByRoomIdAndTimestampAfterAndIsDeletedFalse(roomId, cutoffTime);
        } else {
            messages = chatMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
        }
        
        // 사용자별 메시지 수 계산 (nickname 우선 사용)
        Map<String, Integer> userMessageCount = new HashMap<>();
        for (ChatMessage message : messages) {
            if (message.getMessageType() != ChatMessage.MessageType.SYSTEM) {
                String displayName = message.getName() != null ? message.getName() : message.getUsername();
                if (displayName != null) {
                    userMessageCount.merge(displayName, 1, Integer::sum);
                }
            }
        }
        
        List<Map<String, Object>> userParticipation = userMessageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("username", entry.getKey()); // 실제로는 nickname이 우선 표시됨
                    userInfo.put("messageCount", entry.getValue());
                    return userInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("userParticipation", userParticipation);
        result.put("totalUsers", userMessageCount.size());
        result.put("period", days != null ? days + "일" : "전체 기간");
        result.put("messageCount", messages.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 기간별 시간대별 활동 분석 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomHourlyStatsByPeriod(Long roomId, Integer days) {
        LocalDateTime cutoffTime = days != null ? LocalDateTime.now().minusDays(days) : null;
        
        logger.debug("기간별 시간대별 분석: roomId={}, days={}, cutoffTime={}", roomId, days, cutoffTime);
        
        // 데이터베이스에서 기간에 해당하는 메시지들 조회
        List<ChatMessage> messages;
        if (cutoffTime != null) {
            messages = chatMessageRepository.findByRoomIdAndTimestampAfterAndIsDeletedFalse(roomId, cutoffTime);
        } else {
            messages = chatMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
        }
        
        // 시간대별 메시지 수 계산
        Map<Integer, Integer> hourlyMessageCount = new HashMap<>();
        for (ChatMessage message : messages) {
            if (message.getMessageType() != ChatMessage.MessageType.SYSTEM) {
                int hour = message.getTimestamp().getHour();
                hourlyMessageCount.merge(hour, 1, Integer::sum);
            }
        }
        
        List<Map<String, Object>> hourlyActivity = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourInfo = new HashMap<>();
            hourInfo.put("hour", hour);
            hourInfo.put("messageCount", hourlyMessageCount.getOrDefault(hour, 0));
            hourlyActivity.add(hourInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("hourlyActivity", hourlyActivity);
        result.put("period", days != null ? days + "일" : "전체 기간");
        result.put("messageCount", messages.size());
        result.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }

    /**
     * 채팅방 분석 데이터 초기화 (테스트용) - 캐시도 함께 삭제
     */
    @CacheEvict(value = {"roomKeywordStats", "roomParticipationStats", "roomHourlyStats", "roomAnalysisSummary", "advancedAnalysisSummary"}, key = "#roomId")
    public void clearRoomAnalysis(Long roomId) {
        roomKeywordStats.remove(roomId);
        roomUserMessageCount.remove(roomId);
        roomHourlyStats.remove(roomId);
        
        // 심화 분석 데이터도 함께 초기화
        conversationFlowService.clearConversationData(roomId);
        
        logger.info("채팅방 분석 데이터 초기화 완료: roomId={}", roomId);
    }

    /**
     * 캐시 무효화 (새로운 메시지로 인한 분석 데이터 변경)
     */
    @CacheEvict(value = {"roomKeywordStats", "roomParticipationStats", "roomHourlyStats", "roomAnalysisSummary"}, key = "#roomId")
    private void evictAnalysisCache(Long roomId) {
        // 캐시 무효화만 수행, 실제 로직은 없음
        logger.debug("분석 캐시 무효화: roomId={}", roomId);
    }

    /**
     * 주기적 분석 결과 저장 (매 시간마다)
     */
    @Scheduled(cron = "0 0 * * * ?") // 매 시간 정각
    public void saveHourlyAnalysisResults() {
        logger.info("주기적 분석 결과 저장 시작");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            // 활성화된 채팅방들의 분석 결과 저장
            for (Long roomId : roomKeywordStats.keySet()) {
                try {
                    savePeriodicAnalysisResult(roomId, oneHourAgo, now);
                    logger.debug("주기적 분석 결과 저장 완료: roomId={}", roomId);
                } catch (Exception e) {
                    logger.error("주기적 분석 결과 저장 실패: roomId={}, error={}", roomId, e.getMessage());
                }
            }
            
            logger.info("주기적 분석 결과 저장 완료: 총 {}개 채팅방", roomKeywordStats.size());
            
        } catch (Exception e) {
            logger.error("주기적 분석 결과 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 주기적 분석 결과 데이터베이스 저장
     */
    private void savePeriodicAnalysisResult(Long roomId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        Map<String, Object> keywordStats = getRoomKeywordStats(roomId);
        Map<String, Object> participationStats = getRoomUserParticipation(roomId);
        Map<String, Object> hourlyStats = getRoomHourlyStats(roomId);
        
        // 키워드 분석 결과 저장
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topKeywords = (List<Map<String, Object>>) keywordStats.get("topKeywords");
        if (topKeywords != null && !topKeywords.isEmpty()) {
            String keywordsJson = topKeywords.stream()
                .map(kw -> kw.get("keyword") + ":" + kw.get("count"))
                .collect(Collectors.joining(","));
            
            AnalysisResult keywordResult = AnalysisResult.createKeywordAnalysis(
                roomId,
                keywordsJson,
                topKeywords.size(),
                (Integer) participationStats.get("totalUsers"),
                periodStart,
                periodEnd
            );
            
            analysisResultRepository.save(keywordResult);
        }
        
        // 시간대별 분석 결과 저장
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hourlyActivity = (List<Map<String, Object>>) hourlyStats.get("hourlyActivity");
        if (hourlyActivity != null) {
            String hourlyJson = hourlyActivity.stream()
                .filter(hour -> (Integer) hour.get("messageCount") > 0)
                .map(hour -> hour.get("hour") + ":" + hour.get("messageCount"))
                .collect(Collectors.joining(","));
            
            if (!hourlyJson.isEmpty()) {
                AnalysisResult hourlyResult = AnalysisResult.createTimePatternAnalysis(
                    roomId,
                    hourlyJson,
                    periodStart,
                    periodEnd
                );
                
                analysisResultRepository.save(hourlyResult);
            }
        }
        
        // 참여도 분석 결과 저장
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userParticipation = (List<Map<String, Object>>) participationStats.get("userParticipation");
        if (userParticipation != null && !userParticipation.isEmpty()) {
            String participationJson = userParticipation.stream()
                .map(user -> user.get("username") + ":" + user.get("messageCount"))
                .collect(Collectors.joining(","));
            
            AnalysisResult participationResult = AnalysisResult.createParticipationAnalysis(
                roomId,
                participationJson,
                (Integer) participationStats.get("totalUsers"),
                periodStart,
                periodEnd
            );
            
            analysisResultRepository.save(participationResult);
        }
    }

    /**
     * 기존 메시지를 기반으로 채팅방 분석 데이터 재구축
     */
    public void rebuildRoomAnalysis(Long roomId) {
        rebuildRoomAnalysis(roomId, null);
    }
    
    /**
     * 기간별 채팅방 분석 데이터 재구축
     */
    public void rebuildRoomAnalysis(Long roomId, Integer days) {
        logger.info("채팅방 분석 데이터 재구축 시작: roomId={}, days={}", roomId, days);
        
        // 기존 통계 초기화
        roomKeywordStats.remove(roomId);
        roomUserMessageCount.remove(roomId);
        roomHourlyStats.remove(roomId);
        
        // 기간별 메시지 조회
        List<ChatMessage> messages;
        if (days != null && days > 0) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            messages = chatMessageRepository.findByRoomIdAndMessageTypeAndIsDeletedFalseAndTimestampAfterOrderByTimestampDesc(
                roomId, ChatMessage.MessageType.TEXT, startDate);
        } else {
            // 전체 기간
            messages = chatMessageRepository.findByRoomIdAndMessageTypeAndIsDeletedFalseOrderByTimestampDesc(
                roomId, ChatMessage.MessageType.TEXT);
        }
        
        logger.info("분석할 메시지 수: {} (기간: {}일)", messages.size(), days != null ? days : "전체");
        
        for (ChatMessage message : messages) {
            // 키워드 분석
            List<String> keywords = extractKeywords(message.getContent());
            updateKeywordStats(roomId, keywords);
            
            // 사용자 참여도 분석 (nickname 사용)
            String nickname = message.getName();
            String username = message.getUsername();
            String displayName = nickname != null ? nickname : username;
            
            if (displayName != null) {
                updateUserParticipation(roomId, displayName);
                
                // 시간대별 활동 분석
                updateHourlyStats(roomId, message.getTimestamp());
            }
        }
        
        logger.info("채팅방 분석 데이터 재구축 완료: roomId={}", roomId);
    }
    
    /**
     * 모든 채팅방의 분석 데이터 재구축
     */
    public void rebuildAllRoomAnalysis() {
        logger.info("모든 채팅방 분석 데이터 재구축 시작");
        
        // 모든 메시지가 있는 채팅방 조회 (시간 제한 없음)
        List<Long> roomIds = chatMessageRepository.findAll()
            .stream()
            .filter(message -> !message.getIsDeleted())
            .map(ChatMessage::getRoomId)
            .distinct()
            .collect(Collectors.toList());
        
        for (Long roomId : roomIds) {
            rebuildRoomAnalysis(roomId);
        }
        
        logger.info("모든 채팅방 분석 데이터 재구축 완료: {}개 채팅방", roomIds.size());
    }

    /**
     * 애플리케이션 시작 시 분석 데이터 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAnalysisDataOnStartup() {
        logger.info("애플리케이션 시작 - 분석 데이터 초기화 시작");
        
        try {
            // 최근 7일간 활성화된 채팅방 조회
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<Long> activeRoomIds = chatMessageRepository.findByTimestampBetween(
                sevenDaysAgo, LocalDateTime.now())
                .stream()
                .map(ChatMessage::getRoomId)
                .distinct()
                .collect(Collectors.toList());
            
            logger.info("최근 활성 채팅방 {}개 발견", activeRoomIds.size());
            
            // 각 활성 채팅방에 대해 분석 데이터 재구축
            for (Long roomId : activeRoomIds) {
                try {
                    rebuildRoomAnalysis(roomId);
                    logger.debug("채팅방 {} 분석 데이터 초기화 완료", roomId);
                } catch (Exception e) {
                    logger.error("채팅방 {} 분석 데이터 초기화 실패: {}", roomId, e.getMessage());
                }
            }
            
            logger.info("분석 데이터 초기화 완료 - 총 {}개 채팅방 처리", activeRoomIds.size());
            
        } catch (Exception e) {
            logger.error("분석 데이터 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅방에 저장된 분석 결과가 있는지 확인하고 메모리에 로드
     */
    public void loadStoredAnalysisData(Long roomId) {
        try {
            logger.debug("저장된 분석 데이터 로드 시도: roomId={}", roomId);
            
            // 최근 24시간 내 분석 결과 조회
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<AnalysisResult> recentResults = analysisResultRepository
                .findByRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(roomId, yesterday);
            
            if (!recentResults.isEmpty()) {
                logger.debug("채팅방 {}에서 {}개의 저장된 분석 결과 발견", roomId, recentResults.size());
                
                // 키워드 분석 결과 복원
                restoreKeywordDataFromResults(roomId, recentResults);
                
                logger.debug("저장된 분석 데이터 로드 완료: roomId={}", roomId);
            } else {
                logger.debug("저장된 분석 데이터 없음, 새로 구축: roomId={}", roomId);
                rebuildRoomAnalysis(roomId);
            }
            
        } catch (Exception e) {
            logger.error("저장된 분석 데이터 로드 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }
    
    /**
     * 데이터베이스에 저장된 분석 결과에서 키워드 데이터 복원
     */
    private void restoreKeywordDataFromResults(Long roomId, List<AnalysisResult> results) {
        Map<String, Integer> keywordStats = roomKeywordStats.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        
        for (AnalysisResult result : results) {
            if (result.getAnalysisType() == AnalysisResult.AnalysisType.KEYWORD_FREQUENCY ||
                result.getAnalysisType() == AnalysisResult.AnalysisType.TOPIC_CLASSIFICATION) {
                
                try {
                    String data = result.getAnalysisData();
                    if (data.contains("keywords")) {
                        // 간단한 키워드 추출 (실제로는 JSON 파서 사용 권장)
                        extractKeywordsFromJson(data).forEach(keyword -> 
                            keywordStats.merge(keyword, 1, Integer::sum));
                    }
                } catch (Exception e) {
                    logger.debug("분석 결과 파싱 실패: {}", e.getMessage());
                }
            }
        }
        
        logger.debug("복원된 키워드 수: {}", keywordStats.size());
    }
    
    /**
     * JSON 문자열에서 키워드 추출 (간단한 파서)
     */
    private List<String> extractKeywordsFromJson(String json) {
        List<String> keywords = new ArrayList<>();
        try {
            // "keywords":["word1","word2"] 패턴 찾기
            int start = json.indexOf("\"keywords\":[");
            if (start != -1) {
                int end = json.indexOf("]", start);
                if (end != -1) {
                    String keywordSection = json.substring(start + 12, end);
                    String[] parts = keywordSection.split(",");
                    for (String part : parts) {
                        String keyword = part.trim().replace("\"", "");
                        if (!keyword.isEmpty()) {
                            keywords.add(keyword);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("키워드 추출 실패: {}", e.getMessage());
        }
        return keywords;
    }

    /**
     * 채팅방 목적 분석 (LLM 기반)
     */
    public Map<String, Object> analyzeRoomPurpose(Long roomId) {
        logger.info("채팅방 목적 분석 시작: roomId={}", roomId);
        
        try {
            // 최근 50개 메시지 조회
            List<ChatMessage> recentMessages = chatMessageRepository
                .findByRoomIdAndMessageTypeAndIsDeletedFalseOrderByTimestampDesc(
                    roomId, ChatMessage.MessageType.TEXT)
                .stream()
                .limit(50)
                .collect(Collectors.toList());
            
            if (recentMessages.isEmpty()) {
                return createEmptyPurposeAnalysis(roomId, "분석할 메시지가 없습니다.");
            }
            
            // 메시지 내용 결합
            String combinedContent = recentMessages.stream()
                .map(ChatMessage::getContent)
                .collect(Collectors.joining(" "));
            
            // LLM을 통한 목적 분석
            String prompt = String.format(
                "다음 채팅방 대화 내용을 분석하여 이 채팅방의 주요 목적을 한 문장으로 설명해주세요. " +
                "대화 내용: \"%s\"", 
                combinedContent.length() > 1000 ? combinedContent.substring(0, 1000) + "..." : combinedContent
            );
            
            CompletableFuture<Map<String, Object>> analysis = llmAnalysisService.analyzeCustomPrompt(prompt);
            Map<String, Object> result = analysis.get();
            
            String purpose = (String) result.getOrDefault("purpose", "이 채팅방은 다양한 주제로 소통하는 공간입니다.");
            
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("purpose", purpose);
            response.put("confidence", result.getOrDefault("confidence", 0.8));
            response.put("analyzedMessages", recentMessages.size());
            response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("채팅방 목적 분석 완료: roomId={}, purpose={}", roomId, purpose);
            return response;
            
        } catch (Exception e) {
            logger.error("채팅방 목적 분석 실패: roomId={}, error={}", roomId, e.getMessage());
            return createEmptyPurposeAnalysis(roomId, "목적 분석 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 채팅방 활발한 시간대 분석
     */
    public Map<String, Object> analyzeRoomPeakHours(Long roomId) {
        logger.info("채팅방 활발한 시간대 분석 시작: roomId={}", roomId);
        
        try {
            // 최근 7일간의 메시지 조회
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            List<ChatMessage> recentMessages = chatMessageRepository
                .findByRoomIdAndTimestampAfterAndIsDeletedFalse(roomId, weekAgo);
            
            if (recentMessages.isEmpty()) {
                return createEmptyPeakHoursAnalysis(roomId, "분석할 메시지가 없습니다.");
            }
            
            // 시간대별 메시지 수 집계
            Map<Integer, Integer> hourlyCount = new HashMap<>();
            for (ChatMessage message : recentMessages) {
                if (message.getMessageType() != ChatMessage.MessageType.SYSTEM) {
                    int hour = message.getTimestamp().getHour();
                    hourlyCount.merge(hour, 1, Integer::sum);
                }
            }
            
            // 가장 활발한 시간대 찾기
            List<Map.Entry<Integer, Integer>> sortedHours = hourlyCount.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            
            if (sortedHours.isEmpty()) {
                response.put("peakHour", "정보 없음");
                response.put("peakHourDescription", "활동 패턴을 분석할 수 없습니다.");
            } else {
                int peakHour = sortedHours.get(0).getKey();
                int peakCount = sortedHours.get(0).getValue();
                
                String description = String.format(
                    "%d시가 가장 활발한 시간대입니다 (%d개 메시지)", 
                    peakHour, peakCount
                );
                
                // 추가 인사이트
                List<String> insights = new ArrayList<>();
                if (peakHour >= 9 && peakHour <= 18) {
                    insights.add("업무 시간대에 활발한 소통이 이루어집니다.");
                } else if (peakHour >= 19 && peakHour <= 23) {
                    insights.add("저녁 시간대에 활발한 대화가 이루어집니다.");
                } else {
                    insights.add("특별한 시간대에 소통이 활발합니다.");
                }
                
                response.put("peakHour", peakHour + ":00");
                response.put("peakHourDescription", description);
                response.put("insights", insights);
                response.put("hourlyDistribution", createHourlyDistribution(hourlyCount));
            }
            
            response.put("totalMessages", recentMessages.size());
            response.put("analysisPeriod", "최근 7일");
            response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.info("채팅방 활발한 시간대 분석 완료: roomId={}", roomId);
            return response;
            
        } catch (Exception e) {
            logger.error("채팅방 활발한 시간대 분석 실패: roomId={}, error={}", roomId, e.getMessage());
            return createEmptyPeakHoursAnalysis(roomId, "활발한 시간대 분석 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 빈 목적 분석 결과 생성
     */
    private Map<String, Object> createEmptyPurposeAnalysis(Long roomId, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("purpose", "분석할 수 있는 데이터가 부족합니다.");
        response.put("confidence", 0.0);
        response.put("analyzedMessages", 0);
        response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("error", message);
        return response;
    }
    
    /**
     * 빈 활발한 시간대 분석 결과 생성
     */
    private Map<String, Object> createEmptyPeakHoursAnalysis(Long roomId, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("peakHour", "정보 없음");
        response.put("peakHourDescription", "활동 패턴을 분석할 수 없습니다.");
        response.put("insights", List.of("분석할 데이터가 부족합니다."));
        response.put("totalMessages", 0);
        response.put("analysisPeriod", "최근 7일");
        response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("error", message);
        return response;
    }
    
    /**
     * 시간대별 분포 데이터 생성
     */
    private List<Map<String, Object>> createHourlyDistribution(Map<Integer, Integer> hourlyCount) {
        List<Map<String, Object>> distribution = new ArrayList<>();
        
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", hour);
            hourData.put("count", hourlyCount.getOrDefault(hour, 0));
            distribution.add(hourData);
        }
        
        return distribution;
    }

    /**
     * 순환 의존성 해결을 위한 지연 로딩
     */
    private AnalysisNotificationService getAnalysisNotificationService() {
        if (analysisNotificationService == null) {
            analysisNotificationService = applicationContext.getBean(AnalysisNotificationService.class);
        }
        return analysisNotificationService;
    }
}