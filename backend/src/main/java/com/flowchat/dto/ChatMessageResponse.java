package com.flowchat.dto;

import com.flowchat.entity.ChatMessage;

import java.time.LocalDateTime;

public class ChatMessageResponse {
    
    private Long id;
    private Long roomId;
    private Long userId;
    private String username;
    private String name;
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
    private Boolean isDeleted;
    
    // 기본 생성자
    public ChatMessageResponse() {}
    
    // 생성자
    public ChatMessageResponse(Long id, Long roomId, Long userId, String username, String name,
                              String content, String messageType, LocalDateTime timestamp, Boolean isDeleted) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.isDeleted = isDeleted;
    }
    
    // ChatMessage 엔티티로부터 생성하는 정적 팩토리 메서드
    public static ChatMessageResponse from(ChatMessage message, String username, String name) {
        return new ChatMessageResponse(
            message.getId(),
            message.getRoomId(),
            message.getUserId(),
            username,
            name,
            message.getContent(),
            message.getMessageType().toString(),
            message.getTimestamp(),
            message.getIsDeleted()
        );
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getIsDeleted() {
        return isDeleted;
    }
    
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    @Override
    public String toString() {
        return "ChatMessageResponse{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}