package com.flowchat.controller;

import com.flowchat.service.AnalysisNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class AnalysisWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisWebSocketController.class);

    @Autowired
    private AnalysisNotificationService analysisNotificationService;
    
    @Autowired
    private com.flowchat.service.ChatAnalysisService chatAnalysisService;

    /**
     * 분석 데이터 구독 시작
     */
    @SubscribeMapping("/topic/analysis/{roomId}")
    public void subscribeToAnalysis(@DestinationVariable Long roomId, Principal principal) {
        logger.info("분석 데이터 구독 시작: roomId={}, user={}", roomId, 
                   principal != null ? principal.getName() : "anonymous");
        
        // 구독 시작 시 초기 분석 데이터 전송
        analysisNotificationService.startAnalysisSubscription(roomId);
    }

    /**
     * 분석 데이터 수동 요청 (기간 선택 지원)
     */
    @MessageMapping("/analysis/{roomId}/refresh")
    public void refreshAnalysis(@DestinationVariable Long roomId, 
                               @Payload Map<String, Object> payload,
                               Principal principal) {
        
        logger.info("분석 데이터 새로고침 요청: roomId={}, user={}, payload={}", roomId, 
                   principal != null ? principal.getName() : "anonymous", payload);
        
        String analysisType = (String) payload.getOrDefault("type", "FULL");
        Integer days = null;
        
        // 기간 파라미터 파싱
        Object daysParam = payload.get("days");
        if (daysParam != null) {
            if (daysParam instanceof Integer) {
                days = (Integer) daysParam;
            } else if (daysParam instanceof String) {
                try {
                    days = Integer.parseInt((String) daysParam);
                } catch (NumberFormatException e) {
                    logger.warn("잘못된 days 파라미터: {}", daysParam);
                }
            }
        }
        
        switch (analysisType.toUpperCase()) {
            case "KEYWORD":
                // 개별 분석은 새로 계산하고 전송
                if (days != null) {
                    chatAnalysisService.rebuildRoomAnalysis(roomId, days);
                }
                analysisNotificationService.sendKeywordUpdate(roomId);
                break;
            case "PARTICIPATION":
                if (days != null) {
                    chatAnalysisService.rebuildRoomAnalysis(roomId, days);
                }
                analysisNotificationService.sendParticipationUpdate(roomId);
                break;
            case "HOURLY":
                if (days != null) {
                    chatAnalysisService.rebuildRoomAnalysis(roomId, days);
                }
                analysisNotificationService.sendHourlyUpdate(roomId);
                break;
            default:
                analysisNotificationService.sendFullAnalysisUpdate(roomId, days);
                break;
        }
        
        logger.debug("분석 데이터 새로고침 완료: roomId={}, type={}, days={}", roomId, analysisType, days);
    }
}