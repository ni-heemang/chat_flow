package com.flowchat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_room_id", columnList = "room_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_room_timestamp", columnList = "room_id, timestamp")
})
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_id", nullable = false)
    @NotNull(message = "채팅방 ID는 필수입니다")
    private Long roomId;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
    
    @Column(length = 50)
    private String username;
    
    @Column(length = 100)
    private String name;
    
    @Column(nullable = false, length = 2000)
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(min = 1, max = 2000, message = "메시지는 1-2000자 사이여야 합니다")
    private String content;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    // 메시지 타입 열거형
    public enum MessageType {
        TEXT,           // 일반 텍스트 메시지
        IMAGE,          // 이미지 메시지
        FILE,           // 파일 메시지
        SYSTEM,         // 시스템 메시지 (입장/퇴장 등)
        ANNOUNCEMENT    // 공지사항
    }
    
    // 기본 생성자
    protected ChatMessage() {}
    
    // 생성자
    public ChatMessage(Long roomId, Long userId, String content) {
        this.roomId = roomId;
        this.userId = userId;
        this.content = content;
        this.messageType = MessageType.TEXT;
        this.isDeleted = false;
    }
    
    public ChatMessage(Long roomId, Long userId, String content, MessageType messageType) {
        this.roomId = roomId;
        this.userId = userId;
        this.content = content;
        this.messageType = messageType;
        this.isDeleted = false;
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public Boolean getIsDeleted() {
        return isDeleted;
    }
    
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public LocalDateTime getEditedAt() {
        return editedAt;
    }
    
    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
    
    // 비즈니스 메서드
    public void edit(String newContent) {
        if (isDeleted) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다");
        }
        this.content = newContent;
        this.editedAt = LocalDateTime.now();
    }
    
    public void delete() {
        this.isDeleted = true;
        this.content = "삭제된 메시지입니다";
    }
    
    public boolean isEdited() {
        return editedAt != null;
    }
    
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM || messageType == MessageType.ANNOUNCEMENT;
    }
    
    public boolean isUserMessage() {
        return messageType == MessageType.TEXT || messageType == MessageType.IMAGE || messageType == MessageType.FILE;
    }
    
    // 시스템 메시지 생성을 위한 정적 팩토리 메서드
    public static ChatMessage createSystemMessage(Long roomId, String content) {
        return new ChatMessage(roomId, null, content, MessageType.SYSTEM);
    }
    
    public static ChatMessage createAnnouncementMessage(Long roomId, Long userId, String content) {
        return new ChatMessage(roomId, userId, content, MessageType.ANNOUNCEMENT);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", messageType=" + messageType +
                ", timestamp=" + timestamp +
                ", isDeleted=" + isDeleted +
                '}';
    }
}