package com.flowchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {
    
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;
    
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 6, max = 100, message = "새 비밀번호는 6-100자 사이여야 합니다")
    private String newPassword;
    
    // 기본 생성자
    public PasswordChangeRequest() {}
    
    // 생성자
    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
    
    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }
    
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    @Override
    public String toString() {
        return "PasswordChangeRequest{" +
                "currentPassword='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                '}';
    }
}