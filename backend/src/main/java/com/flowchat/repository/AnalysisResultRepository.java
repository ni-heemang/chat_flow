package com.flowchat.repository;

import com.flowchat.entity.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    
    /**
     * 특정 채팅방의 분석 결과 조회
     */
    List<AnalysisResult> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    
    /**
     * 특정 채팅방의 특정 타입 분석 결과 조회
     */
    List<AnalysisResult> findByRoomIdAndAnalysisTypeOrderByCreatedAtDesc(Long roomId, AnalysisResult.AnalysisType analysisType);
    
    /**
     * 특정 채팅방의 최신 분석 결과 조회 (타입별)
     */
    Optional<AnalysisResult> findFirstByRoomIdAndAnalysisTypeOrderByCreatedAtDesc(Long roomId, AnalysisResult.AnalysisType analysisType);
    
    /**
     * 특정 기간 동안의 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisPeriodStart >= :startDate AND ar.analysisPeriodEnd <= :endDate " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findByRoomIdAndPeriodBetween(@Param("roomId") Long roomId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 타입의 모든 분석 결과 조회
     */
    List<AnalysisResult> findByAnalysisTypeOrderByCreatedAtDesc(AnalysisResult.AnalysisType analysisType);
    
    /**
     * 실시간 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisType IN ('KEYWORD_FREQUENCY', 'TIME_PATTERN', 'USER_PARTICIPATION') " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findRealtimeAnalysisByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 요약 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisType IN ('DAILY_SUMMARY', 'WEEKLY_SUMMARY', 'MONTHLY_SUMMARY') " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findSummaryAnalysisByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 고급 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisType IN ('EMOTION_ANALYSIS', 'TOPIC_CLASSIFICATION') " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findAdvancedAnalysisByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 최근 24시간 내 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.createdAt >= :since ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findRecentAnalysis(@Param("roomId") Long roomId, @Param("since") LocalDateTime since);
    
    /**
     * 특정 날짜 이후의 분석 결과 조회
     */
    List<AnalysisResult> findByRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(Long roomId, LocalDateTime createdAt);
    
    /**
     * 특정 채팅방의 분석 결과 수 조회
     */
    long countByRoomId(Long roomId);
    
    /**
     * 특정 타입의 분석 결과 수 조회
     */
    long countByAnalysisType(AnalysisResult.AnalysisType analysisType);
    
    /**
     * 특정 채팅방의 특정 타입 분석 결과 수 조회
     */
    long countByRoomIdAndAnalysisType(Long roomId, AnalysisResult.AnalysisType analysisType);
    
    /**
     * 오늘 생성된 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE FUNCTION('DATE', ar.createdAt) = CURRENT_DATE " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findTodayAnalysis();
    
    /**
     * 특정 기간의 분석 통계
     */
    @Query("SELECT ar.analysisType, COUNT(ar) FROM AnalysisResult ar " +
           "WHERE ar.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY ar.analysisType")
    List<Object[]> getAnalysisStatsByPeriod(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 채팅방별 분석 통계
     */
    @Query("SELECT ar.roomId, ar.analysisType, COUNT(ar) FROM AnalysisResult ar " +
           "WHERE ar.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY ar.roomId, ar.analysisType ORDER BY ar.roomId")
    List<Object[]> getAnalysisStatsByRoom(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * 가장 활발한 분석이 이루어진 채팅방 조회
     */
    @Query("SELECT ar.roomId, COUNT(ar) as analysisCount FROM AnalysisResult ar " +
           "WHERE ar.createdAt >= :since GROUP BY ar.roomId " +
           "ORDER BY analysisCount DESC")
    List<Object[]> getMostAnalyzedRooms(@Param("since") LocalDateTime since);
    
    /**
     * 오래된 분석 결과 조회 (정리용)
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.createdAt < :before " +
           "ORDER BY ar.createdAt ASC")
    List<AnalysisResult> findOldAnalysisResults(@Param("before") LocalDateTime before);
    
    /**
     * 페이징을 지원하는 채팅방별 분석 결과 조회
     */
    Page<AnalysisResult> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
    
    /**
     * 페이징을 지원하는 채팅방별 특정 타입 분석 결과 조회
     */
    Page<AnalysisResult> findByRoomIdAndAnalysisTypeOrderByCreatedAtDesc(Long roomId, AnalysisResult.AnalysisType analysisType, Pageable pageable);
    
    /**
     * 채팅방의 최신 분석 결과 하나만 조회
     */
    AnalysisResult findTopByRoomIdOrderByCreatedAtDesc(Long roomId);
    
    /**
     * 기간별 분석 결과 조회 (특정 타입)
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisType = :analysisType AND " +
           "ar.analysisPeriodStart >= :startDate AND ar.analysisPeriodEnd <= :endDate " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findByRoomIdAndAnalysisTypeAndAnalysisPeriodStartBetweenOrderByCreatedAtDesc(
        @Param("roomId") Long roomId,
        @Param("analysisType") AnalysisResult.AnalysisType analysisType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * 기간별 분석 결과 조회 (모든 타입)
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.roomId = :roomId AND " +
           "ar.analysisPeriodStart >= :startDate AND ar.analysisPeriodEnd <= :endDate " +
           "ORDER BY ar.createdAt DESC")
    List<AnalysisResult> findByRoomIdAndAnalysisPeriodStartBetweenOrderByCreatedAtDesc(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
}