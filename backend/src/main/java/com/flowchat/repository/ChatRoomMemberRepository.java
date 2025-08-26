package com.flowchat.repository;

import com.flowchat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    
    /**
     * 특정 채팅방의 활성 멤버 조회
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.isActive = true")
    List<ChatRoomMember> findActiveByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 채팅방의 온라인 멤버 조회
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.isActive = true AND m.isOnline = true")
    List<ChatRoomMember> findOnlineByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 사용자의 활성 채팅방 멤버십 조회
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.userId = :userId AND m.isActive = true")
    List<ChatRoomMember> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자가 특정 채팅방의 멤버인지 확인 (활성/비활성 모두 포함)
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId")
    Optional<ChatRoomMember> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * 사용자가 특정 채팅방의 활성 멤버인지 확인
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.userId = :userId AND m.isActive = true")
    Optional<ChatRoomMember> findActiveByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * 특정 채팅방의 활성 멤버 수 조회
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.isActive = true")
    long countActiveByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 특정 채팅방의 온라인 멤버 수 조회
     */
    @Query("SELECT COUNT(m) FROM ChatRoomMember m WHERE m.roomId = :roomId AND m.isActive = true AND m.isOnline = true")
    long countOnlineByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 사용자의 온라인 상태 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember m SET m.isOnline = :isOnline, m.lastSeen = :lastSeen WHERE m.userId = :userId AND m.isActive = true")
    int updateUserOnlineStatus(@Param("userId") Long userId, @Param("isOnline") Boolean isOnline, @Param("lastSeen") LocalDateTime lastSeen);
    
    /**
     * 특정 채팅방에서 사용자의 온라인 상태 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember m SET m.isOnline = :isOnline, m.lastSeen = :lastSeen WHERE m.roomId = :roomId AND m.userId = :userId AND m.isActive = true")
    int updateUserOnlineStatusInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("isOnline") Boolean isOnline, @Param("lastSeen") LocalDateTime lastSeen);
    
    /**
     * 모든 사용자를 오프라인으로 설정 (서버 재시작 시 사용)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatRoomMember m SET m.isOnline = false, m.lastSeen = :lastSeen")
    int setAllUsersOffline(@Param("lastSeen") LocalDateTime lastSeen);
    
    /**
     * 특정 채팅방의 상세 멤버 정보 조회 (사용자 이름 포함)
     */
    @Query("SELECT m, u.name, u.username FROM ChatRoomMember m JOIN User u ON m.userId = u.id " +
           "WHERE m.roomId = :roomId AND m.isActive = true ORDER BY m.isOnline DESC, m.joinedAt ASC")
    List<Object[]> findMemberDetailsByRoomId(@Param("roomId") Long roomId);
    
    /**
     * 비활성화된 멤버십 정리
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatRoomMember m WHERE m.isActive = false AND m.lastSeen < :cutoffDate")
    int cleanupInactiveMembers(@Param("cutoffDate") LocalDateTime cutoffDate);
}