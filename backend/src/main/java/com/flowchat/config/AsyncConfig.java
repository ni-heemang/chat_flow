package com.flowchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring의 @Async 기능을 활성화하여 비동기 이벤트 처리 가능
}