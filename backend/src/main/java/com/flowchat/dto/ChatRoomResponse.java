package com.flowchat.dto;

import com.flowchat.entity.ChatRoom;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class ChatRoomResponse {
    
    private Long id;
    private String name;
    private String description;
    private Integer maxParticipants;
    private Integer currentParticipants;
    @JsonIgnore
    private Long createdBy;
    private String createdByName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private Boolean isActive;
    private Boolean isPublic;
    private Boolean canJoin;
    private Boolean isFull;
    
    // 기본 생성자
    public ChatRoomResponse() {}
    
    // Entity로부터 변환하는 생성자
    public ChatRoomResponse(ChatRoom chatRoom) {
        this.id = chatRoom.getId();
        this.name = chatRoom.getName();
        this.description = chatRoom.getDescription();
        this.maxParticipants = chatRoom.getMaxParticipants();
        this.currentParticipants = chatRoom.getCurrentParticipants();
        this.createdBy = chatRoom.getCreatedBy();
        this.createdAt = chatRoom.getCreatedAt();
        this.isActive = chatRoom.getIsActive();
        this.isPublic = chatRoom.getIsPublic();
        this.canJoin = chatRoom.canJoin();
        this.isFull = chatRoom.isFull();
    }
    
    // Entity로부터 변환하는 정적 메서드
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(chatRoom);
    }
    
    // Entity로부터 변환하며 생성자 이름도 포함하는 정적 메서드
    public static ChatRoomResponse from(ChatRoom chatRoom, String createdByName) {
        ChatRoomResponse response = new ChatRoomResponse(chatRoom);
        response.setCreatedByName(createdByName);
        return response;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
    
    public Boolean getCanJoin() {
        return canJoin;
    }
    
    public void setCanJoin(Boolean canJoin) {
        this.canJoin = canJoin;
    }
    
    public Boolean getIsFull() {
        return isFull;
    }
    
    public void setIsFull(Boolean isFull) {
        this.isFull = isFull;
    }
    
    @Override
    public String toString() {
        return "ChatRoomResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", currentParticipants=" + currentParticipants +
                ", isActive=" + isActive +
                ", isPublic=" + isPublic +
                ", canJoin=" + canJoin +
                ", createdAt=" + createdAt +
                '}';
    }
}