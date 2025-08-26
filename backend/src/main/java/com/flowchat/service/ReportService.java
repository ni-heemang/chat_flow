package com.flowchat.service;

import com.flowchat.entity.AnalysisResult;
import com.flowchat.entity.ChatMessage;
import com.flowchat.repository.AnalysisResultRepository;
import com.flowchat.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private TopicClassificationService topicClassificationService;

    /**
     * 일일 보고서 생성
     */
    public Map<String, Object> generateDailyReport(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return generateReportForPeriod(startOfDay, endOfDay, "DAILY");
    }

    /**
     * 주간 보고서 생성 (월요일~일요일)
     */
    public Map<String, Object> generateWeeklyReport(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDateTime endOfWeek = weekEnd.atTime(LocalTime.MAX);

        return generateReportForPeriod(startOfWeek, endOfWeek, "WEEKLY");
    }

    /**
     * 월간 보고서 생성
     */
    public Map<String, Object> generateMonthlyReport(int year, int month) {
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
        
        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        LocalDateTime endOfMonth = lastDayOfMonth.atTime(LocalTime.MAX);

        return generateReportForPeriod(startOfMonth, endOfMonth, "MONTHLY");
    }

    /**
     * 특정 기간에 대한 보고서 생성
     */
    private Map<String, Object> generateReportForPeriod(LocalDateTime start, LocalDateTime end, String reportType) {
        logger.info("{} 보고서 생성 시작: {} ~ {}", reportType, start, end);

        Map<String, Object> report = new HashMap<>();

        // 1. 기본 메시지 통계
        List<ChatMessage> messages = chatMessageRepository.findByTimestampBetween(start, end);
        report.put("totalMessages", messages.size());
        report.put("reportType", reportType);
        report.put("startDate", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("endDate", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (messages.isEmpty()) {
            report.put("summary", "해당 기간에는 메시지가 없습니다.");
            return report;
        }

        // 2. 사용자별 활동 통계
        Map<String, Long> userActivity = messages.stream()
                .collect(Collectors.groupingBy(
                    msg -> msg.getUsername() != null ? msg.getUsername() : "UNKNOWN",
                    Collectors.counting()
                ));
        report.put("userActivity", userActivity);

        // 3. 메시지 타입별 통계
        Map<String, Long> messageTypeStats = messages.stream()
                .collect(Collectors.groupingBy(
                    msg -> msg.getMessageType() != null ? msg.getMessageType().toString() : "TEXT",
                    Collectors.counting()
                ));
        report.put("messageTypeStats", messageTypeStats);

        // 4. 시간대별 활동 통계
        Map<Integer, Long> hourlyActivity = messages.stream()
                .collect(Collectors.groupingBy(
                    msg -> msg.getTimestamp().getHour(),
                    Collectors.counting()
                ));
        report.put("hourlyActivity", hourlyActivity);

        // 5. 주제별 분석 (시스템 메시지 제외)
        List<String> userMessages = messages.stream()
                .filter(msg -> !"SYSTEM".equals(msg.getMessageType()))
                .map(ChatMessage::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!userMessages.isEmpty()) {
            Map<String, Object> topicStats = topicClassificationService.getTopicStatistics(userMessages);
            report.put("topicAnalysis", topicStats);
        }

        // 6. 채팅방별 활동 통계
        Map<Long, Long> roomActivity = messages.stream()
                .collect(Collectors.groupingBy(
                    ChatMessage::getRoomId,
                    Collectors.counting()
                ));
        report.put("roomActivity", roomActivity);

        // 7. 가장 활발한 시간대 계산
        Optional<Map.Entry<Integer, Long>> mostActiveHour = hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        
        if (mostActiveHour.isPresent()) {
            report.put("mostActiveHour", mostActiveHour.get().getKey() + "시");
            report.put("mostActiveHourCount", mostActiveHour.get().getValue());
        }

        // 8. 요약 정보
        String summary = generateSummary(report, reportType);
        report.put("summary", summary);

        logger.info("{} 보고서 생성 완료: 총 {}개 메시지, {}명 사용자", 
                   reportType, messages.size(), userActivity.size());

        return report;
    }

    /**
     * 보고서 요약 생성
     */
    private String generateSummary(Map<String, Object> report, String reportType) {
        StringBuilder summary = new StringBuilder();
        
        Object totalMessagesObj = report.get("totalMessages");
        int totalMessages = totalMessagesObj instanceof Integer ? (Integer) totalMessagesObj : 0;
        @SuppressWarnings("unchecked")
        Map<String, Long> userActivity = (Map<String, Long>) report.get("userActivity");
        
        summary.append(String.format("📊 %s 보고서\n", getReportTypeName(reportType)));
        summary.append(String.format("• 총 메시지 수: %d개\n", totalMessages));
        summary.append(String.format("• 참여 사용자: %d명\n", userActivity.size()));
        
        if (report.containsKey("mostActiveHour")) {
            summary.append(String.format("• 가장 활발한 시간: %s (%d개 메시지)\n", 
                         report.get("mostActiveHour"), report.get("mostActiveHourCount")));
        }
        
        // 가장 활발한 사용자
        Optional<Map.Entry<String, Long>> mostActiveUser = userActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        
        if (mostActiveUser.isPresent()) {
            summary.append(String.format("• 가장 활발한 사용자: %s (%d개 메시지)\n",
                         mostActiveUser.get().getKey(), mostActiveUser.get().getValue()));
        }
        
        return summary.toString();
    }

    private String getReportTypeName(String reportType) {
        switch (reportType) {
            case "DAILY": return "일일";
            case "WEEKLY": return "주간";
            case "MONTHLY": return "월간";
            default: return reportType;
        }
    }

    /**
     * 사용자별 개인 보고서 생성
     */
    public Map<String, Object> generateUserReport(String username, LocalDateTime start, LocalDateTime end) {
        List<ChatMessage> userMessages = chatMessageRepository.findByUsernameAndTimestampBetween(username, start, end);
        
        Map<String, Object> report = new HashMap<>();
        report.put("username", username);
        report.put("totalMessages", userMessages.size());
        report.put("startDate", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("endDate", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if (!userMessages.isEmpty()) {
            // 시간대별 활동
            Map<Integer, Long> hourlyActivity = userMessages.stream()
                    .collect(Collectors.groupingBy(
                        msg -> msg.getTimestamp().getHour(),
                        Collectors.counting()
                    ));
            report.put("hourlyActivity", hourlyActivity);
            
            // 채팅방별 참여도
            Map<Long, Long> roomParticipation = userMessages.stream()
                    .collect(Collectors.groupingBy(
                        ChatMessage::getRoomId,
                        Collectors.counting()
                    ));
            report.put("roomParticipation", roomParticipation);
        }
        
        return report;
    }
}