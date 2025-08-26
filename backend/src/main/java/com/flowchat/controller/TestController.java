package com.flowchat.controller;

import com.flowchat.dto.ChatMessageResponse;
import com.flowchat.service.MessageService;
import com.flowchat.service.TopicClassificationService;
import com.flowchat.service.ReportService;
import com.flowchat.service.ChatAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private TopicClassificationService topicClassificationService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ChatAnalysisService chatAnalysisService;
    
    /**
     * 메시지 저장 테스트용 엔드포인트
     */
    @PostMapping("/send-message")
    public ResponseEntity<ChatMessageResponse> testSendMessage(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String content = request.get("content").toString();
        String messageType = request.getOrDefault("messageType", "TEXT").toString();
        
        ChatMessageResponse response = messageService.sendMessage(
            roomId, 
            authentication.getName(), 
            content, 
            messageType
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 시스템 메시지 테스트용 엔드포인트
     */
    @PostMapping("/send-system-message")
    public ResponseEntity<ChatMessageResponse> testSendSystemMessage(
            @RequestBody Map<String, Object> request) {
        
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String content = request.get("content").toString();
        
        ChatMessageResponse response = messageService.sendSystemMessage(roomId, content);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * WebSocket 입장 테스트용 엔드포인트
     */
    @PostMapping("/join-room")
    public ResponseEntity<String> testJoinRoom(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Long roomId = Long.valueOf(request.get("roomId").toString());
        String sessionId = "test-session-" + System.currentTimeMillis();
        
        messageService.joinRoom(roomId, authentication.getName(), sessionId);
        
        return ResponseEntity.ok("입장 완료");
    }
    
    /**
     * 주제 분류 및 감정 분석 테스트 엔드포인트
     */
    @PostMapping("/analyze-text")
    public ResponseEntity<Map<String, String>> testAnalyzeText(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        
        Map<String, String> result = new HashMap<>();
        result.put("topic", topicClassificationService.classifyTopic(content));
        result.put("emotion", topicClassificationService.analyzeEmotion(content));
        result.put("originalContent", content);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 보고서 테스트 엔드포인트
     */
    @GetMapping("/report/daily")
    public ResponseEntity<Map<String, Object>> testDailyReport() {
        LocalDate today = LocalDate.now();
        Map<String, Object> report = reportService.generateDailyReport(today);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 채팅방 분석 재구축 테스트 엔드포인트
     */
    @PostMapping("/rebuild-analysis/{roomId}")
    public ResponseEntity<String> rebuildRoomAnalysis(@PathVariable Long roomId) {
        chatAnalysisService.rebuildRoomAnalysis(roomId);
        return ResponseEntity.ok("채팅방 " + roomId + " 분석 데이터 재구축 완료");
    }
    
    /**
     * 모든 채팅방 분석 재구축 테스트 엔드포인트
     */
    @PostMapping("/rebuild-analysis/all")
    public ResponseEntity<String> rebuildAllAnalysis() {
        chatAnalysisService.rebuildAllRoomAnalysis();
        return ResponseEntity.ok("모든 채팅방 분석 데이터 재구축 완료");
    }
    
    /**
     * 메시지 히스토리 조회 테스트 엔드포인트
     */
    @GetMapping("/message-history/{roomId}")
    public ResponseEntity<List<ChatMessageResponse>> testGetMessageHistory(@PathVariable Long roomId) {
        List<ChatMessageResponse> messages = messageService.getRoomMessageHistory(roomId, 10);
        return ResponseEntity.ok(messages);
    }
}