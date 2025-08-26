package com.flowchat.controller;

import com.flowchat.config.JwtConfig;
import com.flowchat.dto.*;
import com.flowchat.entity.User;
import com.flowchat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "사용자 관리 API")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:8080"}, 
            allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 사용자 등록
     */
    @PostMapping("/register")
    @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRequest request) {
        try {
            User user = userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getName()
            );
            
            UserResponse response = UserResponse.from(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 및 JWT 토큰 발급")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        try {
            UserService.LoginResult result = userService.authenticateUser(
                request.getUsername(),
                request.getPassword()
            );
            
            UserResponse userResponse = UserResponse.from(result.getUser());
            Long expiresIn = jwtConfig.getTimeUntilExpiration(result.getAccessToken());
            
            LoginResponse response = new LoginResponse(
                result.getAccessToken(),
                result.getRefreshToken(),
                expiresIn,
                userResponse
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰 발급")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String newAccessToken = userService.refreshAccessToken(request.getRefreshToken());
            Long expiresIn = jwtConfig.getTimeUntilExpiration(newAccessToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", expiresIn);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/profile")
    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필 정보 조회")
    public ResponseEntity<UserResponse> getCurrentUser() {
        try {
            String username = getCurrentUsername();
            User user = userService.getUserByUsername(username);
            UserResponse response = UserResponse.from(user);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 프로필 업데이트
     */
    @PutMapping("/profile")
    @Operation(summary = "프로필 업데이트", description = "사용자 프로필 정보 업데이트")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody Map<String, String> request) {
        try {
            String username = getCurrentUsername();
            User currentUser = userService.getUserByUsername(username);
            
            String name = request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            User updatedUser = userService.updateUserProfile(currentUser.getId(), name.trim());
            UserResponse response = UserResponse.from(updatedUser);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/password")
    @Operation(summary = "비밀번호 변경", description = "사용자 비밀번호 변경")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            String username = getCurrentUsername();
            User currentUser = userService.getUserByUsername(username);
            
            userService.changePassword(
                currentUser.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "비밀번호가 성공적으로 변경되었습니다");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 계정 비활성화
     */
    @DeleteMapping("/account")
    @Operation(summary = "계정 비활성화", description = "사용자 계정을 비활성화합니다")
    public ResponseEntity<Map<String, String>> deactivateAccount() {
        try {
            String username = getCurrentUsername();
            User currentUser = userService.getUserByUsername(username);
            
            userService.deactivateUser(currentUser.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "계정이 비활성화되었습니다");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 사용자명 중복 확인
     */
    @GetMapping("/check-username")
    @Operation(summary = "사용자명 중복 확인", description = "사용자명 사용 가능 여부 확인")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        try {
            boolean available = !userService.getUserByUsername(username).getId().equals(0L);
            Map<String, Boolean> response = new HashMap<>();
            response.put("available", false); // 사용자가 존재하면 사용 불가
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("available", true); // 사용자가 없으면 사용 가능
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 현재 인증된 사용자의 사용자명 가져오기
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }

    /**
     * 전역 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", "서버 내부 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}