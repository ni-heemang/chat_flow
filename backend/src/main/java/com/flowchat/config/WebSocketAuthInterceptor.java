package com.flowchat.config;

import com.flowchat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // WebSocket 연결 시 JWT 토큰 검증
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    // JWT 토큰에서 사용자명 추출
                    String username = jwtConfig.getUsernameFromToken(token);
                    
                    // 토큰 유효성 검증
                    if (jwtConfig.validateToken(token, username)) {
                        // 사용자 인증 정보 설정
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                            username, 
                            null, 
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        
                        // WebSocket 세션에 인증 정보 저장
                        accessor.setUser(auth);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        
                        logger.debug("WebSocket JWT 인증 성공: username={}, sessionId={}", 
                                   username, accessor.getSessionId());
                    } else {
                        logger.warn("WebSocket JWT 토큰 유효성 검증 실패: sessionId={}", 
                                  accessor.getSessionId());
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    logger.error("WebSocket JWT 인증 실패: sessionId={}, error={}", 
                               accessor.getSessionId(), e.getMessage());
                    throw new IllegalArgumentException("JWT authentication failed", e);
                }
            } else {
                logger.warn("WebSocket 연결에 Authorization 헤더가 없음: sessionId={}", 
                          accessor.getSessionId());
                throw new IllegalArgumentException("Missing Authorization header");
            }
        }
        
        return message;
    }
}