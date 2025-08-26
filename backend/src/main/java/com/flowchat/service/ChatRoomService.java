package com.flowchat.service;

import com.flowchat.dto.ChatRoomRequest;
import com.flowchat.dto.ChatRoomResponse;
import com.flowchat.entity.ChatRoom;
import com.flowchat.entity.User;
import com.flowchat.repository.ChatRoomRepository;
import com.flowchat.repository.UserRepository;
import com.flowchat.repository.ChatRoomMemberRepository;
import com.flowchat.entity.ChatRoomMember;
import com.flowchat.service.ChatRoomMemberService;
import com.flowchat.service.ChatRoomMemberService.ChatRoomMemberInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
@Transactional
public class ChatRoomService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);
    
    /**
     * 실제 참여자 수를 반영한 ChatRoomResponse 생성
     */
    private ChatRoomResponse createResponseWithRealParticipants(ChatRoom room, String createdByName) {
        long actualParticipants = chatRoomMemberService.getRoomMemberCount(room.getId());
        ChatRoomResponse response = ChatRoomResponse.from(room, createdByName);
        response.setCurrentParticipants((int) actualParticipants);
        return response;
    }
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatRoomMemberService chatRoomMemberService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    
    /**
     * 애플리케이션 시작 시 채팅방 참여자 수 및 온라인 상태 초기화
     */
    @PostConstruct
    public void initializeParticipantCounts() {
        try {
            // 모든 사용자를 오프라인으로 설정
            chatRoomMemberService.setAllUsersOffline();
            
            // 모든 채팅방의 참여자 수를 실제 멤버 수로 업데이트
            List<ChatRoom> allRooms = chatRoomRepository.findAll();
            for (ChatRoom room : allRooms) {
                long memberCount = chatRoomMemberService.getRoomMemberCount(room.getId());
                room.setCurrentParticipants((int) memberCount);
                chatRoomRepository.save(room);
            }
            logger.info("채팅방 참여자 수 초기화 완료: {} 개의 채팅방", allRooms.size());
        } catch (Exception e) {
            logger.error("채팅방 참여자 수 초기화 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 채팅방 생성
     */
    public ChatRoomResponse createChatRoom(ChatRoomRequest request, Long userId) {
        logger.info("새 채팅방 생성 요청: name={}, userId={}", request.getName(), userId);
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom(
            request.getName(),
            request.getDescription(),
            request.getMaxParticipants(),
            userId
        );
        
        chatRoom.setIsPublic(request.getIsPublic());
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        logger.info("채팅방 생성 완료: roomId={}, name={}", savedRoom.getId(), savedRoom.getName());
        
        return ChatRoomResponse.from(savedRoom, user.getName());
    }
    
    /**
     * 모든 공개 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getPublicChatRooms() {
        logger.debug("공개 채팅방 목록 조회");
        
        List<ChatRoom> chatRooms = chatRoomRepository.findByIsActiveTrueAndIsPublicTrueOrderByCreatedAtDesc();
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return createResponseWithRealParticipants(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 모든 비공개 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getPrivateChatRooms() {
        logger.debug("비공개 채팅방 목록 조회");
        
        List<ChatRoom> chatRooms = chatRoomRepository.findByIsActiveTrueAndIsPublicFalseOrderByCreatedAtDesc();
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return ChatRoomResponse.from(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 모든 활성 채팅방 조회 (공개 + 비공개)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getAllChatRooms() {
        logger.debug("전체 채팅방 목록 조회");
        
        List<ChatRoom> chatRooms = chatRoomRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return createResponseWithRealParticipants(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 채팅방 상세 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoom(Long roomId) {
        logger.debug("채팅방 상세 조회: roomId={}", roomId);
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        String createdByName = userRepository.findById(chatRoom.getCreatedBy())
            .map(User::getName)
            .orElse("알 수 없음");
        
        return ChatRoomResponse.from(chatRoom, createdByName);
    }
    
    /**
     * 사용자가 참여한 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        logger.debug("사용자 참여 채팅방 목록 조회: userId={}", userId);
        
        // 사용자가 활성 멤버인 채팅방 멤버십 조회
        List<ChatRoomMember> membershipList = chatRoomMemberRepository.findActiveByUserId(userId);
        
        if (membershipList.isEmpty()) {
            logger.debug("사용자가 참여 중인 채팅방이 없음: userId={}", userId);
            return List.of();
        }
        
        // 채팅방 ID 목록 추출
        List<Long> roomIds = membershipList.stream()
                .map(ChatRoomMember::getRoomId)
                .collect(Collectors.toList());
        
        // 채팅방 정보 조회 및 응답 변환
        List<ChatRoom> rooms = chatRoomRepository.findAllById(roomIds);
        
        List<ChatRoomResponse> result = rooms.stream()
                .filter(ChatRoom::getIsActive) // 활성 채팅방만 필터링
                .map(room -> {
                    String createdByName = userRepository.findById(room.getCreatedBy())
                        .map(User::getName)
                        .orElse("알 수 없음");
                    return ChatRoomResponse.from(room, createdByName);
                })
                .collect(Collectors.toList());
        
        logger.debug("사용자 참여 채팅방 조회 완료: userId={}, count={}", userId, result.size());
        return result;
    }
    
    /**
     * 채팅방 입장 (실제 멤버십 생성)
     */
    public ChatRoomResponse joinChatRoom(Long roomId, Long userId) {
        logger.info("채팅방 참여 요청: roomId={}, userId={}", roomId, userId);
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        // 채팅방 활성 상태 확인
        if (!chatRoom.getIsActive()) {
            throw new IllegalStateException("비활성화된 채팅방입니다");
        }
        
        // 이미 멤버인지 확인
        boolean isMember = chatRoomMemberService.isMemberOfRoom(roomId, userId);
        if (isMember) {
            throw new IllegalStateException("이미 참여 중인 채팅방입니다");
        }
        
        // 채팅방이 가득 찬지 확인 (실제 멤버 수 기준)
        long currentMemberCount = chatRoomMemberService.getRoomMemberCount(roomId);
        if (currentMemberCount >= chatRoom.getMaxParticipants()) {
            throw new IllegalStateException("채팅방이 가득 찼습니다");
        }
        
        // 채팅방 멤버십 추가
        chatRoomMemberService.addMemberToRoom(roomId, userId);
        
        // 현재 참여자 수 업데이트 (DB의 실제 멤버 수로)
        long updatedMemberCount = chatRoomMemberService.getRoomMemberCount(roomId);
        chatRoom.setCurrentParticipants((int) updatedMemberCount);
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        logger.info("채팅방 참여 완료: roomId={}, userId={}, totalMembers={}", 
                   roomId, userId, updatedMemberCount);
        
        return ChatRoomResponse.from(savedRoom, user.getName());
    }
    
    /**
     * 채팅방 퇴장 (멤버십 제거)
     */
    public void leaveChatRoom(Long roomId, Long userId) {
        logger.info("채팅방 퇴장 요청: roomId={}, userId={}", roomId, userId);
        
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        // 멤버인지 확인
        boolean isMember = chatRoomMemberService.isMemberOfRoom(roomId, userId);
        if (!isMember) {
            throw new IllegalStateException("참여하지 않은 채팅방입니다");
        }
        
        // 멤버십에서 제거
        chatRoomMemberService.removeMemberFromRoom(roomId, userId);
        
        // 현재 참여자 수 업데이트 (DB의 실제 멤버 수로)
        long memberCount = chatRoomMemberService.getRoomMemberCount(roomId);
        chatRoom.setCurrentParticipants((int) memberCount);
        chatRoomRepository.save(chatRoom);
        
        logger.info("채팅방 퇴장 완료: roomId={}, userId={}, totalMembers={}", 
                   roomId, userId, memberCount);
    }
    
    /**
     * 채팅방 검색
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> searchChatRooms(String keyword) {
        logger.debug("채팅방 검색: keyword={}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllChatRooms();
        }
        
        List<ChatRoom> chatRooms = chatRoomRepository.searchByName(keyword.trim());
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return ChatRoomResponse.from(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 참여 가능한 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getAvailableChatRooms() {
        logger.debug("참여 가능한 채팅방 목록 조회");
        
        List<ChatRoom> chatRooms = chatRoomRepository.findAvailableRooms();
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return ChatRoomResponse.from(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 인기 채팅방 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getPopularChatRooms() {
        logger.debug("인기 채팅방 목록 조회");
        
        List<ChatRoom> chatRooms = chatRoomRepository.findPopularRooms();
        
        return chatRooms.stream()
            .map(room -> {
                String createdByName = userRepository.findById(room.getCreatedBy())
                    .map(User::getName)
                    .orElse("알 수 없음");
                return ChatRoomResponse.from(room, createdByName);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 채팅방 비활성화 (삭제)
     */
    public void deleteChatRoom(Long roomId, Long userId) {
        logger.info("채팅방 삭제 요청: roomId={}, userId={}", roomId, userId);
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        // 생성자 권한 확인
        if (!chatRoom.getCreatedBy().equals(userId)) {
            throw new IllegalStateException("채팅방을 삭제할 권한이 없습니다");
        }
        
        // 채팅방 비활성화
        chatRoom.deactivate();
        chatRoomRepository.save(chatRoom);
        
        logger.info("채팅방 삭제 완료: roomId={}", roomId);
    }
    
    /**
     * 채팅방 설정 업데이트
     */
    public ChatRoomResponse updateChatRoom(Long roomId, ChatRoomRequest request, Long userId) {
        logger.info("채팅방 업데이트 요청: roomId={}, userId={}", roomId, userId);
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        // 생성자 권한 확인
        if (!chatRoom.getCreatedBy().equals(userId)) {
            throw new IllegalStateException("채팅방을 수정할 권한이 없습니다");
        }
        
        // 정보 업데이트
        chatRoom.setName(request.getName());
        chatRoom.setDescription(request.getDescription());
        chatRoom.setMaxParticipants(request.getMaxParticipants());
        chatRoom.setIsPublic(request.getIsPublic());
        
        ChatRoom updatedRoom = chatRoomRepository.save(chatRoom);
        
        String createdByName = userRepository.findById(userId)
            .map(User::getName)
            .orElse("알 수 없음");
        
        logger.info("채팅방 업데이트 완료: roomId={}", roomId);
        
        return ChatRoomResponse.from(updatedRoom, createdByName);
    }
    
    /**
     * 채팅방의 참여 멤버 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomMemberInfo> getRoomMembers(Long roomId) {
        logger.debug("채팅방 멤버 목록 조회: roomId={}", roomId);
        
        // 채팅방 존재 확인
        chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        return chatRoomMemberService.getRoomMembers(roomId);
    }
    
    /**
     * 채팅방 통계 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomStats getChatRoomStats() {
        logger.debug("채팅방 통계 조회");
        
        long totalRooms = chatRoomRepository.countActiveRooms();
        long publicRooms = chatRoomRepository.countPublicRooms();
        long todayRooms = chatRoomRepository.countTodayCreatedRooms();
        Long totalParticipants = chatRoomRepository.sumTotalParticipants();
        Double avgParticipants = chatRoomRepository.getAverageParticipants();
        
        return new ChatRoomStats(
            totalRooms,
            publicRooms,
            todayRooms,
            totalParticipants != null ? totalParticipants : 0L,
            avgParticipants != null ? avgParticipants : 0.0
        );
    }
    
    /**
     * 사용자가 참여 중인 채팅방 목록 조회 (분석용)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserJoinedRooms(Long userId) {
        logger.debug("사용자 참여 채팅방 조회: userId={}", userId);
        
        // 사용자가 활성 멤버인 채팅방 멤버십 조회
        List<ChatRoomMember> membershipList = chatRoomMemberRepository.findActiveByUserId(userId);
        
        if (membershipList.isEmpty()) {
            logger.debug("사용자가 참여 중인 채팅방이 없음: userId={}", userId);
            return List.of();
        }
        
        // 채팅방 ID 목록 추출
        List<Long> roomIds = membershipList.stream()
                .map(ChatRoomMember::getRoomId)
                .collect(Collectors.toList());
        
        // 채팅방 정보 조회 및 응답 변환
        List<ChatRoom> rooms = chatRoomRepository.findAllById(roomIds);
        
        List<ChatRoomResponse> result = rooms.stream()
                .filter(ChatRoom::getIsActive) // 활성 채팅방만 필터링
                .map(room -> {
                    String createdByName = userRepository.findById(room.getCreatedBy())
                        .map(User::getName)
                        .orElse("알 수 없음");
                    return ChatRoomResponse.from(room, createdByName);
                })
                .collect(Collectors.toList());
        
        logger.debug("사용자 참여 채팅방 조회 완료: userId={}, count={}", userId, result.size());
        return result;
    }
    
    /**
     * 채팅방 통계 정보 클래스
     */
    public static class ChatRoomStats {
        private final long totalRooms;
        private final long publicRooms;
        private final long todayRooms;
        private final long totalParticipants;
        private final double averageParticipants;
        
        public ChatRoomStats(long totalRooms, long publicRooms, long todayRooms, 
                           long totalParticipants, double averageParticipants) {
            this.totalRooms = totalRooms;
            this.publicRooms = publicRooms;
            this.todayRooms = todayRooms;
            this.totalParticipants = totalParticipants;
            this.averageParticipants = averageParticipants;
        }
        
        // Getters
        public long getTotalRooms() { return totalRooms; }
        public long getPublicRooms() { return publicRooms; }
        public long getTodayRooms() { return todayRooms; }
        public long getTotalParticipants() { return totalParticipants; }
        public double getAverageParticipants() { return averageParticipants; }
    }
}