package com.flowchat.service;

import com.flowchat.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConversationFlowService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationFlowService.class);

    // 채팅방별 대화 세션 관리
    private final Map<Long, List<ConversationSession>> roomConversations = new ConcurrentHashMap<>();
    
    // 대화 세션 임계값 (분)
    private static final long SESSION_BREAK_MINUTES = 10;
    
    // 활발한 대화 임계값 (초)
    private static final long ACTIVE_CONVERSATION_SECONDS = 30;

    /**
     * 대화 세션 클래스
     */
    public static class ConversationSession {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<ChatMessage> messages;
        private Set<String> participants;
        private Duration totalDuration;
        private double averageResponseTime;
        
        public ConversationSession(LocalDateTime startTime) {
            this.startTime = startTime;
            this.messages = new ArrayList<>();
            this.participants = new HashSet<>();
        }
        
        // Getters
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public List<ChatMessage> getMessages() { return messages; }
        public Set<String> getParticipants() { return participants; }
        public Duration getTotalDuration() { return totalDuration; }
        public double getAverageResponseTime() { return averageResponseTime; }
        
        // Setters
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public void setTotalDuration(Duration totalDuration) { this.totalDuration = totalDuration; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public void addMessage(ChatMessage message, String username) {
            this.messages.add(message);
            this.participants.add(username);
        }
    }

    /**
     * 메시지를 대화 세션에 추가하고 흐름을 분석합니다
     */
    public void analyzeConversationFlow(Long roomId, ChatMessage message, String username) {
        List<ConversationSession> sessions = roomConversations.computeIfAbsent(roomId, k -> new ArrayList<>());
        
        ConversationSession currentSession = getCurrentOrCreateSession(sessions, message.getTimestamp());
        currentSession.addMessage(message, username);
        
        // 세션 통계 업데이트
        updateSessionStatistics(currentSession);
        
        logger.debug("대화 흐름 분석 완료: roomId={}, session 메시지 수={}, 참여자 수={}", 
                    roomId, currentSession.getMessages().size(), currentSession.getParticipants().size());
    }

    /**
     * 현재 세션을 가져오거나 새 세션을 생성합니다
     */
    private ConversationSession getCurrentOrCreateSession(List<ConversationSession> sessions, LocalDateTime messageTime) {
        if (sessions.isEmpty()) {
            ConversationSession newSession = new ConversationSession(messageTime);
            sessions.add(newSession);
            return newSession;
        }
        
        ConversationSession lastSession = sessions.get(sessions.size() - 1);
        
        // 마지막 메시지로부터 10분 이상 지났으면 새 세션 시작
        if (lastSession.getMessages().isEmpty() || 
            Duration.between(lastSession.getMessages().get(lastSession.getMessages().size() - 1).getTimestamp(), messageTime)
                .toMinutes() > SESSION_BREAK_MINUTES) {
            
            // 이전 세션 종료
            if (!lastSession.getMessages().isEmpty()) {
                lastSession.setEndTime(lastSession.getMessages().get(lastSession.getMessages().size() - 1).getTimestamp());
            }
            
            ConversationSession newSession = new ConversationSession(messageTime);
            sessions.add(newSession);
            return newSession;
        }
        
        return lastSession;
    }

    /**
     * 세션 통계를 업데이트합니다
     */
    private void updateSessionStatistics(ConversationSession session) {
        List<ChatMessage> messages = session.getMessages();
        if (messages.size() < 2) return;
        
        // 세션 지속 시간 계산
        LocalDateTime start = messages.get(0).getTimestamp();
        LocalDateTime end = messages.get(messages.size() - 1).getTimestamp();
        session.setTotalDuration(Duration.between(start, end));
        
        // 평균 응답 시간 계산
        List<Duration> responseTimes = new ArrayList<>();
        for (int i = 1; i < messages.size(); i++) {
            Duration responseTime = Duration.between(
                messages.get(i - 1).getTimestamp(), 
                messages.get(i).getTimestamp()
            );
            responseTimes.add(responseTime);
        }
        
        double avgResponseSeconds = responseTimes.stream()
            .mapToLong(Duration::getSeconds)
            .average()
            .orElse(0.0);
        
        session.setAverageResponseTime(avgResponseSeconds);
    }

    /**
     * 채팅방의 대화 흐름 통계를 조회합니다
     */
    public Map<String, Object> getConversationFlowStats(Long roomId) {
        List<ConversationSession> sessions = roomConversations.getOrDefault(roomId, new ArrayList<>());
        
        if (sessions.isEmpty()) {
            return createEmptyFlowStats(roomId);
        }
        
        // 활성 세션 개수
        long activeSessions = sessions.stream()
            .filter(session -> !session.getMessages().isEmpty())
            .filter(session -> {
                ChatMessage lastMessage = session.getMessages().get(session.getMessages().size() - 1);
                return Duration.between(lastMessage.getTimestamp(), LocalDateTime.now()).getSeconds() < SESSION_BREAK_MINUTES * 60;
            })
            .count();
        
        // 평균 세션 지속 시간
        double avgSessionDuration = sessions.stream()
            .filter(session -> session.getTotalDuration() != null)
            .mapToLong(session -> session.getTotalDuration().toMinutes())
            .average()
            .orElse(0.0);
        
        // 평균 응답 시간
        double avgResponseTime = sessions.stream()
            .mapToDouble(ConversationSession::getAverageResponseTime)
            .filter(time -> time > 0)
            .average()
            .orElse(0.0);
        
        // 가장 활발한 시간대
        Map<Integer, Long> hourlyActivity = sessions.stream()
            .flatMap(session -> session.getMessages().stream())
            .collect(Collectors.groupingBy(
                message -> message.getTimestamp().getHour(),
                Collectors.counting()
            ));
        
        // 세션별 참여자 수 분포
        Map<String, Long> participantDistribution = sessions.stream()
            .collect(Collectors.groupingBy(
                session -> session.getParticipants().size() + "명",
                Collectors.counting()
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("totalSessions", sessions.size());
        result.put("activeSessions", activeSessions);
        result.put("averageSessionDuration", Math.round(avgSessionDuration * 100.0) / 100.0); // 분 단위
        result.put("averageResponseTime", Math.round(avgResponseTime * 100.0) / 100.0); // 초 단위
        result.put("hourlyActivity", hourlyActivity);
        result.put("participantDistribution", participantDistribution);
        result.put("lastUpdated", LocalDateTime.now());
        
        return result;
    }

    /**
     * 빈 통계 데이터를 생성합니다
     */
    private Map<String, Object> createEmptyFlowStats(Long roomId) {
        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("totalSessions", 0);
        result.put("activeSessions", 0);
        result.put("averageSessionDuration", 0.0);
        result.put("averageResponseTime", 0.0);
        result.put("hourlyActivity", new HashMap<>());
        result.put("participantDistribution", new HashMap<>());
        result.put("lastUpdated", LocalDateTime.now());
        return result;
    }

    /**
     * 대화의 활발함 정도를 측정합니다
     */
    public String getConversationActivity(Long roomId) {
        List<ConversationSession> sessions = roomConversations.getOrDefault(roomId, new ArrayList<>());
        
        if (sessions.isEmpty()) {
            return "비활성";
        }
        
        ConversationSession currentSession = sessions.get(sessions.size() - 1);
        List<ChatMessage> messages = currentSession.getMessages();
        
        if (messages.isEmpty()) {
            return "비활성";
        }
        
        // 최근 메시지 시간 확인
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        long secondsSinceLastMessage = Duration.between(lastMessage.getTimestamp(), LocalDateTime.now()).getSeconds();
        
        if (secondsSinceLastMessage <= ACTIVE_CONVERSATION_SECONDS) {
            return "매우 활발";
        } else if (secondsSinceLastMessage <= ACTIVE_CONVERSATION_SECONDS * 3) {
            return "활발";
        } else if (secondsSinceLastMessage <= SESSION_BREAK_MINUTES * 60) {
            return "보통";
        } else {
            return "비활성";
        }
    }

    /**
     * 대화 패턴 분석
     */
    public Map<String, Object> analyzeConversationPatterns(Long roomId) {
        List<ConversationSession> sessions = roomConversations.getOrDefault(roomId, new ArrayList<>());
        
        Map<String, Object> patterns = new HashMap<>();
        
        if (sessions.isEmpty()) {
            patterns.put("dominantPattern", "데이터 부족");
            patterns.put("conversationRhythm", "분석 불가");
            patterns.put("peakHours", new ArrayList<>());
            return patterns;
        }
        
        // 대화 리듬 분석 (빠름/보통/느림)
        double avgResponseTime = sessions.stream()
            .mapToDouble(ConversationSession::getAverageResponseTime)
            .filter(time -> time > 0)
            .average()
            .orElse(0.0);
        
        String rhythm;
        if (avgResponseTime <= 10) {
            rhythm = "빠름";
        } else if (avgResponseTime <= 60) {
            rhythm = "보통";
        } else {
            rhythm = "느림";
        }
        
        // 주요 활동 시간대 (상위 3개)
        Map<Integer, Long> hourlyActivity = sessions.stream()
            .flatMap(session -> session.getMessages().stream())
            .collect(Collectors.groupingBy(
                message -> message.getTimestamp().getHour(),
                Collectors.counting()
            ));
        
        List<Integer> peakHours = hourlyActivity.entrySet().stream()
            .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // 주요 대화 패턴
        String dominantPattern;
        if (sessions.size() > 10 && avgResponseTime <= 30) {
            dominantPattern = "활발한 토론형";
        } else if (avgResponseTime <= 60) {
            dominantPattern = "일반 대화형";
        } else {
            dominantPattern = "신중한 소통형";
        }
        
        patterns.put("dominantPattern", dominantPattern);
        patterns.put("conversationRhythm", rhythm);
        patterns.put("peakHours", peakHours);
        patterns.put("averageResponseTime", Math.round(avgResponseTime * 100.0) / 100.0);
        
        return patterns;
    }

    /**
     * 채팅방 대화 데이터 초기화
     */
    public void clearConversationData(Long roomId) {
        roomConversations.remove(roomId);
        logger.info("대화 흐름 데이터 초기화 완료: roomId={}", roomId);
    }
}