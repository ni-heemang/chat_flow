package com.flowchat.dto;

import com.flowchat.entity.User;

import java.time.LocalDateTime;

public class UserResponse {
    
    private Long id;
    private String username;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Boolean isActive;
    
    // 기본 생성자
    public UserResponse() {}
    
    // 생성자
    public UserResponse(Long id, String username, String name, 
                       LocalDateTime createdAt, LocalDateTime lastLoginAt, Boolean isActive) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.isActive = isActive;
    }
    
    // User 엔티티로부터 생성하는 정적 팩토리 메서드
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            user.getIsActive()
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "UserResponse{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}