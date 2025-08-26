package com.flowchat.controller;

import com.flowchat.scheduler.ReportScheduler;
import com.flowchat.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "보고서", description = "채팅 활동 보고서 생성 및 조회 API")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportScheduler reportScheduler;

    /**
     * 일일 보고서 조회
     */
    @GetMapping("/daily")
    @Operation(summary = "일일 보고서 조회", description = "특정 날짜의 일일 채팅 활동 보고서를 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 성공")
    public ResponseEntity<Map<String, Object>> getDailyReport(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", example = "2024-08-22")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        
        try {
            logger.info("일일 보고서 요청: {} by {}", date, authentication.getName());
            Map<String, Object> report = reportService.generateDailyReport(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("일일 보고서 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 주간 보고서 조회
     */
    @GetMapping("/weekly")
    @Operation(summary = "주간 보고서 조회", description = "특정 주의 주간 채팅 활동 보고서를 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 성공")
    public ResponseEntity<Map<String, Object>> getWeeklyReport(
            @Parameter(description = "주 시작일 (월요일, YYYY-MM-DD)", example = "2024-08-19")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        
        try {
            logger.info("주간 보고서 요청: {} by {}", weekStart, authentication.getName());
            Map<String, Object> report = reportService.generateWeeklyReport(weekStart);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("주간 보고서 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 월간 보고서 조회
     */
    @GetMapping("/monthly")
    @Operation(summary = "월간 보고서 조회", description = "특정 월의 월간 채팅 활동 보고서를 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 성공")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @Parameter(description = "연도", example = "2024")
            @RequestParam int year,
            @Parameter(description = "월", example = "8")
            @RequestParam int month,
            Authentication authentication) {
        
        try {
            logger.info("월간 보고서 요청: {}-{:02d} by {}", year, month, authentication.getName());
            Map<String, Object> report = reportService.generateMonthlyReport(year, month);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("월간 보고서 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자별 개인 보고서 조회
     */
    @GetMapping("/user/{username}")
    @Operation(summary = "사용자 개인 보고서", description = "특정 사용자의 개인 활동 보고서를 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 성공")
    public ResponseEntity<Map<String, Object>> getUserReport(
            @Parameter(description = "사용자명")
            @PathVariable String username,
            @Parameter(description = "시작 일시 (YYYY-MM-DDTHH:mm:ss)", example = "2024-08-22T00:00:00")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime start,
            @Parameter(description = "종료 일시 (YYYY-MM-DDTHH:mm:ss)", example = "2024-08-22T23:59:59")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime end,
            Authentication authentication) {
        
        try {
            logger.info("사용자 보고서 요청: {} ({} ~ {}) by {}", username, start, end, authentication.getName());
            Map<String, Object> report = reportService.generateUserReport(username, start, end);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("사용자 보고서 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 수동 보고서 생성 트리거 (관리자용)
     */
    @PostMapping("/trigger/daily")
    @Operation(summary = "일일 보고서 수동 생성", description = "특정 날짜의 일일 보고서를 수동으로 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 트리거 성공")
    public ResponseEntity<String> triggerDailyReport(
            @Parameter(description = "생성할 보고서 날짜 (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        
        try {
            logger.info("수동 일일 보고서 생성 트리거: {} by {}", date, authentication.getName());
            reportScheduler.triggerDailyReport(date);
            return ResponseEntity.ok("일일 보고서 생성이 시작되었습니다.");
        } catch (Exception e) {
            logger.error("수동 일일 보고서 생성 트리거 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("보고서 생성 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/trigger/weekly")
    @Operation(summary = "주간 보고서 수동 생성", description = "특정 주의 주간 보고서를 수동으로 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 트리거 성공")
    public ResponseEntity<String> triggerWeeklyReport(
            @Parameter(description = "주 시작일 (월요일, YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        
        try {
            logger.info("수동 주간 보고서 생성 트리거: {} by {}", weekStart, authentication.getName());
            reportScheduler.triggerWeeklyReport(weekStart);
            return ResponseEntity.ok("주간 보고서 생성이 시작되었습니다.");
        } catch (Exception e) {
            logger.error("수동 주간 보고서 생성 트리거 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("보고서 생성 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/trigger/monthly")
    @Operation(summary = "월간 보고서 수동 생성", description = "특정 월의 월간 보고서를 수동으로 생성합니다")
    @ApiResponse(responseCode = "200", description = "보고서 생성 트리거 성공")
    public ResponseEntity<String> triggerMonthlyReport(
            @Parameter(description = "연도") @RequestParam int year,
            @Parameter(description = "월") @RequestParam int month,
            Authentication authentication) {
        
        try {
            logger.info("수동 월간 보고서 생성 트리거: {}-{:02d} by {}", year, month, authentication.getName());
            reportScheduler.triggerMonthlyReport(year, month);
            return ResponseEntity.ok("월간 보고서 생성이 시작되었습니다.");
        } catch (Exception e) {
            logger.error("수동 월간 보고서 생성 트리거 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("보고서 생성 중 오류가 발생했습니다.");
        }
    }
}