package com.flowchat;

import com.flowchat.entity.User;
import com.flowchat.entity.ChatRoom;
import com.flowchat.entity.ChatMessage;
import com.flowchat.entity.AnalysisResult;
import com.flowchat.repository.UserRepository;
import com.flowchat.repository.ChatRoomRepository;
import com.flowchat.repository.ChatMessageRepository;
import com.flowchat.repository.AnalysisResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@org.junit.jupiter.api.Disabled("Security configuration conflicts")
class EntityIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Test
    void testUserEntityAndRepository() {
        // Given
        User user = new User("testuser", "password123", "테스트사용자");
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getName()).isEqualTo("테스트사용자");
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void testChatRoomEntityAndRepository() {
        // Given
        User user = new User("roomowner", "password123", "방장");
        User savedUser = userRepository.save(user);
        
        ChatRoom chatRoom = new ChatRoom("테스트방", "테스트용 채팅방", 10, savedUser.getId());
        
        // When
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        // Then
        assertThat(savedRoom.getId()).isNotNull();
        assertThat(savedRoom.getName()).isEqualTo("테스트방");
        assertThat(savedRoom.getMaxParticipants()).isEqualTo(10);
        assertThat(savedRoom.getCurrentParticipants()).isEqualTo(0);
        assertThat(savedRoom.getCreatedBy()).isEqualTo(savedUser.getId());
        assertThat(savedRoom.getIsActive()).isTrue();
    }

    @Test
    void testChatMessageEntityAndRepository() {
        // Given
        User user = new User("messageuser", "password123", "메시지유저");
        User savedUser = userRepository.save(user);
        
        ChatRoom chatRoom = new ChatRoom("메시지방", "메시지 테스트방", 10, savedUser.getId());
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        ChatMessage message = new ChatMessage(savedRoom.getId(), savedUser.getId(), "안녕하세요!");
        
        // When
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Then
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getRoomId()).isEqualTo(savedRoom.getId());
        assertThat(savedMessage.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedMessage.getContent()).isEqualTo("안녕하세요!");
        assertThat(savedMessage.getMessageType()).isEqualTo(ChatMessage.MessageType.TEXT);
        assertThat(savedMessage.getIsDeleted()).isFalse();
    }

    @Test
    void testAnalysisResultEntityAndRepository() {
        // Given
        User user = new User("analysisuser", "password123", "분석유저");
        User savedUser = userRepository.save(user);
        
        ChatRoom chatRoom = new ChatRoom("분석방", "분석 테스트방", 10, savedUser.getId());
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        LocalDateTime now = LocalDateTime.now();
        AnalysisResult analysis = AnalysisResult.createKeywordAnalysis(
            savedRoom.getId(),
            "{\"keywords\": [\"안녕\", \"테스트\"]}",
            5,
            2,
            now.minusHours(1),
            now
        );
        
        // When
        AnalysisResult savedAnalysis = analysisResultRepository.save(analysis);
        
        // Then
        assertThat(savedAnalysis.getId()).isNotNull();
        assertThat(savedAnalysis.getRoomId()).isEqualTo(savedRoom.getId());
        assertThat(savedAnalysis.getAnalysisType()).isEqualTo(AnalysisResult.AnalysisType.KEYWORD_FREQUENCY);
        assertThat(savedAnalysis.getMessageCount()).isEqualTo(5);
        assertThat(savedAnalysis.getParticipantCount()).isEqualTo(2);
        assertThat(savedAnalysis.getAnalysisData()).contains("keywords");
    }

    @Test
    void testRepositoryQueries() {
        // Given
        User user = new User("queryuser", "password123", "쿼리유저");
        User savedUser = userRepository.save(user);
        
        // When & Then
        assertThat(userRepository.existsByUsername("queryuser")).isTrue();
        assertThat(userRepository.existsByUsername("notexists")).isFalse();
        
        var foundUser = userRepository.findByUsername("queryuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("쿼리유저");
        
        long activeCount = userRepository.countActiveUsers();
        assertThat(activeCount).isGreaterThanOrEqualTo(1);
    }
}