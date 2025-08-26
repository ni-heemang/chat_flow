package com.flowchat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_created_by", columnList = "created_by"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 1, max = 200, message = "채팅방 이름은 1-200자 사이여야 합니다")
    private String name;
    
    @Column(length = 500)
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @Column(name = "max_participants", nullable = false)
    @Min(value = 2, message = "최대 참여자 수는 2명 이상이어야 합니다")
    @Max(value = 100, message = "최대 참여자 수는 100명을 초과할 수 없습니다")
    private Integer maxParticipants = 50;
    
    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 0;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;
    
    // 기본 생성자
    protected ChatRoom() {}
    
    // 생성자
    public ChatRoom(String name, String description, Integer maxParticipants, Long createdBy) {
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants != null ? maxParticipants : 50;
        this.createdBy = createdBy;
        this.currentParticipants = 0;
        this.isActive = true;
        this.isPublic = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public Integer getCurrentParticipants() {
        return currentParticipants;
    }
    
    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    // 비즈니스 메서드
    public boolean canJoin() {
        return isActive && currentParticipants < maxParticipants;
    }
    
    public void incrementParticipants() {
        if (canJoin()) {
            this.currentParticipants++;
        } else {
            throw new IllegalStateException("채팅방이 가득 차거나 비활성 상태입니다");
        }
    }
    
    public void decrementParticipants() {
        if (currentParticipants > 0) {
            this.currentParticipants--;
        }
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public void activate() {
        this.isActive = true;
    }
    
    public void makePrivate() {
        this.isPublic = false;
    }
    
    public void makePublic() {
        this.isPublic = true;
    }
    
    public boolean isFull() {
        return currentParticipants >= maxParticipants;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return Objects.equals(id, chatRoom.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ChatRoom{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", currentParticipants=" + currentParticipants +
                ", isActive=" + isActive +
                ", isPublic=" + isPublic +
                ", createdAt=" + createdAt +
                '}';
    }
}