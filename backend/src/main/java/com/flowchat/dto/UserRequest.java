package com.flowchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequest {
    
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 3, max = 50, message = "아이디는 3-50자 사이여야 합니다")
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 100, message = "비밀번호는 6-100자 사이여야 합니다")
    private String password;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 100, message = "이름은 2-100자 사이여야 합니다")
    private String name;
    
    // 기본 생성자
    public UserRequest() {}
    
    // 생성자
    public UserRequest(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "UserRequest{" +
                "username='" + username + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}