package com.flowchat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 설정
        // /topic - 일대다 메시지 (채팅방 브로드캐스트)
        // /queue - 일대일 메시지 (개인 메시지)
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 메시지 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // 사용자별 메시지 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",
                    "http://localhost:5173", 
                    "http://localhost:5174",
                    "http://localhost:8080"
                )
                .withSockJS(); // SockJS 폴백 지원
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // WebSocket 메시지 인바운드 채널에 JWT 인증 인터셉터 등록
        registration.interceptors(webSocketAuthInterceptor);
    }
}