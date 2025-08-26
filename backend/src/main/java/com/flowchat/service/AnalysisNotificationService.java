package com.flowchat.service;

import com.flowchat.dto.AnalysisData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AnalysisNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatAnalysisService chatAnalysisService;

    // 채팅방별 마지막 업데이트 시간 및 메시지 카운트 추적
    private final Map<Long, LocalDateTime> lastUpdateTime = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> messageCountSinceLastUpdate = new ConcurrentHashMap<>();
    
    // 업데이트 임계값 설정
    private static final int MESSAGE_THRESHOLD = 10; // 메시지 10개마다 업데이트
    private static final int TIME_THRESHOLD_SECONDS = 10; // 10초마다 업데이트

    /**
     * 분석 결과를 WebSocket으로 전송
     */
    @Async
    public void sendAnalysisUpdate(Long roomId, AnalysisData data) {
        try {
            String destination = "/topic/analysis/" + roomId;
            messagingTemplate.convertAndSend(destination, data);
            
            logger.debug("분석 결과 WebSocket 전송 완료: roomId={}, type={}, destination={}", 
                        roomId, data.getAnalysisType(), destination);
            
        } catch (Exception e) {
            logger.error("분석 결과 WebSocket 전송 실패: roomId={}, type={}, error={}", 
                        roomId, data.getAnalysisType(), e.getMessage(), e);
        }
    }

    /**
     * 키워드 분석 업데이트 전송
     */
    @Async
    public void sendKeywordUpdate(Long roomId) {
        try {
            Map<String, Object> keywordStats = chatAnalysisService.getRoomKeywordStats(roomId);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> topKeywords = (List<Map<String, Object>>) keywordStats.get("topKeywords");
            
            if (topKeywords != null && !topKeywords.isEmpty()) {
                // Chart.js 형식으로 데이터 변환
                List<String> labels = topKeywords.stream()
                    .map(keyword -> (String) keyword.get("keyword"))
                    .collect(Collectors.toList());
                    
                List<Integer> data = topKeywords.stream()
                    .map(keyword -> (Integer) keyword.get("count"))
                    .collect(Collectors.toList());
                
                Integer totalKeywords = (Integer) keywordStats.get("totalKeywords");
                
                AnalysisData.KeywordAnalysis keywordAnalysis = new AnalysisData.KeywordAnalysis(
                    labels, data, topKeywords, totalKeywords);
                
                AnalysisData analysisData = AnalysisData.createKeywordUpdate(roomId, keywordAnalysis);
                sendAnalysisUpdate(roomId, analysisData);
            }
            
        } catch (Exception e) {
            logger.error("키워드 분석 업데이트 전송 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    /**
     * 참여도 분석 업데이트 전송
     */
    @Async
    public void sendParticipationUpdate(Long roomId) {
        try {
            Map<String, Object> participationStats = chatAnalysisService.getRoomUserParticipation(roomId);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> userParticipation = (List<Map<String, Object>>) participationStats.get("userParticipation");
            
            if (userParticipation != null && !userParticipation.isEmpty()) {
                // Chart.js 형식으로 데이터 변환
                List<String> labels = userParticipation.stream()
                    .map(user -> (String) user.get("username"))
                    .collect(Collectors.toList());
                    
                List<Integer> data = userParticipation.stream()
                    .map(user -> (Integer) user.get("messageCount"))
                    .collect(Collectors.toList());
                
                Integer totalUsers = (Integer) participationStats.get("totalUsers");
                
                AnalysisData.ParticipationAnalysis participationAnalysis = new AnalysisData.ParticipationAnalysis(
                    labels, data, userParticipation, totalUsers);
                
                AnalysisData analysisData = AnalysisData.createParticipationUpdate(roomId, participationAnalysis);
                sendAnalysisUpdate(roomId, analysisData);
            }
            
        } catch (Exception e) {
            logger.error("참여도 분석 업데이트 전송 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    /**
     * 시간대별 활동 분석 업데이트 전송
     */
    @Async
    public void sendHourlyUpdate(Long roomId) {
        try {
            Map<String, Object> hourlyStats = chatAnalysisService.getRoomHourlyStats(roomId);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hourlyActivity = (List<Map<String, Object>>) hourlyStats.get("hourlyActivity");
            
            if (hourlyActivity != null) {
                // Chart.js 형식으로 데이터 변환
                List<String> labels = hourlyActivity.stream()
                    .map(hour -> hour.get("hour") + "시")
                    .collect(Collectors.toList());
                    
                List<Integer> data = hourlyActivity.stream()
                    .map(hour -> (Integer) hour.get("messageCount"))
                    .collect(Collectors.toList());
                
                AnalysisData.HourlyAnalysis hourlyAnalysis = new AnalysisData.HourlyAnalysis(
                    labels, data, hourlyActivity);
                
                AnalysisData analysisData = AnalysisData.createHourlyUpdate(roomId, hourlyAnalysis);
                sendAnalysisUpdate(roomId, analysisData);
            }
            
        } catch (Exception e) {
            logger.error("시간대별 분석 업데이트 전송 실패: roomId={}, error={}", roomId, e.getMessage());
        }
    }

    /**
     * 전체 분석 결과 업데이트 전송 (데이터 재구축 포함)
     */
    @Async
    public void sendFullAnalysisUpdate(Long roomId) {
        sendFullAnalysisUpdate(roomId, null);
    }
    
    /**
     * 기간별 전체 분석 결과 업데이트 전송
     */
    @Async
    public void sendFullAnalysisUpdate(Long roomId, Integer days) {
        logger.debug("전체 분석 업데이트 시작: roomId={}, days={}", roomId, days);
        
        // 새로고침 시 분석 데이터를 새로 계산
        chatAnalysisService.rebuildRoomAnalysis(roomId, days);
        
        sendKeywordUpdate(roomId);
        sendParticipationUpdate(roomId);
        sendHourlyUpdate(roomId);
    }

    /**
     * 메시지 수신 시 업데이트 조건 확인 및 전송
     */
    @Async
    public void onMessageReceived(Long roomId) {
        // 메시지 카운트 증가
        messageCountSinceLastUpdate.computeIfAbsent(roomId, k -> new AtomicInteger(0)).incrementAndGet();
        
        // 업데이트 조건 확인
        if (shouldSendUpdate(roomId)) {
            sendFullAnalysisUpdate(roomId);
            resetUpdateTracking(roomId);
        }
    }

    /**
     * 업데이트 전송 조건 확인
     */
    private boolean shouldSendUpdate(Long roomId) {
        LocalDateTime lastUpdate = lastUpdateTime.get(roomId);
        LocalDateTime now = LocalDateTime.now();
        
        // 첫 번째 업데이트이거나 시간 임계값 초과
        if (lastUpdate == null || now.minusSeconds(TIME_THRESHOLD_SECONDS).isAfter(lastUpdate)) {
            return true;
        }
        
        // 메시지 임계값 초과
        AtomicInteger messageCount = messageCountSinceLastUpdate.get(roomId);
        return messageCount != null && messageCount.get() >= MESSAGE_THRESHOLD;
    }

    /**
     * 업데이트 추적 정보 초기화
     */
    private void resetUpdateTracking(Long roomId) {
        lastUpdateTime.put(roomId, LocalDateTime.now());
        messageCountSinceLastUpdate.put(roomId, new AtomicInteger(0));
    }

    /**
     * 정기적으로 모든 활성 채팅방의 분석 결과 업데이트 (30초마다)
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledAnalysisUpdate() {
        try {
            // 마지막 업데이트 이후 활동이 있었던 채팅방들에 대해 업데이트
            for (Map.Entry<Long, AtomicInteger> entry : messageCountSinceLastUpdate.entrySet()) {
                Long roomId = entry.getKey();
                
                if (entry.getValue().get() > 0) { // 활동이 있었던 방만
                    logger.debug("정기 분석 업데이트: roomId={}", roomId);
                    sendFullAnalysisUpdate(roomId);
                    resetUpdateTracking(roomId);
                }
            }
        } catch (Exception e) {
            logger.error("정기 분석 업데이트 실패: error={}", e.getMessage(), e);
        }
    }

    /**
     * 특정 채팅방의 실시간 분석 구독 시작 (WebSocket 연결 시 호출)
     */
    public void startAnalysisSubscription(Long roomId) {
        logger.info("분석 구독 시작: roomId={}", roomId);
        
        // 초기 분석 데이터 전송
        sendFullAnalysisUpdate(roomId);
        resetUpdateTracking(roomId);
    }

    /**
     * 특정 채팅방의 실시간 분석 구독 종료
     */
    public void stopAnalysisSubscription(Long roomId) {
        logger.info("분석 구독 종료: roomId={}", roomId);
        
        lastUpdateTime.remove(roomId);
        messageCountSinceLastUpdate.remove(roomId);
    }
}