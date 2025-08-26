package com.flowchat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class LlmAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(LlmAnalysisService.class);

    @Value("${llm.provider:openai}")
    private String llmProvider;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.model:gpt-3.5-turbo}")
    private String model;

    @Value("${llm.max-tokens:1000}")
    private int maxTokens;

    @Value("${llm.temperature:0.3}")
    private double temperature;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * LLM을 사용한 종합 채팅 분석
     */
    public CompletableFuture<Map<String, Object>> analyzeMessage(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = createAnalysisPrompt(content);
                String response = callLlm(prompt);
                return parseAnalysisResponse(response);
            } catch (Exception e) {
                logger.error("LLM 분석 실패: content={}, error={}", content, e.getMessage(), e);
                return createFallbackAnalysis(content);
            }
        });
    }

    /**
     * 키워드 추출을 위한 LLM 호출
     */
    public CompletableFuture<List<String>> extractKeywords(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = createKeywordExtractionPrompt(content);
                String response = callLlm(prompt);
                return parseKeywordsFromResponse(response);
            } catch (Exception e) {
                logger.error("LLM 키워드 추출 실패: content={}, error={}", content, e.getMessage(), e);
                return extractKeywordsFallback(content);
            }
        });
    }

    /**
     * 주제 분류를 위한 LLM 호출
     */
    public CompletableFuture<String> classifyTopic(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = createTopicClassificationPrompt(content);
                String response = callLlm(prompt);
                return parseTopicFromResponse(response);
            } catch (Exception e) {
                logger.error("LLM 주제 분류 실패: content={}, error={}", content, e.getMessage(), e);
                return classifyTopicFallback(content);
            }
        });
    }

    /**
     * 감정 분석을 위한 LLM 호출
     */
    public CompletableFuture<Map<String, Object>> analyzeEmotion(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = createEmotionAnalysisPrompt(content);
                String response = callLlm(prompt);
                return parseEmotionFromResponse(response);
            } catch (Exception e) {
                logger.error("LLM 감정 분석 실패: content={}, error={}", content, e.getMessage(), e);
                return createFallbackEmotion(content);
            }
        });
    }

    /**
     * 대화 맥락 분석 (여러 메시지를 함께 분석)
     */
    public CompletableFuture<Map<String, Object>> analyzeConversationContext(List<String> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = createContextAnalysisPrompt(messages);
                String response = callLlm(prompt);
                return parseContextResponse(response);
            } catch (Exception e) {
                logger.error("LLM 맥락 분석 실패: messages count={}, error={}", messages.size(), e.getMessage(), e);
                return createFallbackContext();
            }
        });
    }

    /**
     * 커스텀 프롬프트를 사용한 LLM 분석
     */
    public CompletableFuture<Map<String, Object>> analyzeCustomPrompt(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("커스텀 프롬프트 LLM 분석 시작: {}", prompt.substring(0, Math.min(50, prompt.length())));
                
                String response = callLlm(prompt);
                
                // 응답이 JSON 형태인지 확인하고 파싱
                try {
                    return objectMapper.readValue(response, Map.class);
                } catch (Exception parseException) {
                    // JSON이 아닌 경우, 텍스트 응답으로 처리
                    Map<String, Object> result = new HashMap<>();
                    result.put("purpose", response.trim());
                    result.put("confidence", 0.8);
                    return result;
                }
                
            } catch (Exception e) {
                logger.error("커스텀 프롬프트 LLM 분석 실패: error={}", e.getMessage());
                
                // 폴백 응답
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("purpose", "이 채팅방은 다양한 주제로 소통하는 공간입니다.");
                fallback.put("confidence", 0.1);
                fallback.put("error", "LLM 분석 실패");
                return fallback;
            }
        });
    }

    /**
     * LLM API 호출
     */
    private String callLlm(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = createRequestBody(prompt);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return extractContentFromResponse(responseJson);
        } else {
            throw new RuntimeException("LLM API 호출 실패: " + response.getStatusCode());
        }
    }

    /**
     * 요청 본문 생성
     */
    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> message = Map.of(
            "role", "user",
            "content", prompt
        );

        return Map.of(
            "model", model,
            "messages", List.of(message),
            "max_tokens", maxTokens,
            "temperature", temperature
        );
    }

    /**
     * LLM 응답에서 내용 추출
     */
    private String extractContentFromResponse(JsonNode response) throws Exception {
        return response.path("choices")
                      .get(0)
                      .path("message")
                      .path("content")
                      .asText();
    }

    /**
     * 종합 분석 프롬프트 생성
     */
    private String createAnalysisPrompt(String content) {
        return """
            다음 채팅 메시지를 분석해주세요:
            "%s"
            
            다음 JSON 형식으로 응답해주세요:
            {
                "keywords": ["키워드1", "키워드2", "키워드3"],
                "topic": "업무|일상|문제|기술|팀워크|기타",
                "emotion": "긍정|부정|중립",
                "sentiment_score": 0.5,
                "urgency": "높음|보통|낮음",
                "intent": "질문|정보공유|요청|답변|잡담",
                "confidence": 0.8
            }
            
            키워드는 최대 5개까지 추출하고, 모든 값은 한국어로 작성해주세요.
            """.formatted(content);
    }

    /**
     * 키워드 추출 프롬프트 생성
     */
    private String createKeywordExtractionPrompt(String content) {
        return """
            다음 채팅 메시지에서 핵심 키워드를 추출해주세요:
            "%s"
            
            규칙:
            - 최대 10개의 키워드 추출
            - 불용어(조사, 접속사 등) 제외
            - 중요도 순으로 정렬
            - 단일 단어 또는 2-3글자 구문
            
            JSON 배열로만 응답: ["키워드1", "키워드2", "키워드3"]
            """.formatted(content);
    }

    /**
     * 주제 분류 프롬프트 생성
     */
    private String createTopicClassificationPrompt(String content) {
        return """
            다음 채팅 메시지의 주제를 분류해주세요:
            "%s"
            
            가능한 주제:
            - 업무: 회의, 프로젝트, 일정, 작업 관련
            - 일상: 안부, 취미, 개인적인 이야기
            - 문제: 오류, 이슈, 도움 요청
            - 기술: 개발, 프로그래밍, 기술적 논의
            - 팀워크: 협업, 소통, 팀 관련
            - 기타: 위 카테고리에 맞지 않는 경우
            
            하나의 주제명만 응답해주세요: 업무|일상|문제|기술|팀워크|기타
            """.formatted(content);
    }

    /**
     * 감정 분석 프롬프트 생성
     */
    private String createEmotionAnalysisPrompt(String content) {
        return """
            다음 채팅 메시지의 감정을 분석해주세요:
            "%s"
            
            다음 JSON 형식으로 응답해주세요:
            {
                "emotion": "긍정|부정|중립",
                "intensity": "강함|보통|약함",
                "score": 0.7,
                "emotions": {
                    "기쁨": 0.3,
                    "분노": 0.1,
                    "슬픔": 0.0,
                    "놀람": 0.2,
                    "두려움": 0.0,
                    "중립": 0.4
                }
            }
            
            score는 -1(매우 부정) ~ 1(매우 긍정) 범위입니다.
            """.formatted(content);
    }

    /**
     * 맥락 분석 프롬프트 생성
     */
    private String createContextAnalysisPrompt(List<String> messages) {
        String conversation = String.join("\n", messages);
        return """
            다음 대화의 맥락을 분석해주세요:
            %s
            
            다음 JSON 형식으로 응답해주세요:
            {
                "flow": "활발|보통|소극적",
                "tone": "공식적|친근|전문적|캐주얼",
                "main_topics": ["주요 주제1", "주요 주제2"],
                "participation_pattern": "균등|주도적|수동적",
                "resolution_status": "해결됨|진행중|미해결|해당없음",
                "summary": "대화 요약 (50자 이내)"
            }
            """.formatted(conversation);
    }

    /**
     * 분석 응답 파싱
     */
    private Map<String, Object> parseAnalysisResponse(String response) {
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            logger.warn("LLM 응답 파싱 실패, 기본값 반환: {}", e.getMessage());
            return createFallbackAnalysis("");
        }
    }

    /**
     * 키워드 응답 파싱
     */
    private List<String> parseKeywordsFromResponse(String response) {
        try {
            // JSON 배열 파싱 시도
            List<String> keywords = objectMapper.readValue(response, List.class);
            return keywords.subList(0, Math.min(keywords.size(), 10));
        } catch (Exception e) {
            // 실패 시 텍스트에서 키워드 추출 시도
            return Arrays.asList(response.replaceAll("[\\[\\]\"]", "").split(","))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(10)
                        .toList();
        }
    }

    /**
     * 주제 응답 파싱
     */
    private String parseTopicFromResponse(String response) {
        String topic = response.trim();
        Set<String> validTopics = Set.of("업무", "일상", "문제", "기술", "팀워크", "기타");
        return validTopics.contains(topic) ? topic : "기타";
    }

    /**
     * 감정 응답 파싱
     */
    private Map<String, Object> parseEmotionFromResponse(String response) {
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            return createFallbackEmotion("");
        }
    }

    /**
     * 맥락 응답 파싱
     */
    private Map<String, Object> parseContextResponse(String response) {
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            return createFallbackContext();
        }
    }

    /**
     * 폴백 분석 결과 생성
     */
    private Map<String, Object> createFallbackAnalysis(String content) {
        return Map.of(
            "keywords", extractKeywordsFallback(content),
            "topic", "기타",
            "emotion", "중립",
            "sentiment_score", 0.0,
            "urgency", "보통",
            "intent", "잡담",
            "confidence", 0.1
        );
    }

    /**
     * 폴백 키워드 추출 (정적 방법)
     */
    private List<String> extractKeywordsFallback(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }
        
        String[] words = content.split("[\\s\\p{Punct}]+");
        return Arrays.stream(words)
                    .filter(word -> word.length() >= 2)
                    .limit(5)
                    .toList();
    }

    /**
     * 폴백 주제 분류
     */
    private String classifyTopicFallback(String content) {
        return "기타";
    }

    /**
     * 폴백 감정 분석
     */
    private Map<String, Object> createFallbackEmotion(String content) {
        return Map.of(
            "emotion", "중립",
            "intensity", "보통",
            "score", 0.0,
            "emotions", Map.of(
                "기쁨", 0.0,
                "분노", 0.0,
                "슬픔", 0.0,
                "놀람", 0.0,
                "두려움", 0.0,
                "중립", 1.0
            )
        );
    }

    /**
     * 폴백 맥락 분석
     */
    private Map<String, Object> createFallbackContext() {
        return Map.of(
            "flow", "보통",
            "tone", "캐주얼",
            "main_topics", List.of(),
            "participation_pattern", "균등",
            "resolution_status", "해당없음",
            "summary", "분석 불가"
        );
    }
}