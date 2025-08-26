package com.flowchat.handler;

import com.flowchat.config.JwtConfig;
import com.flowchat.service.UserService;
import com.flowchat.service.ChatRoomService;
import com.flowchat.service.ChatRoomMemberService;
import com.flowchat.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebSocketEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventHandler.class);

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;
    
    @Autowired 
    private ChatRoomService chatRoomService;
    
    @Autowired
    private ChatRoomMemberService chatRoomMemberService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 세션 ID와 사용자 정보 매핑
    private final ConcurrentMap<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    // 채팅방별 사용자 세션 관리
    private final ConcurrentMap<String, ConcurrentMap<String, String>> roomSessionMap = new ConcurrentHashMap<>();
    
    // 사용자별 참여 중인 채팅방 관리 (중복 참여 방지)
    private final ConcurrentMap<String, ConcurrentMap<String, Boolean>> userRoomMap = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결 시작 시 호출
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.debug("WebSocket 연결 시도: sessionId={}", sessionId);
        
        // JWT 토큰을 통한 인증
        String token = getTokenFromHeaders(headerAccessor);
        if (token != null) {
            try {
                String username = jwtConfig.getUsernameFromToken(token);
                
                // 토큰 유효성 검증
                if (jwtConfig.validateToken(token, username)) {
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    // 인증 정보 설정
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    headerAccessor.setUser(authentication);
                    sessionUserMap.put(sessionId, username);
                    
                    logger.info("WebSocket 인증 성공: sessionId={}, username={}", sessionId, username);
                } else {
                    logger.warn("WebSocket 토큰 검증 실패: sessionId={}, username={}", sessionId, username);
                }
            } catch (Exception e) {
                logger.warn("WebSocket 인증 실패: sessionId={}, error={}", sessionId, e.getMessage());
            }
        } else {
            logger.warn("WebSocket 연결 시 토큰이 없음: sessionId={}", sessionId);
        }
    }

    /**
     * WebSocket 연결 완료 시 호출
     */
    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = sessionUserMap.get(sessionId);
        
        logger.info("WebSocket 연결 완료: sessionId={}, username={}", sessionId, username);
    }

    /**
     * WebSocket 연결 해제 시 호출
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = sessionUserMap.get(sessionId);
        
        if (username != null) {
            // 사용자가 접속 중인 모든 채팅방에서 오프라인 처리
            try {
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    // 모든 채팅방에서 오프라인 상태로 변경
                    chatRoomMemberService.updateUserOnlineStatus(user.getId(), false);
                }
            } catch (Exception e) {
                logger.error("사용자 오프라인 상태 업데이트 중 오류: username={}, error={}", username, e.getMessage());
            }
            
            // 세션 관리에서 채팅방별 제거
            ConcurrentMap<String, Boolean> userRooms = userRoomMap.get(username);
            if (userRooms != null) {
                for (String roomId : userRooms.keySet()) {
                    removeUserFromRoom(roomId, username);
                }
            }
            
            // 세션 정보 제거
            sessionUserMap.remove(sessionId);
            
            // 모든 채팅방에서 해당 세션 제거
            roomSessionMap.values().forEach(roomSessions -> 
                roomSessions.entrySet().removeIf(entry -> entry.getValue().equals(sessionId))
            );
            
            logger.info("WebSocket 연결 해제: sessionId={}, username={}", sessionId, username);
        }
    }

    /**
     * 채팅방에 WebSocket 세션 추가 (멤버십 생성 안함)
     */
    public void addUserToRoom(String roomId, String sessionId, String username) {
        // WebSocket 세션 관리만 수행
        roomSessionMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                     .put(username, sessionId);
        
        // 사용자별 접속 중인 채팅방 기록
        boolean isFirstConnection = userRoomMap.computeIfAbsent(username, k -> new ConcurrentHashMap<>())
                                               .putIfAbsent(roomId, true) == null;
        
        try {
            // 사용자 ID 조회
            User user = userService.getUserByUsername(username);
            if (user != null) {
                // 멤버인 경우에만 온라인 상태 업데이트
                boolean isMember = chatRoomMemberService.isMemberOfRoom(Long.parseLong(roomId), user.getId());
                if (isMember) {
                    chatRoomMemberService.updateUserOnlineStatusInRoom(Long.parseLong(roomId), user.getId(), true);
                    logger.info("멤버 사용자 온라인 상태 업데이트: roomId={}, username={}", roomId, username);
                    
                    // 채팅방 멤버 목록 업데이트 알림
                    notifyMemberStatusChange(roomId, "USER_ONLINE", username);
                }
                
                if (isFirstConnection) {
                    logger.info("사용자가 채팅방에 WebSocket 연결: roomId={}, username={}, isMember={}", roomId, username, isMember);
                } else {
                    logger.debug("사용자 추가 WebSocket 세션: roomId={}, username={}, sessionId={}", roomId, username, sessionId);
                }
            }
        } catch (Exception e) {
            logger.error("채팅방 WebSocket 연결 처리 중 오류: roomId={}, username={}, error={}", roomId, username, e.getMessage());
        }
    }

    /**
     * 채팅방에서 WebSocket 세션 제거
     */
    public void removeUserFromRoom(String roomId, String username) {
        ConcurrentMap<String, String> roomSessions = roomSessionMap.get(roomId);
        if (roomSessions != null) {
            roomSessions.remove(username);
            
            // 빈 채팅방 정리
            if (roomSessions.isEmpty()) {
                roomSessionMap.remove(roomId);
            }
        }
        
        // 사용자가 해당 채팅방에서 WebSocket 연결 해제
        ConcurrentMap<String, Boolean> userRooms = userRoomMap.get(username);
        if (userRooms != null && userRooms.remove(roomId) != null) {
            try {
                // 사용자 ID 조회
                User user = userService.getUserByUsername(username);
                if (user != null) {
                    // 멤버인 경우에만 오프라인 상태 업데이트
                    boolean isMember = chatRoomMemberService.isMemberOfRoom(Long.parseLong(roomId), user.getId());
                    if (isMember) {
                        chatRoomMemberService.updateUserOnlineStatusInRoom(Long.parseLong(roomId), user.getId(), false);
                        logger.info("멤버 사용자 오프라인 상태 업데이트: roomId={}, username={}", roomId, username);
                        
                        // 채팅방 멤버 목록 업데이트 알림
                        notifyMemberStatusChange(roomId, "USER_OFFLINE", username);
                    }
                    
                    logger.info("사용자가 채팅방에서 WebSocket 연겴 해제: roomId={}, username={}, isMember={}", roomId, username, isMember);
                }
            } catch (Exception e) {
                logger.error("채팅방 WebSocket 연겴 해제 처리 중 오류: roomId={}, username={}, error={}", roomId, username, e.getMessage());
            }
            
            // 빈 사용자 룸 맵 정리
            if (userRooms.isEmpty()) {
                userRoomMap.remove(username);
            }
        }
    }

    /**
     * 채팅방의 실시간 접속 사용자 수 조회 (WebSocket 세션 기준)
     */
    public int getRoomSessionCount(String roomId) {
        ConcurrentMap<String, String> roomSessions = roomSessionMap.get(roomId);
        return roomSessions != null ? roomSessions.size() : 0;
    }
    
    /**
     * 채팅방의 온라인 사용자 수 조회 (DB 기준)
     */
    public long getRoomOnlineUserCount(String roomId) {
        try {
            return chatRoomMemberService.getRoomOnlineCount(Long.parseLong(roomId));
        } catch (Exception e) {
            logger.error("채팅방 온라인 사용자 수 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 채팅방의 전체 멤버 수 조회 (DB 기준)
     */
    public long getRoomMemberCount(String roomId) {
        try {
            return chatRoomMemberService.getRoomMemberCount(Long.parseLong(roomId));
        } catch (Exception e) {
            logger.error("채팅방 멤버 수 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return 0;
        }
    }

    /**
     * 세션 ID로 사용자명 조회
     */
    public String getUsernameBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * 헤더에서 JWT 토큰 추출
     */
    private String getTokenFromHeaders(StompHeaderAccessor headerAccessor) {
        // Authorization 헤더에서 토큰 추출
        String authHeader = headerAccessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 쿼리 파라미터에서 토큰 추출 (폴백)
        String token = headerAccessor.getFirstNativeHeader("token");
        return token;
    }
    
    /**
     * 채팅방 멤버 상태 변경 알림
     */
    private void notifyMemberStatusChange(String roomId, String eventType, String username) {
        try {
            // 멤버 상태 변경 알림 객체 생성
            final String finalEventType = eventType;
            final String finalUsername = username;
            
            Object memberUpdate = new Object() {
                public final String type = finalEventType;
                public final String username = finalUsername;
                public final long timestamp = System.currentTimeMillis();
            };
            
            // 채팅방 멤버들에게 상태 변경 알림
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/members", memberUpdate);
            
            logger.debug("멤버 상태 변경 알림 전송: roomId={}, eventType={}, username={}", roomId, eventType, username);
        } catch (Exception e) {
            logger.error("멤버 상태 변경 알림 전송 실패: roomId={}, eventType={}, username={}, error={}", 
                        roomId, eventType, username, e.getMessage());
        }
    }
}