package com.flowchat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "애플리케이션 상태 확인 API")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * 기본 헬스체크
     */
    @GetMapping
    @Operation(summary = "기본 헬스체크", description = "애플리케이션 기본 상태 확인")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "flowchat-backend");
        health.put("version", "0.0.1-SNAPSHOT");
        
        return ResponseEntity.ok(health);
    }

    /**
     * 상세 헬스체크 (데이터베이스 연결 포함)
     */
    @GetMapping("/detailed")
    @Operation(summary = "상세 헬스체크", description = "데이터베이스 연결 상태를 포함한 상세 상태 확인")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "flowchat-backend");
        health.put("version", "0.0.1-SNAPSHOT");
        
        // 애플리케이션 상태
        health.put("application", Map.of("status", "UP"));
        
        // 데이터베이스 상태 확인
        Map<String, Object> dbStatus = checkDatabaseHealth();
        health.put("database", dbStatus);
        
        // 전체 상태 결정
        boolean isHealthy = "UP".equals(dbStatus.get("status"));
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(health);
    }

    /**
     * 데이터베이스 연결 상태 확인
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                dbHealth.put("status", "UP");
                dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
                dbHealth.put("version", connection.getMetaData().getDatabaseProductVersion());
            } else {
                dbHealth.put("status", "DOWN");
                dbHealth.put("error", "Database connection is not valid");
            }
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }

    /**
     * 준비 상태 확인 (Readiness probe)
     */
    @GetMapping("/ready")
    @Operation(summary = "준비 상태 확인", description = "애플리케이션이 요청을 처리할 준비가 되었는지 확인")
    public ResponseEntity<Map<String, String>> ready() {
        Map<String, String> status = new HashMap<>();
        
        // 데이터베이스 연결 확인
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                status.put("status", "READY");
                return ResponseEntity.ok(status);
            } else {
                status.put("status", "NOT_READY");
                status.put("reason", "Database not available");
                return ResponseEntity.status(503).body(status);
            }
        } catch (Exception e) {
            status.put("status", "NOT_READY");
            status.put("reason", "Database connection failed: " + e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    /**
     * 활성 상태 확인 (Liveness probe)
     */
    @GetMapping("/live")
    @Operation(summary = "활성 상태 확인", description = "애플리케이션이 살아있는지 확인")
    public ResponseEntity<Map<String, String>> live() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ALIVE");
        status.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }
}