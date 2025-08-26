package com.flowchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring의 @Scheduled 기능을 활성화하여 정기적 분석 업데이트 가능
}