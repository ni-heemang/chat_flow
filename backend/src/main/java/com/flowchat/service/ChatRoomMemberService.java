package com.flowchat.service;

import com.flowchat.entity.ChatRoomMember;
import com.flowchat.entity.User;
import com.flowchat.repository.ChatRoomMemberRepository;
import com.flowchat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatRoomMemberService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomMemberService.class);
    
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 채팅방에 사용자 추가 (멤버십 생성)
     */
    public ChatRoomMember addMemberToRoom(Long roomId, Long userId) {
        logger.info("채팅방 멤버십 추가: roomId={}, userId={}", roomId, userId);
        
        try {
            // 이미 멤버인지 확인 (활성/비활성 모두 포함)
            Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId);
            if (existingMember.isPresent()) {
                ChatRoomMember member = existingMember.get();
                if (!member.getIsActive()) {
                    // 비활성 멤버를 다시 활성화
                    member.activate();
                    member = chatRoomMemberRepository.save(member);
                    logger.info("비활성 멤버 재활성화: roomId={}, userId={}", roomId, userId);
                    return member;
                } else {
                    logger.debug("이미 활성 멤버임: roomId={}, userId={}", roomId, userId);
                    return member; // 이미 활성 멤버인 경우 그대로 반환
                }
            }
            
            // 새 멤버십 생성
            ChatRoomMember newMember = new ChatRoomMember(roomId, userId);
            ChatRoomMember savedMember = chatRoomMemberRepository.save(newMember);
            
            logger.info("새 채팅방 멤버십 생성: id={}, roomId={}, userId={}", savedMember.getId(), roomId, userId);
            return savedMember;
            
        } catch (Exception e) {
            logger.error("채팅방 멤버십 추가 중 오류: roomId={}, userId={}, error={}", roomId, userId, e.getMessage());
            
            // 오류 발생 시 다시 한 번 확인해서 기존 멤버 반환
            Optional<ChatRoomMember> fallbackMember = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId);
            if (fallbackMember.isPresent()) {
                ChatRoomMember member = fallbackMember.get();
                if (!member.getIsActive()) {
                    member.activate();
                    member = chatRoomMemberRepository.save(member);
                    logger.info("폴백: 비활성 멤버 재활성화: roomId={}, userId={}", roomId, userId);
                } else {
                    logger.info("폴백: 이미 활성 멤버 반환: roomId={}, userId={}", roomId, userId);
                }
                return member;
            }
            
            // 예외를 다시 발생
            throw new RuntimeException("채팅방 멤버십 추가에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 채팅방에서 사용자 제거 (멤버십 비활성화)
     */
    public void removeMemberFromRoom(Long roomId, Long userId) {
        logger.info("채팅방 멤버십 제거: roomId={}, userId={}", roomId, userId);
        
        Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (memberOpt.isPresent()) {
            ChatRoomMember member = memberOpt.get();
            member.deactivate();
            chatRoomMemberRepository.save(member);
            logger.info("채팅방 멤버십 비활성화 완료: roomId={}, userId={}", roomId, userId);
        } else {
            logger.warn("제거할 멤버십을 찾을 수 없음: roomId={}, userId={}", roomId, userId);
        }
    }
    
    /**
     * 사용자의 온라인 상태 업데이트
     */
    public void updateUserOnlineStatus(Long userId, boolean isOnline) {
        logger.debug("사용자 온라인 상태 업데이트: userId={}, isOnline={}", userId, isOnline);
        
        int updated = chatRoomMemberRepository.updateUserOnlineStatus(userId, isOnline, LocalDateTime.now());
        logger.debug("업데이트된 멤버십 수: {}", updated);
    }
    
    /**
     * 특정 채팅방에서 사용자의 온라인 상태 업데이트
     */
    public void updateUserOnlineStatusInRoom(Long roomId, Long userId, boolean isOnline) {
        logger.debug("채팅방별 사용자 온라인 상태 업데이트: roomId={}, userId={}, isOnline={}", roomId, userId, isOnline);
        
        int updated = chatRoomMemberRepository.updateUserOnlineStatusInRoom(roomId, userId, isOnline, LocalDateTime.now());
        logger.debug("업데이트된 멤버십 수: {}", updated);
    }
    
    /**
     * 채팅방의 모든 활성 멤버 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomMemberInfo> getRoomMembers(Long roomId) {
        logger.debug("채팅방 멤버 목록 조회: roomId={}", roomId);
        
        List<Object[]> results = chatRoomMemberRepository.findMemberDetailsByRoomId(roomId);
        
        return results.stream().map(result -> {
            ChatRoomMember member = (ChatRoomMember) result[0];
            String name = (String) result[1];
            String username = (String) result[2];
            
            return new ChatRoomMemberInfo(
                member.getUserId(),
                username,
                name,
                member.getIsOnline(),
                member.getJoinedAt(),
                member.getLastSeen()
            );
        }).collect(Collectors.toList());
    }
    
    /**
     * 채팅방의 활성 멤버 수 조회
     */
    @Transactional(readOnly = true)
    public long getRoomMemberCount(Long roomId) {
        return chatRoomMemberRepository.countActiveByRoomId(roomId);
    }
    
    /**
     * 채팅방의 온라인 멤버 수 조회
     */
    @Transactional(readOnly = true)
    public long getRoomOnlineCount(Long roomId) {
        return chatRoomMemberRepository.countOnlineByRoomId(roomId);
    }
    
    /**
     * 사용자가 특정 채팅방의 활성 멤버인지 확인
     */
    @Transactional(readOnly = true)
    public boolean isMemberOfRoom(Long roomId, Long userId) {
        return chatRoomMemberRepository.findActiveByRoomIdAndUserId(roomId, userId).isPresent();
    }
    
    /**
     * 모든 사용자를 오프라인으로 설정 (서버 재시작 시)
     */
    public void setAllUsersOffline() {
        logger.info("모든 사용자를 오프라인으로 설정");
        
        int updated = chatRoomMemberRepository.setAllUsersOffline(LocalDateTime.now());
        logger.info("오프라인으로 설정된 멤버십 수: {}", updated);
    }
    
    /**
     * 모든 채팅방 멤버십 데이터 초기화 (개발용)
     */
    public void resetAllMembers() {
        logger.warn("모든 채팅방 멤버십 데이터 삭제 시작");
        
        chatRoomMemberRepository.deleteAll();
        
        logger.warn("모든 채팅방 멤버십 데이터 삭제 완료");
    }
    
    /**
     * 채팅방 멤버 정보를 담는 내부 클래스
     */
    public static class ChatRoomMemberInfo {
        private final Long userId;
        private final String username;
        private final String name;
        private final Boolean isOnline;
        private final LocalDateTime joinedAt;
        private final LocalDateTime lastSeen;
        
        public ChatRoomMemberInfo(Long userId, String username, String name, Boolean isOnline, 
                                 LocalDateTime joinedAt, LocalDateTime lastSeen) {
            this.userId = userId;
            this.username = username;
            this.name = name;
            this.isOnline = isOnline;
            this.joinedAt = joinedAt;
            this.lastSeen = lastSeen;
        }
        
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getName() { return name; }
        public Boolean getIsOnline() { return isOnline; }
        public LocalDateTime getJoinedAt() { return joinedAt; }
        public LocalDateTime getLastSeen() { return lastSeen; }
    }
}