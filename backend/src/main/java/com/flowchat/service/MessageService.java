package com.flowchat.service;

import com.flowchat.dto.ChatMessageResponse;
import com.flowchat.entity.ChatMessage;
import com.flowchat.entity.ChatRoom;
import com.flowchat.entity.User;
import com.flowchat.event.MessageReceivedEvent;
import com.flowchat.repository.ChatMessageRepository;
import com.flowchat.repository.ChatRoomRepository;
import com.flowchat.repository.UserRepository;
import com.flowchat.service.ChatRoomMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ChatRoomMemberService chatRoomMemberService;

    // 채팅방별 활성 사용자 관리 (username -> sessionId)
    private final ConcurrentMap<Long, ConcurrentMap<String, String>> roomUsers = new ConcurrentHashMap<>();

    /**
     * 메시지 전송 및 브로드캐스트
     */
    public ChatMessageResponse sendMessage(Long roomId, String username, String content, String messageType) {
        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + username));

        // 채팅방 조회 및 권한 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + roomId));

        if (!chatRoom.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 채팅방입니다: " + roomId);
        }
        
        // 메시지 전송은 멤버만 가능
        if (!chatRoomMemberService.isMemberOfRoom(roomId, user.getId())) {
            throw new IllegalStateException("채팅방 멤버만 메시지를 보낼 수 있습니다");
        }

        // 메시지 생성 및 저장
        ChatMessage message = new ChatMessage(roomId, user.getId(), content);
        if ("SYSTEM".equals(messageType)) {
            message.setMessageType(ChatMessage.MessageType.SYSTEM);
        }
        
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 응답 DTO 생성
        ChatMessageResponse response = ChatMessageResponse.from(savedMessage, user.getUsername(), user.getName());

        // 채팅방의 모든 사용자에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        // 메시지 분석을 위한 이벤트 발행
        eventPublisher.publishEvent(new MessageReceivedEvent(this, savedMessage, roomId, username));

        logger.debug("메시지 브로드캐스트 완료: roomId={}, messageId={}, userCount={}", 
                    roomId, savedMessage.getId(), getRoomUserCount(roomId));

        return response;
    }

    /**
     * 시스템 메시지 전송 (사용자가 없는 시스템 메시지)
     */
    public ChatMessageResponse sendSystemMessage(Long roomId, String content) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + roomId));

        if (!chatRoom.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 채팅방입니다: " + roomId);
        }

        // 시스템 메시지 생성 및 저장 (userId를 0으로 설정 - 시스템 메시지 식별용)
        ChatMessage message = new ChatMessage(roomId, 0L, content);
        message.setMessageType(ChatMessage.MessageType.SYSTEM);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 응답 DTO 생성 (시스템 메시지용)
        ChatMessageResponse response = ChatMessageResponse.from(savedMessage, "SYSTEM", "시스템");

        // 채팅방의 모든 사용자에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        logger.debug("시스템 메시지 브로드캐스트 완료: roomId={}, messageId={}", 
                    roomId, savedMessage.getId());

        return response;
    }

    /**
     * 채팅방 입장
     */
    public void joinRoom(Long roomId, String username, String sessionId) {
        // 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + roomId));

        if (!chatRoom.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 채팅방입니다: " + roomId);
        }

        // 사용자를 채팅방에 추가
        roomUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                 .put(username, sessionId);

        // 현재 참여자 수 업데이트
        int currentParticipants = getRoomUserCount(roomId);
        chatRoom.setCurrentParticipants(currentParticipants);
        chatRoomRepository.save(chatRoom);

        // 채팅방의 최근 메시지 히스토리를 해당 사용자에게 전송
        sendMessageHistoryToUser(roomId, username, sessionId);

        // 입장 알림 메시지 전송
        String joinMessage = username + "님이 채팅방에 입장했습니다.";
        sendSystemMessage(roomId, joinMessage);

        // 채팅방 정보 업데이트 브로드캐스트
        broadcastRoomInfo(roomId, currentParticipants);

        logger.info("사용자 채팅방 입장: username={}, roomId={}, 현재 참여자 수={}", 
                   username, roomId, currentParticipants);
    }

    /**
     * 채팅방 퇴장
     */
    public void leaveRoom(Long roomId, String username) {
        // 사용자를 채팅방에서 제거
        ConcurrentMap<String, String> users = roomUsers.get(roomId);
        if (users != null) {
            users.remove(username);
            
            // 빈 채팅방 정리
            if (users.isEmpty()) {
                roomUsers.remove(roomId);
            }
        }

        // 현재 참여자 수 업데이트
        int currentParticipants = getRoomUserCount(roomId);
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElse(null);
        if (chatRoom != null) {
            chatRoom.setCurrentParticipants(currentParticipants);
            chatRoomRepository.save(chatRoom);
        }

        // 퇴장 알림 메시지 전송
        String leaveMessage = username + "님이 채팅방에서 나갔습니다.";
        sendSystemMessage(roomId, leaveMessage);

        // 채팅방 정보 업데이트 브로드캐스트
        broadcastRoomInfo(roomId, currentParticipants);

        logger.info("사용자 채팅방 퇴장: username={}, roomId={}, 현재 참여자 수={}", 
                   username, roomId, currentParticipants);
    }

    /**
     * 타이핑 상태 브로드캐스트
     */
    public void sendTypingStatus(Long roomId, String username, boolean isTyping) {
        Map<String, Object> typingStatus = new HashMap<>();
        typingStatus.put("type", "TYPING");
        typingStatus.put("username", username);
        typingStatus.put("isTyping", isTyping);
        typingStatus.put("timestamp", LocalDateTime.now());

        // 타이핑 상태를 다른 사용자들에게만 전송 (본인 제외)
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/typing", typingStatus);

        logger.debug("타이핑 상태 브로드캐스트: roomId={}, username={}, isTyping={}", 
                    roomId, username, isTyping);
    }

    /**
     * 채팅방 정보 브로드캐스트
     */
    private void broadcastRoomInfo(Long roomId, int currentParticipants) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("type", "ROOM_INFO");
        roomInfo.put("roomId", roomId);
        roomInfo.put("currentParticipants", currentParticipants);
        roomInfo.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/info", roomInfo);
    }

    /**
     * 채팅방의 현재 사용자 수 조회
     */
    public int getRoomUserCount(Long roomId) {
        ConcurrentMap<String, String> users = roomUsers.get(roomId);
        return users != null ? users.size() : 0;
    }

    /**
     * 특정 사용자를 채팅방에서 강제 제거 (관리자 기능)
     */
    public void kickUserFromRoom(Long roomId, String username, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다: " + adminUsername));

        // 관리자 권한 확인 (향후 구현)
        // if (!hasAdminPermission(admin, roomId)) {
        //     throw new IllegalArgumentException("관리자 권한이 없습니다");
        // }

        leaveRoom(roomId, username);

        // 강제 퇴장 알림
        String kickMessage = username + "님이 관리자에 의해 퇴장되었습니다.";
        sendSystemMessage(roomId, kickMessage);

        logger.info("사용자 강제 퇴장: kicked={}, admin={}, roomId={}", 
                   username, adminUsername, roomId);
    }

    /**
     * 채팅방의 활성 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomActiveUsers(Long roomId) {
        ConcurrentMap<String, String> users = roomUsers.get(roomId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("userCount", users != null ? users.size() : 0);
        result.put("users", users != null ? users.keySet() : new String[0]);
        
        return result;
    }

    /**
     * 특정 사용자에게 채팅방 메시지 히스토리 전송 (채팅방 입장 시)
     */
    private void sendMessageHistoryToUser(Long roomId, String username, String sessionId) {
        try {
            logger.debug("메시지 히스토리 전송 시작: roomId={}, username={}", roomId, username);
            
            // 최근 메시지 50개 조회 (시간순 정렬)
            List<ChatMessage> recentMessages = chatMessageRepository.findTop50ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
            
            if (recentMessages.isEmpty()) {
                logger.debug("전송할 메시지 히스토리가 없습니다: roomId={}", roomId);
                return;
            }
            
            // 메시지를 시간순으로 정렬 (오래된 메시지부터)
            List<ChatMessageResponse> messageHistory = recentMessages.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(message -> {
                    // 사용자 정보 조회 (캐시 또는 데이터베이스)
                    User user = null;
                    if (message.getUserId() != null && message.getUserId() > 0) {
                        try {
                            user = userRepository.findById(message.getUserId()).orElse(null);
                        } catch (Exception e) {
                            logger.warn("사용자 정보 조회 실패: userId={}", message.getUserId());
                        }
                    }
                    
                    return ChatMessageResponse.from(
                        message, 
                        user != null ? user.getUsername() : (message.getUsername() != null ? message.getUsername() : "unknown"), 
                        user != null ? user.getName() : (message.getName() != null ? message.getName() : "Unknown")
                    );
                })
                .collect(Collectors.toList());
            
            // 메시지 히스토리 전송 객체 구성
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("type", "MESSAGE_HISTORY");
            historyData.put("roomId", roomId);
            historyData.put("messages", messageHistory);
            historyData.put("messageCount", messageHistory.size());
            historyData.put("timestamp", System.currentTimeMillis());
            
            // 특정 사용자의 세션에만 메시지 히스토리 전송
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/room/" + roomId + "/history", 
                historyData
            );
            
            logger.info("메시지 히스토리 전송 완료: roomId={}, username={}, 메시지 수={}", 
                       roomId, username, messageHistory.size());
            
        } catch (Exception e) {
            logger.error("메시지 히스토리 전송 실패: roomId={}, username={}, error={}", 
                        roomId, username, e.getMessage(), e);
        }
    }

    /**
     * 채팅방의 모든 메시지 히스토리 조회 (관리용)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRoomMessageHistory(Long roomId, int limit) {
        try {
            logger.debug("채팅방 메시지 히스토리 조회: roomId={}, limit={}", roomId, limit);
            
            List<ChatMessage> messages;
            if (limit > 0) {
                // 지정된 개수만큼 최근 메시지 조회
                messages = chatMessageRepository.findTop50ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            } else {
                // 모든 메시지 조회
                messages = chatMessageRepository.findTop50ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
            }
            
            // 시간순으로 정렬하여 응답 생성
            return messages.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(message -> {
                    User user = null;
                    if (message.getUserId() != null && message.getUserId() > 0) {
                        try {
                            user = userRepository.findById(message.getUserId()).orElse(null);
                        } catch (Exception e) {
                            logger.warn("사용자 정보 조회 실패: userId={}", message.getUserId());
                        }
                    }
                    
                    return ChatMessageResponse.from(
                        message,
                        user != null ? user.getUsername() : (message.getUsername() != null ? message.getUsername() : "unknown"),
                        user != null ? user.getName() : (message.getName() != null ? message.getName() : "Unknown")
                    );
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("채팅방 메시지 히스토리 조회 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            return List.of();
        }
    }
}