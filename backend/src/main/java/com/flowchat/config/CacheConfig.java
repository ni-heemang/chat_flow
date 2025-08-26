package com.flowchat.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 메모리 기반 캐시 매니저 (개발용)
        // 운영환경에서는 Redis나 Ehcache 사용 권장
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 캐시 이름들 미리 등록
        cacheManager.setCacheNames(List.of(
            "roomKeywordStats",
            "roomParticipationStats", 
            "roomHourlyStats",
            "roomAnalysisSummary",
            "advancedAnalysisSummary"
        ));
        
        return cacheManager;
    }
}