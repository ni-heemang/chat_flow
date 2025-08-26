package com.flowchat.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {
    
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
    
    // 기본 생성자
    public RefreshTokenRequest() {}
    
    // 생성자
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    @Override
    public String toString() {
        return "RefreshTokenRequest{" +
                "refreshToken='[PROTECTED]'" +
                '}';
    }
}