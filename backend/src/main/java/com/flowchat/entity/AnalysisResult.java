package com.flowchat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "analysis_results", indexes = {
    @Index(name = "idx_room_id", columnList = "room_id"),
    @Index(name = "idx_analysis_type", columnList = "analysis_type"),
    @Index(name = "idx_room_type", columnList = "room_id, analysis_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AnalysisResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_id", nullable = false)
    @NotNull(message = "채팅방 ID는 필수입니다")
    private Long roomId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false)
    private AnalysisType analysisType;
    
    @Column(name = "analysis_data", columnDefinition = "JSON")
    private String analysisData;
    
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;
    
    @Column(name = "participant_count", nullable = false)
    private Integer participantCount = 0;
    
    @Column(name = "analysis_period_start", nullable = false)
    private LocalDateTime analysisPeriodStart;
    
    @Column(name = "analysis_period_end", nullable = false)
    private LocalDateTime analysisPeriodEnd;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 분석 타입 열거형
    public enum AnalysisType {
        KEYWORD_FREQUENCY,      // 키워드 빈도 분석
        TIME_PATTERN,          // 시간대별 패턴 분석
        USER_PARTICIPATION,    // 사용자 참여도 분석
        EMOTION_ANALYSIS,      // 감정 분석
        TOPIC_CLASSIFICATION,  // 주제별 분류
        DAILY_SUMMARY,         // 일일 요약
        WEEKLY_SUMMARY,        // 주간 요약
        MONTHLY_SUMMARY        // 월간 요약
    }
    
    // 기본 생성자
    protected AnalysisResult() {}
    
    // 생성자
    public AnalysisResult(Long roomId, AnalysisType analysisType, String analysisData,
                         LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.roomId = roomId;
        this.analysisType = analysisType;
        this.analysisData = analysisData;
        this.analysisPeriodStart = periodStart;
        this.analysisPeriodEnd = periodEnd;
        this.messageCount = 0;
        this.participantCount = 0;
    }
    
    public AnalysisResult(Long roomId, AnalysisType analysisType, String analysisData,
                         Integer messageCount, Integer participantCount,
                         LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.roomId = roomId;
        this.analysisType = analysisType;
        this.analysisData = analysisData;
        this.messageCount = messageCount;
        this.participantCount = participantCount;
        this.analysisPeriodStart = periodStart;
        this.analysisPeriodEnd = periodEnd;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public AnalysisType getAnalysisType() {
        return analysisType;
    }
    
    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }
    
    public String getAnalysisData() {
        return analysisData;
    }
    
    public void setAnalysisData(String analysisData) {
        this.analysisData = analysisData;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public Integer getParticipantCount() {
        return participantCount;
    }
    
    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }
    
    public LocalDateTime getAnalysisPeriodStart() {
        return analysisPeriodStart;
    }
    
    public void setAnalysisPeriodStart(LocalDateTime analysisPeriodStart) {
        this.analysisPeriodStart = analysisPeriodStart;
    }
    
    public LocalDateTime getAnalysisPeriodEnd() {
        return analysisPeriodEnd;
    }
    
    public void setAnalysisPeriodEnd(LocalDateTime analysisPeriodEnd) {
        this.analysisPeriodEnd = analysisPeriodEnd;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // 비즈니스 메서드
    public void updateAnalysisData(String newAnalysisData) {
        this.analysisData = newAnalysisData;
    }
    
    public void updateCounts(Integer messageCount, Integer participantCount) {
        this.messageCount = messageCount;
        this.participantCount = participantCount;
    }
    
    public boolean isRealtimeAnalysis() {
        return analysisType == AnalysisType.KEYWORD_FREQUENCY ||
               analysisType == AnalysisType.TIME_PATTERN ||
               analysisType == AnalysisType.USER_PARTICIPATION;
    }
    
    public boolean isSummaryAnalysis() {
        return analysisType == AnalysisType.DAILY_SUMMARY ||
               analysisType == AnalysisType.WEEKLY_SUMMARY ||
               analysisType == AnalysisType.MONTHLY_SUMMARY;
    }
    
    public boolean isAdvancedAnalysis() {
        return analysisType == AnalysisType.EMOTION_ANALYSIS ||
               analysisType == AnalysisType.TOPIC_CLASSIFICATION;
    }
    
    // 정적 팩토리 메서드
    public static AnalysisResult createKeywordAnalysis(Long roomId, String analysisData,
                                                      Integer messageCount, Integer participantCount,
                                                      LocalDateTime periodStart, LocalDateTime periodEnd) {
        return new AnalysisResult(roomId, AnalysisType.KEYWORD_FREQUENCY, analysisData,
                                messageCount, participantCount, periodStart, periodEnd);
    }
    
    public static AnalysisResult createTimePatternAnalysis(Long roomId, String analysisData,
                                                          LocalDateTime periodStart, LocalDateTime periodEnd) {
        return new AnalysisResult(roomId, AnalysisType.TIME_PATTERN, analysisData, periodStart, periodEnd);
    }
    
    public static AnalysisResult createParticipationAnalysis(Long roomId, String analysisData,
                                                           Integer participantCount,
                                                           LocalDateTime periodStart, LocalDateTime periodEnd) {
        AnalysisResult result = new AnalysisResult(roomId, AnalysisType.USER_PARTICIPATION, analysisData, periodStart, periodEnd);
        result.setParticipantCount(participantCount);
        return result;
    }
    
    public static AnalysisResult createTopicAnalysis(Long roomId, String analysisData,
                                                    LocalDateTime periodStart, LocalDateTime periodEnd) {
        return new AnalysisResult(roomId, AnalysisType.TOPIC_CLASSIFICATION, analysisData, periodStart, periodEnd);
    }
    
    public static AnalysisResult createEmotionAnalysis(Long roomId, String analysisData,
                                                      LocalDateTime periodStart, LocalDateTime periodEnd) {
        return new AnalysisResult(roomId, AnalysisType.EMOTION_ANALYSIS, analysisData, periodStart, periodEnd);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisResult that = (AnalysisResult) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AnalysisResult{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", analysisType=" + analysisType +
                ", messageCount=" + messageCount +
                ", participantCount=" + participantCount +
                ", analysisPeriodStart=" + analysisPeriodStart +
                ", analysisPeriodEnd=" + analysisPeriodEnd +
                ", createdAt=" + createdAt +
                '}';
    }
}