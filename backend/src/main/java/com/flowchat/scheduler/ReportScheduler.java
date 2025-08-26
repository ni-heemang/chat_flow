package com.flowchat.scheduler;

import com.flowchat.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@Component
public class ReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduler.class);

    @Autowired
    private ReportService reportService;

    /**
     * 매일 자정에 일일 보고서 생성 (전날 데이터)
     * 크론 표현식: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyReport() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            logger.info("일일 보고서 생성 시작: {}", yesterday);
            
            Map<String, Object> report = reportService.generateDailyReport(yesterday);
            
            // 보고서 결과 로깅
            logReportSummary("일일", report);
            
            // TODO: 생성된 보고서를 파일로 저장하거나 이메일로 발송
            // saveReportToFile(report, "daily_" + yesterday.toString());
            // sendReportByEmail(report, "일일 보고서");
            
        } catch (Exception e) {
            logger.error("일일 보고서 생성 중 오류 발생", e);
        }
    }

    /**
     * 매주 월요일 오전 1시에 주간 보고서 생성 (지난 주 데이터)
     */
    @Scheduled(cron = "0 0 1 * * MON")
    public void generateWeeklyReport() {
        try {
            LocalDate lastWeekStart = LocalDate.now()
                    .minusWeeks(1)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    
            logger.info("주간 보고서 생성 시작: {} 주차", lastWeekStart);
            
            Map<String, Object> report = reportService.generateWeeklyReport(lastWeekStart);
            
            // 보고서 결과 로깅
            logReportSummary("주간", report);
            
            // TODO: 생성된 보고서를 파일로 저장하거나 이메일로 발송
            // saveReportToFile(report, "weekly_" + lastWeekStart.toString());
            // sendReportByEmail(report, "주간 보고서");
            
        } catch (Exception e) {
            logger.error("주간 보고서 생성 중 오류 발생", e);
        }
    }

    /**
     * 매월 1일 오전 2시에 월간 보고서 생성 (지난 달 데이터)
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyReport() {
        try {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            int year = lastMonth.getYear();
            int month = lastMonth.getMonthValue();
            
            logger.info("월간 보고서 생성 시작: {}-{:02d}", year, month);
            
            Map<String, Object> report = reportService.generateMonthlyReport(year, month);
            
            // 보고서 결과 로깅
            logReportSummary("월간", report);
            
            // TODO: 생성된 보고서를 파일로 저장하거나 이메일로 발송
            // saveReportToFile(report, String.format("monthly_%d-%02d", year, month));
            // sendReportByEmail(report, "월간 보고서");
            
        } catch (Exception e) {
            logger.error("월간 보고서 생성 중 오류 발생", e);
        }
    }

    /**
     * 테스트용 즉시 보고서 생성 (개발 시에만 활용)
     * 매 10분마다 실행 (운영 환경에서는 비활성화 권장)
     */
    // @Scheduled(fixedRate = 600000) // 10분마다 실행 (개발용)
    public void generateTestReport() {
        try {
            LocalDate today = LocalDate.now();
            logger.info("테스트 보고서 생성: {}", today);
            
            Map<String, Object> report = reportService.generateDailyReport(today);
            logReportSummary("테스트", report);
            
        } catch (Exception e) {
            logger.error("테스트 보고서 생성 중 오류 발생", e);
        }
    }

    /**
     * 보고서 요약 정보를 로그에 출력
     */
    private void logReportSummary(String reportType, Map<String, Object> report) {
        if (report != null) {
            Long totalMessages = (Long) report.get("totalMessages");
            String summary = (String) report.get("summary");
            
            logger.info("=== {} 보고서 생성 완료 ===", reportType);
            logger.info("총 메시지 수: {}", totalMessages);
            
            if (summary != null) {
                logger.info("보고서 요약:\n{}", summary);
            }
            
            logger.info("보고서 생성 시간: {}", report.get("generatedAt"));
            logger.info("=== {} 보고서 완료 ===", reportType);
        }
    }

    /**
     * 수동으로 보고서 생성 트리거 (관리자용)
     */
    public void triggerDailyReport(LocalDate date) {
        logger.info("수동 일일 보고서 생성 요청: {}", date);
        try {
            Map<String, Object> report = reportService.generateDailyReport(date);
            logReportSummary("수동 일일", report);
        } catch (Exception e) {
            logger.error("수동 일일 보고서 생성 중 오류 발생", e);
        }
    }

    public void triggerWeeklyReport(LocalDate weekStart) {
        logger.info("수동 주간 보고서 생성 요청: {}", weekStart);
        try {
            Map<String, Object> report = reportService.generateWeeklyReport(weekStart);
            logReportSummary("수동 주간", report);
        } catch (Exception e) {
            logger.error("수동 주간 보고서 생성 중 오류 발생", e);
        }
    }

    public void triggerMonthlyReport(int year, int month) {
        logger.info("수동 월간 보고서 생성 요청: {}-{:02d}", year, month);
        try {
            Map<String, Object> report = reportService.generateMonthlyReport(year, month);
            logReportSummary("수동 월간", report);
        } catch (Exception e) {
            logger.error("수동 월간 보고서 생성 중 오류 발생", e);
        }
    }
}