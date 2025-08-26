package com.flowchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatMessageRequest {
    
    @NotNull(message = "채팅방 ID는 필수입니다")
    private Long roomId;
    
    @NotBlank(message = "메시지 내용은 필수입니다")
    private String content;
    
    private String messageType = "TEXT";
    
    // 기본 생성자
    public ChatMessageRequest() {}
    
    // 생성자
    public ChatMessageRequest(Long roomId, String content, String messageType) {
        this.roomId = roomId;
        this.content = content;
        this.messageType = messageType;
    }
    
    // Getters and Setters
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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
    
    @Override
    public String toString() {
        return "ChatMessageRequest{" +
                "roomId=" + roomId +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}