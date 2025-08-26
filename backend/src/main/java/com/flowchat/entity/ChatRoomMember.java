package com.flowchat.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_room_members", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}),
    indexes = {
        @Index(name = "idx_room_id", columnList = "room_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_joined_at", columnList = "joined_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ChatRoomMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    // 기본 생성자
    protected ChatRoomMember() {}
    
    // 생성자
    public ChatRoomMember(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
        this.isActive = true;
        this.isOnline = false;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsOnline() {
        return isOnline;
    }
    
    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
        if (isOnline) {
            this.lastSeen = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    // 비즈니스 메서드
    public void goOnline() {
        this.isOnline = true;
        this.lastSeen = LocalDateTime.now();
    }
    
    public void goOffline() {
        this.isOnline = false;
        this.lastSeen = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.isActive = false;
        this.isOnline = false;
        this.lastSeen = LocalDateTime.now();
    }
    
    public void activate() {
        this.isActive = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomMember that = (ChatRoomMember) o;
        return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
    
    @Override
    public String toString() {
        return "ChatRoomMember{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", userId=" + userId +
                ", joinedAt=" + joinedAt +
                ", isActive=" + isActive +
                ", isOnline=" + isOnline +
                ", lastSeen=" + lastSeen +
                '}';
    }
}