package com.flowchat.controller;

import com.flowchat.service.ChatRoomMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "관리자 API")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private ChatRoomMemberService chatRoomMemberService;
    
    /**
     * 모든 채팅방 멤버십 데이터 초기화 (개발용)
     */
    @DeleteMapping("/members/reset")
    @Operation(summary = "채팅방 멤버십 초기화", description = "모든 채팅방 멤버십 데이터를 삭제합니다 (개발용)")
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    public ResponseEntity<Map<String, Object>> resetChatRoomMembers() {
        logger.warn("채팅방 멤버십 데이터 초기화 요청");
        
        try {
            chatRoomMemberService.resetAllMembers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 채팅방 멤버십 데이터가 초기화되었습니다");
            
            logger.info("채팅방 멤버십 데이터 초기화 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("채팅방 멤버십 데이터 초기화 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "초기화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}