package com.flowchat.service;

import com.flowchat.config.JwtConfig;
import com.flowchat.entity.User;
import com.flowchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 사용자 등록
     */
    public User registerUser(String username, String password, String name) {
        // 사용자명 중복 검사
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }

        // 이름 중복 검사
        if (userRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 이름입니다: " + name);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 사용자 생성 및 저장
        User user = new User(username, encodedPassword, name);
        return userRepository.save(user);
    }

    /**
     * 로그인 및 토큰 생성
     */
    public LoginResult authenticateUser(String username, String password) {
        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + username));

        // 활성 사용자 확인
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다");
        }

        // 마지막 로그인 시간 업데이트
        user.updateLastLogin();
        userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtConfig.generateToken(user.getUsername(), user.getId());
        String refreshToken = jwtConfig.generateRefreshToken(user.getUsername(), user.getId());

        return new LoginResult(user, accessToken, refreshToken);
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰 생성
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtConfig.isValidToken(refreshToken) || !jwtConfig.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다");
        }

        String username = jwtConfig.getUsernameFromToken(refreshToken);
        Long userId = jwtConfig.getUserIdFromToken(refreshToken);

        // 사용자 존재 및 활성 상태 확인
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 사용자입니다");
        }

        // 새 액세스 토큰 생성
        return jwtConfig.generateToken(username, userId);
    }

    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
    }

    /**
     * 사용자명으로 사용자 조회
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + username));
    }

    /**
     * 사용자 프로필 업데이트
     */
    public User updateUserProfile(Long userId, String name) {
        User user = getUserById(userId);

        // 이름 중복 검사 (현재 사용자 제외)
        Optional<User> existingUser = userRepository.findByName(name);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new IllegalArgumentException("이미 존재하는 이름입니다: " + name);
        }

        user.setName(name);
        return userRepository.save(user);
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
        }

        // 새 비밀번호 암호화 및 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 사용자 비활성화
     */
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.deactivate();
        userRepository.save(user);
    }

    /**
     * 사용자 활성화
     */
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.activate();
        userRepository.save(user);
    }

    /**
     * Spring Security UserDetailsService 구현
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User is inactive: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new ArrayList<>()) // 추후 권한 시스템 구현 시 수정
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }

    /**
     * 로그인 결과를 담는 내부 클래스
     */
    public static class LoginResult {
        private final User user;
        private final String accessToken;
        private final String refreshToken;

        public LoginResult(User user, String accessToken, String refreshToken) {
            this.user = user;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public User getUser() {
            return user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}