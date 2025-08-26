package com.flowchat.repository;

import com.flowchat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 특정 채팅방의 메시지 조회 (페이징)
     */
    Page<ChatMessage> findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long roomId, Pageable pageable);
    
    /**
     * 특정 채팅방의 최근 메시지 조회
     */
    List<ChatMessage> findTop50ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long roomId);
    
    /**
     * 특정 사용자의 메시지 조회
     */
    List<ChatMessage> findByUserIdAndIsDeletedFalseOrderByTimestampDesc(Long userId);
    
    /**
     * 특정 기간 동안의 채팅방 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startTime AND :endTime ORDER BY cm.timestamp ASC")
    List<ChatMessage> findByRoomIdAndTimestampBetween(@Param("roomId") Long roomId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 채팅방의 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.isDeleted = false")
    long countByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 사용자의 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.userId = :userId AND cm.isDeleted = false")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 채팅방에서 특정 사용자의 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.userId = :userId AND cm.isDeleted = false")
    long countByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * 특정 기간 동안의 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startTime AND :endTime")
    long countByRoomIdAndTimestampBetween(@Param("roomId") Long roomId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 시간대별 메시지 수 통계
     */
    @Query("SELECT FUNCTION('HOUR', cm.timestamp) as hour, COUNT(cm) as count FROM ChatMessage cm " +
           "WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "FUNCTION('DATE', cm.timestamp) = CURRENT_DATE GROUP BY FUNCTION('HOUR', cm.timestamp) ORDER BY hour")
    List<Object[]> getHourlyMessageStats(@Param("roomId") Long roomId);
    
    /**
     * 사용자별 메시지 통계
     */
    @Query("SELECT cm.userId, COUNT(cm) as count FROM ChatMessage cm " +
           "WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY cm.userId ORDER BY count DESC")
    List<Object[]> getUserMessageStats(@Param("roomId") Long roomId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 키워드가 포함된 메시지 검색
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "LOWER(cm.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY cm.timestamp DESC")
    List<ChatMessage> searchByKeyword(@Param("roomId") Long roomId, @Param("keyword") String keyword);
    
    /**
     * 특정 메시지 타입의 메시지 조회
     */
    List<ChatMessage> findByRoomIdAndMessageTypeAndIsDeletedFalseOrderByTimestampDesc(
        Long roomId, ChatMessage.MessageType messageType);
    
    /**
     * 최근 삭제되지 않은 메시지 조회
     */
    @Query(value = "SELECT TOP 1 * FROM chat_messages cm WHERE cm.room_id = :roomId AND cm.is_deleted = false " +
           "ORDER BY cm.timestamp DESC", nativeQuery = true)
    ChatMessage findLatestMessageByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 채팅방의 일일 메시지 수 통계
     */
    @Query("SELECT FUNCTION('DATE', cm.timestamp) as date, COUNT(cm) as count FROM ChatMessage cm " +
           "WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', cm.timestamp) ORDER BY date DESC")
    List<Object[]> getDailyMessageStats(@Param("roomId") Long roomId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * 활성 사용자 목록 (최근 메시지를 보낸 사용자)
     */
    @Query("SELECT DISTINCT cm.userId FROM ChatMessage cm " +
           "WHERE cm.roomId = :roomId AND cm.isDeleted = false AND " +
           "cm.timestamp >= :since ORDER BY cm.timestamp DESC")
    List<Long> findActiveUserIds(@Param("roomId") Long roomId, @Param("since") LocalDateTime since);
    
    /**
     * 오늘의 전체 메시지 수
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.isDeleted = false AND " +
           "FUNCTION('DATE', cm.timestamp) = CURRENT_DATE")
    long countTodayMessages();
    
    /**
     * 특정 기간의 전체 메시지 조회 (보고서용)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startTime AND :endTime ORDER BY cm.timestamp ASC")
    List<ChatMessage> findByTimestampBetween(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 사용자의 특정 기간 메시지 조회 (보고서용)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.username = :username AND cm.isDeleted = false AND " +
           "cm.timestamp BETWEEN :startTime AND :endTime ORDER BY cm.timestamp ASC")
    List<ChatMessage> findByUsernameAndTimestampBetween(@Param("username") String username,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 특정 채팅방의 특정 날짜 이후 메시지 조회 (분석용)
     */
    List<ChatMessage> findByRoomIdAndMessageTypeAndIsDeletedFalseAndTimestampAfterOrderByTimestampDesc(
        Long roomId, ChatMessage.MessageType messageType, LocalDateTime timestamp);

    /**
     * 특정 채팅방의 특정 날짜 이후 메시지 조회 (기간별 분석용)
     */
    List<ChatMessage> findByRoomIdAndTimestampAfterAndIsDeletedFalse(Long roomId, LocalDateTime timestamp);

    /**
     * 특정 채팅방의 모든 메시지 조회 (분석용)
     */
    List<ChatMessage> findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long roomId);

    /**
     * 특정 채팅방의 특정 메시지 타입, 특정 날짜 이후 메시지 조회 (목적 분석용)
     */
    List<ChatMessage> findByRoomIdAndMessageTypeAndTimestampAfterAndIsDeletedFalseOrderByTimestampDesc(
        Long roomId, ChatMessage.MessageType messageType, LocalDateTime timestamp);
}