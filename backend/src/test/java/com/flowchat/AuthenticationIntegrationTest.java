package com.flowchat;

import com.flowchat.dto.LoginRequest;
import com.flowchat.dto.UserRequest;
import com.flowchat.entity.User;
import com.flowchat.repository.UserRepository;
import com.flowchat.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 정리
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistration() {
        // Given
        String username = "testuser";
        String password = "password123";
        String nickname = "테스트유저";

        // When
        User user = userService.registerUser(username, password, nickname);

        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getName()).isEqualTo(nickname);
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getPassword()).isNotEqualTo(password); // 암호화되어야 함
    }

    @Test
    void testUserAuthentication() {
        // Given
        String username = "loginuser";
        String password = "password123";
        String nickname = "로그인유저";

        // 사용자 등록
        userService.registerUser(username, password, nickname);

        // When
        UserService.LoginResult result = userService.authenticateUser(username, password);

        // Then
        assertThat(result.getUser().getUsername()).isEqualTo(username);
        assertThat(result.getAccessToken()).isNotNull();
        assertThat(result.getRefreshToken()).isNotNull();
        assertThat(result.getUser().getLastLoginAt()).isNotNull();
    }

    @Test
    void testInvalidLogin() {
        // Given
        String username = "testuser";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        String nickname = "테스트유저";

        userService.registerUser(username, correctPassword, nickname);

        // When & Then
        assertThatThrownBy(() -> userService.authenticateUser(username, wrongPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("잘못된 비밀번호");
    }

    @Test
    void testDuplicateUsername() {
        // Given
        String username = "duplicate";
        String password = "password123";
        String nickname1 = "첫번째";
        String nickname2 = "두번째";

        userService.registerUser(username, password, nickname1);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(username, password, nickname2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 사용자명");
    }

    @Test
    void testDuplicateName() {
        // Given
        String username1 = "user1";
        String username2 = "user2";
        String password = "password123";
        String name = "중복이름";

        userService.registerUser(username1, password, name);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(username2, password, name))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 이름");
    }

    @Test
    void testRefreshToken() {
        // Given
        String username = "refreshuser";
        String password = "password123";
        String nickname = "리프레시유저";

        userService.registerUser(username, password, nickname);
        UserService.LoginResult loginResult = userService.authenticateUser(username, password);

        // When
        String newAccessToken = userService.refreshAccessToken(loginResult.getRefreshToken());

        // Then
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken).isNotEmpty();
        // 리프레시 토큰이 정상적으로 작동하는지 확인
    }
}