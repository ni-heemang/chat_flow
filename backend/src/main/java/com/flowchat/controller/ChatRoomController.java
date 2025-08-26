package com.flowchat.controller;

import com.flowchat.dto.ChatRoomRequest;
import com.flowchat.dto.ChatRoomResponse;
import com.flowchat.service.ChatRoomService;
import com.flowchat.service.ChatRoomMemberService.ChatRoomMemberInfo;
import com.flowchat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatrooms")
@Tag(name = "Chat Room Management", description = "채팅방 관리 API")
public class ChatRoomController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomController.class);
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 채팅방 목록 조회
     */
    @GetMapping("")
    @Operation(summary = "채팅방 목록 조회", description = "활성 채팅방 목록을 조회합니다. 필터: all(전체), available(입장가능), popular(인기), public(공개만), private(비공개만)")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String search,
            @Parameter(description = "필터 타입 (all, available, popular, public, private)") @RequestParam(defaultValue = "all") String filter) {
        
        logger.info("채팅방 목록 조회 요청: search={}, filter={}", search, filter);
        
        List<ChatRoomResponse> chatRooms;
        
        if (search != null && !search.trim().isEmpty()) {
            chatRooms = chatRoomService.searchChatRooms(search);
        } else {
            switch (filter.toLowerCase()) {
                case "available":
                    chatRooms = chatRoomService.getAvailableChatRooms();
                    break;
                case "popular":
                    chatRooms = chatRoomService.getPopularChatRooms();
                    break;
                case "public":
                    chatRooms = chatRoomService.getPublicChatRooms();
                    break;
                case "private":
                    chatRooms = chatRoomService.getPrivateChatRooms();
                    break;
                default:
                    // "all" 또는 기본값: 모든 채팅방 조회 (공개 + 비공개)
                    chatRooms = chatRoomService.getAllChatRooms();
                    break;
            }
        }
        
        logger.info("채팅방 목록 조회 완료: count={}", chatRooms.size());
        return ResponseEntity.ok(chatRooms);
    }
    
    /**
     * 내가 참여한 채팅방 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 채팅방 목록 조회", description = "로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(Authentication authentication) {
        logger.info("내 참여 채팅방 목록 조회 요청: user={}", authentication.getName());
        
        Long userId = getCurrentUserId(authentication);
        List<ChatRoomResponse> chatRooms = chatRoomService.getMyChatRooms(userId);
        
        logger.info("내 채팅방 목록 조회 완료: count={}", chatRooms.size());
        return ResponseEntity.ok(chatRooms);
    }
    
    /**
     * 채팅방 상세 조회
     */
    @GetMapping("/{roomId}")
    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        
        logger.info("채팅방 상세 조회 요청: roomId={}", roomId);
        
        try {
            ChatRoomResponse chatRoom = chatRoomService.getChatRoom(roomId);
            return ResponseEntity.ok(chatRoom);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 채팅방 생성
     */
    @PostMapping("")
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @Valid @RequestBody ChatRoomRequest request,
            Authentication authentication) {
        
        logger.info("채팅방 생성 요청: name={}, user={}", request.getName(), authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            ChatRoomResponse chatRoom = chatRoomService.createChatRoom(request, userId);
            
            logger.info("채팅방 생성 완료: roomId={}", chatRoom.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(chatRoom);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 생성 실패: error={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 채팅방 입장
     */
    @PostMapping("/{roomId}/join")
    @Operation(summary = "채팅방 입장", description = "지정된 채팅방에 입장합니다")
    @ApiResponse(responseCode = "200", description = "입장 성공")
    @ApiResponse(responseCode = "400", description = "입장 불가 (가득 참, 비활성화됨)")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<Map<String, Object>> joinChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("채팅방 입장 요청: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            ChatRoomResponse chatRoom = chatRoomService.joinChatRoom(roomId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "채팅방에 입장했습니다");
            response.put("chatRoom", chatRoom);
            
            logger.info("채팅방 입장 성공: roomId={}, userId={}", roomId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 입장 실패 - 찾을 수 없음: roomId={}, error={}", roomId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("채팅방 입장 실패 - 상태 오류: roomId={}, error={}", roomId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 채팅방 퇴장
     */
    @PostMapping("/{roomId}/leave")
    @Operation(summary = "채팅방 퇴장", description = "지정된 채팅방에서 퇴장합니다")
    @ApiResponse(responseCode = "200", description = "퇴장 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<Map<String, Object>> leaveChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("채팅방 퇴장 요청: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            chatRoomService.leaveChatRoom(roomId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "채팅방에서 퇴장했습니다");
            
            logger.info("채팅방 퇴장 성공: roomId={}, userId={}", roomId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 퇴장 실패: roomId={}, error={}", roomId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 채팅방 수정
     */
    @PutMapping("/{roomId}")
    @Operation(summary = "채팅방 정보 수정", description = "채팅방 생성자만 채팅방 정보를 수정할 수 있습니다")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<ChatRoomResponse> updateChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomRequest request,
            Authentication authentication) {
        
        logger.info("채팅방 수정 요청: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            ChatRoomResponse chatRoom = chatRoomService.updateChatRoom(roomId, request, userId);
            
            logger.info("채팅방 수정 완료: roomId={}", roomId);
            return ResponseEntity.ok(chatRoom);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 수정 실패 - 찾을 수 없음: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("채팅방 수정 실패 - 권한 없음: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    /**
     * 채팅방 삭제
     */
    @DeleteMapping("/{roomId}")
    @Operation(summary = "채팅방 삭제", description = "채팅방 생성자만 채팅방을 삭제할 수 있습니다")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<Map<String, Object>> deleteChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("채팅방 삭제 요청: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            chatRoomService.deleteChatRoom(roomId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "채팅방이 삭제되었습니다");
            
            logger.info("채팅방 삭제 완료: roomId={}", roomId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 삭제 실패 - 찾을 수 없음: roomId={}, error={}", roomId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("채팅방 삭제 실패 - 권한 없음: roomId={}, error={}", roomId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
    }
    
    /**
     * 채팅방 멤버 목록 조회
     */
    @GetMapping("/{roomId}/members")
    @Operation(summary = "채팅방 멤버 목록 조회", description = "지정된 채팅방의 릤버 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    public ResponseEntity<List<ChatRoomMemberInfo>> getRoomMembers(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        
        logger.debug("채팅방 멤버 목록 조회 요청: roomId={}", roomId);
        
        try {
            List<ChatRoomMemberInfo> members = chatRoomService.getRoomMembers(roomId);
            logger.debug("채팅방 멤버 목록 조회 완료: roomId={}, memberCount={}", roomId, members.size());
            return ResponseEntity.ok(members);
        } catch (IllegalArgumentException e) {
            logger.warn("채팅방 멤버 목록 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 채팅방 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "채팅방 통계 조회", description = "전체 채팅방 통계 정보를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<ChatRoomService.ChatRoomStats> getChatRoomStats() {
        logger.debug("채팅방 통계 조회 요청");
        
        ChatRoomService.ChatRoomStats stats = chatRoomService.getChatRoomStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 현재 인증된 사용자의 ID 추출
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.getUserByUsername(userDetails.getUsername()).getId();
    }
}