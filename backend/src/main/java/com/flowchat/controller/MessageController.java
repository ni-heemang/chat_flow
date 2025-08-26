package com.flowchat.controller;

import com.flowchat.dto.ChatMessageRequest;
import com.flowchat.dto.ChatMessageResponse;
import com.flowchat.handler.WebSocketEventHandler;
import com.flowchat.service.MessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebSocketEventHandler webSocketEventHandler;

    /**
     * 메시지 전송
     * 클라이언트에서 /app/send-message로 메시지 전송
     */
    @MessageMapping("/send-message")
    public void sendMessage(@Valid @Payload ChatMessageRequest messageRequest,
                           SimpMessageHeaderAccessor headerAccessor,
                           Principal principal) {
        try {
            String username = principal.getName();
            String sessionId = headerAccessor.getSessionId();
            
            logger.debug("메시지 전송 요청: username={}, roomId={}, content={}", 
                        username, messageRequest.getRoomId(), messageRequest.getContent());
            
            // 메시지 저장 및 브로드캐스트
            ChatMessageResponse response = messageService.sendMessage(
                messageRequest.getRoomId(),
                username,
                messageRequest.getContent(),
                messageRequest.getMessageType()
            );
            
            logger.info("메시지 전송 완료: messageId={}, roomId={}, username={}", 
                       response.getId(), response.getRoomId(), username);
                       
        } catch (Exception e) {
            logger.error("메시지 전송 실패: username={}, roomId={}, error={}", 
                        principal != null ? principal.getName() : "unknown", 
                        messageRequest.getRoomId(), e.getMessage(), e);
            
            // 에러 메시지를 클라이언트에 전송
            sendErrorToUser(headerAccessor, "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅방 WebSocket 연결 (멤버십 생성 안함)
     * 클라이언트에서 /app/connect-room으로 WebSocket 연결만 수행
     */
    @MessageMapping("/connect-room")
    public void connectToRoom(@Payload Map<String, Object> payload,
                             SimpMessageHeaderAccessor headerAccessor,
                             Principal principal) {
        try {
            String username = principal.getName();
            String sessionId = headerAccessor.getSessionId();
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            
            logger.debug("채팅방 WebSocket 연결 요청: username={}, roomId={}, sessionId={}", 
                        username, roomId, sessionId);
            
            // WebSocket 세션 관리에만 추가 (멤버십 생성 안함)
            webSocketEventHandler.addUserToRoom(roomId.toString(), sessionId, username);
            
            logger.info("채팅방 WebSocket 연결 완료: username={}, roomId={}", username, roomId);
            
        } catch (Exception e) {
            logger.error("채팅방 WebSocket 연결 실패: username={}, error={}", 
                        principal != null ? principal.getName() : "unknown", e.getMessage(), e);
            
            sendErrorToUser(headerAccessor, "채팅방 연결에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 채팅방 실제 참여 (멤버십 생성)
     * 클라이언트에서 /app/join-room으로 실제 멤버십 생성
     */
    @MessageMapping("/join-room")
    public void joinRoom(@Payload Map<String, Object> payload,
                        SimpMessageHeaderAccessor headerAccessor,
                        Principal principal) {
        try {
            String username = principal.getName();
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            
            logger.debug("채팅방 참여 요청: username={}, roomId={}", username, roomId);
            
            // 실제 채팅방 멤버십 생성
            messageService.joinRoom(roomId, username, headerAccessor.getSessionId());
            
            logger.info("채팅방 참여 완료: username={}, roomId={}", username, roomId);
            
        } catch (Exception e) {
            logger.error("채팅방 참여 실패: username={}, error={}", 
                        principal != null ? principal.getName() : "unknown", e.getMessage(), e);
            
            sendErrorToUser(headerAccessor, "채팅방 참여에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅방 퇴장 (멤버십 제거)
     * 클라이언트에서 /app/leave-room으로 멤버십 제거
     */
    @MessageMapping("/leave-room")
    public void leaveRoom(@Payload Map<String, Object> payload,
                         SimpMessageHeaderAccessor headerAccessor,
                         Principal principal) {
        try {
            String username = principal.getName();
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            
            logger.debug("채팅방 퇴장 요청: username={}, roomId={}", username, roomId);
            
            // 실제 채팅방 멤버십 제거
            messageService.leaveRoom(roomId, username);
            
            logger.info("채팅방 퇴장 완료: username={}, roomId={}", username, roomId);
            
        } catch (Exception e) {
            logger.error("채팅방 퇴장 실패: username={}, error={}", 
                        principal != null ? principal.getName() : "unknown", e.getMessage(), e);
            
            sendErrorToUser(headerAccessor, "채팅방 퇴장에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 채팅방 WebSocket 연결 해제
     * 클라이언트에서 /app/disconnect-room으로 WebSocket 연결 해제
     */
    @MessageMapping("/disconnect-room")
    public void disconnectFromRoom(@Payload Map<String, Object> payload,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        try {
            String username = principal.getName();
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            
            logger.debug("채팅방 WebSocket 연결 해제 요청: username={}, roomId={}", username, roomId);
            
            // WebSocket 세션 관리에서만 제거
            webSocketEventHandler.removeUserFromRoom(roomId.toString(), username);
            
            logger.info("채팅방 WebSocket 연결 해제 완료: username={}, roomId={}", username, roomId);
            
        } catch (Exception e) {
            logger.error("채팅방 WebSocket 연겴 해제 실패: username={}, error={}", 
                        principal != null ? principal.getName() : "unknown", e.getMessage(), e);
        }
    }

    /**
     * 타이핑 상태 전송
     * 클라이언트에서 /app/typing으로 타이핑 상태 전송
     */
    @MessageMapping("/typing")
    public void sendTypingStatus(@Payload Map<String, Object> payload,
                                Principal principal) {
        try {
            String username = principal.getName();
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            Boolean isTyping = Boolean.valueOf(payload.get("isTyping").toString());
            
            logger.debug("타이핑 상태 전송: username={}, roomId={}, isTyping={}", 
                        username, roomId, isTyping);
            
            // 타이핑 상태 브로드캐스트
            messageService.sendTypingStatus(roomId, username, isTyping);
            
        } catch (Exception e) {
            logger.error("타이핑 상태 전송 실패: username={}, error={}", 
                        principal != null ? principal.getName() : "unknown", e.getMessage(), e);
        }
    }

    /**
     * 사용자에게 에러 메시지 전송
     */
    @SendToUser("/queue/error")
    private void sendErrorToUser(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        Map<String, Object> error = Map.of(
            "type", "ERROR",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        
        // 실제 구현에서는 SimpMessagingTemplate을 사용하여 직접 전송
        logger.warn("에러 메시지 전송: sessionId={}, message={}", 
                   headerAccessor.getSessionId(), errorMessage);
    }
}