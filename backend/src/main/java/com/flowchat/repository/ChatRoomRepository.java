package com.flowchat.repository;

import com.flowchat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * 활성 채팅방 목록 조회
     */
    List<ChatRoom> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * 공개 채팅방 목록 조회
     */
    List<ChatRoom> findByIsActiveTrueAndIsPublicTrueOrderByCreatedAtDesc();
    
    /**
     * 비공개 채팅방 목록 조회
     */
    List<ChatRoom> findByIsActiveTrueAndIsPublicFalseOrderByCreatedAtDesc();
    
    /**
     * 특정 사용자가 생성한 채팅방 조회
     */
    List<ChatRoom> findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(Long createdBy);
    
    /**
     * 채팅방 이름으로 검색
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true AND " +
           "LOWER(cr.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> searchByName(@Param("keyword") String keyword);
    
    /**
     * 참여 가능한 채팅방 조회 (가득 차지 않은 방)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true AND cr.isPublic = true AND " +
           "cr.currentParticipants < cr.maxParticipants ORDER BY cr.createdAt DESC")
    List<ChatRoom> findAvailableRooms();
    
    /**
     * 인기 채팅방 조회 (참여자 수 기준)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true AND cr.isPublic = true " +
           "ORDER BY cr.currentParticipants DESC, cr.createdAt DESC")
    List<ChatRoom> findPopularRooms();
    
    /**
     * 특정 기간 동안 생성된 채팅방 조회
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * 빈 채팅방 조회 (참여자가 없는 방)
     */
    List<ChatRoom> findByCurrentParticipantsAndIsActiveTrue(Integer currentParticipants);
    
    /**
     * 가득 찬 채팅방 조회
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isActive = true AND " +
           "cr.currentParticipants >= cr.maxParticipants")
    List<ChatRoom> findFullRooms();
    
    /**
     * 활성 채팅방 수 조회
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.isActive = true")
    long countActiveRooms();
    
    /**
     * 공개 채팅방 수 조회
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.isActive = true AND cr.isPublic = true")
    long countPublicRooms();
    
    /**
     * 오늘 생성된 채팅방 수 조회
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE FUNCTION('DATE', cr.createdAt) = CURRENT_DATE")
    long countTodayCreatedRooms();
    
    /**
     * 전체 참여자 수 조회
     */
    @Query("SELECT SUM(cr.currentParticipants) FROM ChatRoom cr WHERE cr.isActive = true")
    Long sumTotalParticipants();
    
    /**
     * 평균 참여자 수 조회
     */
    @Query("SELECT AVG(cr.currentParticipants) FROM ChatRoom cr WHERE cr.isActive = true")
    Double getAverageParticipants();
    
    /**
     * 특정 사용자가 생성한 채팅방 수 조회
     */
    long countByCreatedByAndIsActiveTrue(Long createdBy);
}