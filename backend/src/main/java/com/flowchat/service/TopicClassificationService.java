package com.flowchat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class TopicClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(TopicClassificationService.class);

    // 주제별 키워드 분류
    private static final Map<String, Set<String>> TOPIC_KEYWORDS = Map.of(
        "업무", Set.of(
            "회의", "프로젝트", "업무", "일정", "작업", "개발", "구현", "설계", "테스트", "배포",
            "문서", "요구사항", "기능", "버그", "수정", "완료", "진행", "검토", "승인", "기획",
            "마감", "스케줄", "업데이트", "리뷰", "피드백", "개선", "최적화", "성능"
        ),
        "일상", Set.of(
            "안녕", "좋아", "나쁘다", "재미있다", "심심하다", "피곤하다", "배고프다", "맛있다",
            "날씨", "오늘", "내일", "주말", "휴가", "여행", "영화", "게임", "음악", "책",
            "커피", "점심", "저녁", "아침", "취미", "운동", "쇼핑", "친구", "가족"
        ),
        "문제", Set.of(
            "문제", "오류", "에러", "실패", "안됨", "도움", "질문", "모르겠다", "어렵다", "복잡하다",
            "해결", "수정", "고치다", "버그", "이슈", "장애", "복구", "점검", "확인", "체크",
            "급해", "빨리", "긴급", "중요", "심각", "위험", "주의", "경고"
        ),
        "기술", Set.of(
            "코드", "프로그래밍", "개발", "API", "데이터베이스", "서버", "클라이언트", "백엔드", "프론트엔드",
            "자바", "스프링", "리액트", "자바스크립트", "파이썬", "SQL", "노드", "웹소켓", "REST",
            "JSON", "HTML", "CSS", "Git", "도커", "쿠버네티스", "AWS", "클라우드", "배포", "CI/CD"
        ),
        "팀워크", Set.of(
            "팀", "협업", "소통", "공유", "의견", "토론", "논의", "브레인스토밍", "아이디어",
            "제안", "동의", "반대", "찬성", "결정", "합의", "조율", "역할", "책임", "지원",
            "도움", "멘토링", "코칭", "가이드", "안내", "설명", "공지", "알림"
        )
    );

    // 감정 키워드 분류
    private static final Map<String, Set<String>> EMOTION_KEYWORDS = Map.of(
        "긍정", Set.of(
            "좋다", "훌륭하다", "완벽하다", "만족", "기쁘다", "행복하다", "즐겁다", "재미있다",
            "성공", "완료", "달성", "해결", "최고", "멋지다", "대단하다", "감사", "고마워",
            "사랑", "좋아한다", "추천", "괜찮다", "잘됐다", "축하", "화이팅", "파이팅"
        ),
        "부정", Set.of(
            "나쁘다", "최악", "싫다", "화나다", "짜증", "스트레스", "실망", "속상하다", "우울하다",
            "문제", "실패", "안됨", "어렵다", "복잡하다", "힘들다", "피곤하다", "지쳤다",
            "걱정", "불안", "두렵다", "무섭다", "답답하다", "귀찮다"
        ),
        "중립", Set.of(
            "보통", "그냥", "일반", "평범", "괜찮다", "그럭저럭", "보통이다", "무난하다",
            "알겠다", "이해", "확인", "체크", "검토", "점검", "분석", "정리", "요약"
        )
    );

    // 단어 분리 패턴
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\s\\p{Punct}]+");

    /**
     * 메시지의 주제를 분류합니다
     */
    public String classifyTopic(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "기타";
        }

        String[] words = WORD_PATTERN.split(content.toLowerCase().trim());
        Map<String, Integer> topicScores = new HashMap<>();

        // 각 주제별 점수 계산
        for (String word : words) {
            word = word.trim();
            if (word.length() < 2) continue;

            for (Map.Entry<String, Set<String>> entry : TOPIC_KEYWORDS.entrySet()) {
                String topic = entry.getKey();
                Set<String> keywords = entry.getValue();

                // 완전 일치
                if (keywords.contains(word)) {
                    topicScores.merge(topic, 3, Integer::sum);
                    continue;
                }

                // 부분 일치 (키워드가 단어에 포함된 경우)
                for (String keyword : keywords) {
                    if (word.contains(keyword) || keyword.contains(word)) {
                        topicScores.merge(topic, 1, Integer::sum);
                        break;
                    }
                }
            }
        }

        // 가장 높은 점수의 주제 반환
        return topicScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("기타");
    }

    /**
     * 메시지의 감정을 분석합니다
     */
    public String analyzeEmotion(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "중립";
        }

        String[] words = WORD_PATTERN.split(content.toLowerCase().trim());
        Map<String, Integer> emotionScores = new HashMap<>();

        // 각 감정별 점수 계산
        for (String word : words) {
            word = word.trim();
            if (word.length() < 2) continue;

            for (Map.Entry<String, Set<String>> entry : EMOTION_KEYWORDS.entrySet()) {
                String emotion = entry.getKey();
                Set<String> keywords = entry.getValue();

                // 완전 일치
                if (keywords.contains(word)) {
                    emotionScores.merge(emotion, 3, Integer::sum);
                    continue;
                }

                // 부분 일치
                for (String keyword : keywords) {
                    if (word.contains(keyword) || keyword.contains(word)) {
                        emotionScores.merge(emotion, 1, Integer::sum);
                        break;
                    }
                }
            }
        }

        // 감정 점수가 없으면 중립
        if (emotionScores.isEmpty()) {
            return "중립";
        }

        // 가장 높은 점수의 감정 반환
        return emotionScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("중립");
    }

    /**
     * 주제별 통계를 계산합니다
     */
    public Map<String, Object> getTopicStatistics(List<String> messages) {
        Map<String, Integer> topicCounts = new HashMap<>();
        Map<String, Integer> emotionCounts = new HashMap<>();

        for (String message : messages) {
            String topic = classifyTopic(message);
            String emotion = analyzeEmotion(message);
            
            topicCounts.merge(topic, 1, Integer::sum);
            emotionCounts.merge(emotion, 1, Integer::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("topicDistribution", topicCounts);
        result.put("emotionDistribution", emotionCounts);
        result.put("totalMessages", messages.size());
        result.put("lastUpdated", new Date());

        logger.debug("주제 분류 통계 계산 완료: 총 {}개 메시지, 주제 {}, 감정 {}", 
                    messages.size(), topicCounts.size(), emotionCounts.size());

        return result;
    }

    /**
     * 단일 메시지에 대한 종합 분석
     */
    public Map<String, String> analyzeMessage(String content) {
        Map<String, String> result = new HashMap<>();
        result.put("topic", classifyTopic(content));
        result.put("emotion", analyzeEmotion(content));
        return result;
    }
}