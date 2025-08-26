package com.flowchat.controller;

import com.flowchat.dto.ChatMessageResponse;
import com.flowchat.entity.ChatMessage;
import com.flowchat.repository.ChatMessageRepository;
import com.flowchat.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(MessageHistoryController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * 특정 채팅방의 메시지 히스토리 조회 (페이징)
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomMessageHistory(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        try {
            logger.info("메시지 히스토리 조회 요청: roomId={}, page={}, size={}, user={}", 
                       roomId, page, size, authentication.getName());
            
            // 정렬 방향 설정
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? 
                                     Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortBy);
            
            // 페이징 설정
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // 메시지 조회
            Page<ChatMessage> messagePage = chatMessageRepository
                .findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId, pageable);
            
            // ChatMessage -> ChatMessageResponse 변환
            List<ChatMessageResponse> messages = messagePage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("currentPage", messagePage.getNumber());
            response.put("totalPages", messagePage.getTotalPages());
            response.put("totalElements", messagePage.getTotalElements());
            response.put("hasNext", messagePage.hasNext());
            response.put("hasPrevious", messagePage.hasPrevious());
            response.put("roomId", roomId);
            
            logger.info("메시지 히스토리 조회 완료: roomId={}, 조회된 메시지 수={}", 
                       roomId, messages.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메시지 히스토리 조회 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "메시지 히스토리 조회 실패");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("roomId", roomId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 채팅방의 최근 메시지 조회 (채팅방 입장 시 사용)
     */
    @GetMapping("/room/{roomId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        
        try {
            logger.info("최근 메시지 조회 요청: roomId={}, limit={}, user={}", 
                       roomId, limit, authentication.getName());
            
            // 최근 메시지 조회 (limit 개수만큼)
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<ChatMessage> messagePage = chatMessageRepository
                .findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId, pageable);
            
            // 시간순으로 정렬 (최신 메시지가 마지막에 오도록)
            List<ChatMessageResponse> messages = messagePage.getContent().stream()
                .map(this::convertToResponse)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
            
            // 전체 메시지 수 조회
            long totalMessages = chatMessageRepository.countByRoomId(roomId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("messageCount", messages.size());
            response.put("totalMessages", totalMessages);
            response.put("roomId", roomId);
            response.put("hasMore", totalMessages > limit);
            
            logger.info("최근 메시지 조회 완료: roomId={}, 조회된 메시지 수={}, 전체 메시지 수={}", 
                       roomId, messages.size(), totalMessages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("최근 메시지 조회 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "최근 메시지 조회 실패");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("roomId", roomId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 채팅방의 메시지 통계 조회
     */
    @GetMapping("/room/{roomId}/stats")
    public ResponseEntity<Map<String, Object>> getRoomMessageStats(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        try {
            logger.info("메시지 통계 조회 요청: roomId={}, user={}", roomId, authentication.getName());
            
            // 전체 메시지 수
            long totalMessages = chatMessageRepository.countByRoomId(roomId);
            
            // 메시지 타입별 통계
            List<ChatMessage> allMessages = chatMessageRepository
                .findByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId, Pageable.unpaged()).getContent();
            
            Map<String, Long> messageTypeCounts = allMessages.stream()
                .collect(Collectors.groupingBy(
                    msg -> msg.getMessageType().toString(),
                    Collectors.counting()
                ));
            
            // 사용자별 메시지 수 (상위 10명)
            Map<String, Long> userMessageCounts = allMessages.stream()
                .filter(msg -> msg.getUsername() != null)
                .collect(Collectors.groupingBy(
                    ChatMessage::getUsername,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("totalMessages", totalMessages);
            response.put("messageTypeCounts", messageTypeCounts);
            response.put("topUsers", userMessageCounts);
            response.put("activeUsers", userMessageCounts.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메시지 통계 조회 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "메시지 통계 조회 실패");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("roomId", roomId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 메시지 검색
     */
    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<Map<String, Object>> searchMessages(
            @PathVariable Long roomId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        try {
            logger.info("메시지 검색 요청: roomId={}, keyword={}, user={}", 
                       roomId, keyword, authentication.getName());
            
            List<ChatMessage> searchResults = chatMessageRepository.searchByKeyword(roomId, keyword);
            
            // 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, searchResults.size());
            List<ChatMessage> pagedResults = searchResults.subList(start, end);
            
            List<ChatMessageResponse> messages = pagedResults.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("totalResults", searchResults.size());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("keyword", keyword);
            response.put("roomId", roomId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메시지 검색 실패: roomId={}, keyword={}, error={}", 
                        roomId, keyword, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "메시지 검색 실패");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * ChatMessage 엔티티를 ChatMessageResponse DTO로 변환
     */
    private ChatMessageResponse convertToResponse(ChatMessage message) {
        return ChatMessageResponse.from(
            message,
            message.getUsername() != null ? message.getUsername() : "unknown",
            message.getName() != null ? message.getName() : "Unknown"
        );
    }
}