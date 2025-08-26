package com.flowchat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRoomRequest {
    
    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 1, max = 200, message = "채팅방 이름은 1-200자 사이여야 합니다")
    private String name;
    
    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @Min(value = 2, message = "최대 참여자 수는 2명 이상이어야 합니다")
    @Max(value = 100, message = "최대 참여자 수는 100명을 초과할 수 없습니다")
    private Integer maxParticipants = 50;
    
    private Boolean isPublic = true;
    
    // 기본 생성자
    public ChatRoomRequest() {}
    
    // 생성자
    public ChatRoomRequest(String name, String description, Integer maxParticipants, Boolean isPublic) {
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants != null ? maxParticipants : 50;
        this.isPublic = isPublic != null ? isPublic : true;
    }
    
    // Getters and Setters
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
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    @Override
    public String toString() {
        return "ChatRoomRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", isPublic=" + isPublic +
                '}';
    }
}