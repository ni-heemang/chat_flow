package com.flowchat.repository;

import com.flowchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);
    
    /**
     * 이름으로 사용자 조회
     */
    Optional<User> findByName(String name);
    
    /**
     * 이름 존재 여부 확인
     */
    boolean existsByName(String name);
    
    /**
     * 활성 사용자 목록 조회
     */
    List<User> findByIsActiveTrue();
    
    /**
     * 비활성 사용자 목록 조회
     */
    List<User> findByIsActiveFalse();
    
    /**
     * 특정 기간 동안 생성된 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * 최근 로그인한 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
    
    /**
     * 사용자명이나 이름으로 검색 (LIKE 검색)
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchByUsernameOrName(@Param("keyword") String keyword);
    
    /**
     * 활성 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    
    /**
     * 오늘 가입한 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE FUNCTION('DATE', u.createdAt) = CURRENT_DATE")
    long countTodayRegisteredUsers();
    
    /**
     * 특정 시간 이후 로그인한 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since")
    long countUsersLoggedInSince(@Param("since") LocalDateTime since);
}