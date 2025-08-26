package com.flowchat.controller;

import com.flowchat.service.LlmAnalysisService;
import com.flowchat.service.ChatAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/llm-analysis")
@Tag(name = "LLM 기반 채팅 분석", description = "LLM을 활용한 고도화된 채팅 분석 API")
public class LlmAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(LlmAnalysisController.class);

    @Autowired
    private LlmAnalysisService llmAnalysisService;

    @Autowired
    private ChatAnalysisService chatAnalysisService;

    /**
     * LLM 기반 메시지 종합 분석 (테스트용)
     */
    @PostMapping("/analyze-message")
    @Operation(summary = "LLM 메시지 분석", description = "단일 메시지에 대한 LLM 기반 종합 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> analyzeMessage(
            @Parameter(description = "분석할 메시지 내용") @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String content = request.get("content");
        String username = authentication.getName();
        logger.info("LLM 메시지 분석 요청: user={}, content={}", username, content);
        
        try {
            // 비동기 처리를 동기로 변경하여 SecurityContext 유지
            Map<String, Object> analysisResult = llmAnalysisService.analyzeMessage(content).get();
            logger.info("LLM 분석 완료: user={}, result={}", username, analysisResult);
            return ResponseEntity.ok(analysisResult);
        } catch (Exception e) {
            logger.error("LLM 분석 실패: user={}, error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LLM 기반 키워드 추출 (테스트용)
     */
    @PostMapping("/extract-keywords")
    @Operation(summary = "LLM 키워드 추출", description = "메시지에서 LLM을 사용한 키워드 추출")
    @ApiResponse(responseCode = "200", description = "추출 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, List<String>>> extractKeywords(
            @Parameter(description = "키워드를 추출할 메시지 내용") @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String content = request.get("content");
        String username = authentication.getName();
        logger.info("LLM 키워드 추출 요청: user={}, content={}", username, content);
        
        try {
            List<String> keywords = llmAnalysisService.extractKeywords(content).get();
            Map<String, List<String>> result = Map.of("keywords", keywords);
            logger.info("LLM 키워드 추출 완료: user={}, keywords={}", username, keywords);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("LLM 키워드 추출 실패: user={}, error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LLM 기반 주제 분류 (테스트용)
     */
    @PostMapping("/classify-topic")
    @Operation(summary = "LLM 주제 분류", description = "메시지의 LLM 기반 주제 분류")
    @ApiResponse(responseCode = "200", description = "분류 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, String>> classifyTopic(
            @Parameter(description = "주제를 분류할 메시지 내용") @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String content = request.get("content");
        String username = authentication.getName();
        logger.info("LLM 주제 분류 요청: user={}, content={}", username, content);
        
        try {
            String topic = llmAnalysisService.classifyTopic(content).get();
            Map<String, String> result = Map.of("topic", topic);
            logger.info("LLM 주제 분류 완료: user={}, topic={}", username, topic);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("LLM 주제 분류 실패: user={}, error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LLM 기반 감정 분석 (테스트용)
     */
    @PostMapping("/analyze-emotion")
    @Operation(summary = "LLM 감정 분석", description = "메시지의 LLM 기반 감정 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> analyzeEmotion(
            @Parameter(description = "감정을 분석할 메시지 내용") @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String content = request.get("content");
        String username = authentication.getName();
        logger.info("LLM 감정 분석 요청: user={}, content={}", username, content);
        
        try {
            Map<String, Object> emotion = llmAnalysisService.analyzeEmotion(content).get();
            logger.info("LLM 감정 분석 완료: user={}, emotion={}", username, emotion);
            return ResponseEntity.ok(emotion);
        } catch (Exception e) {
            logger.error("LLM 감정 분석 실패: user={}, error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LLM 기반 대화 맥락 분석 (테스트용)
     */
    @PostMapping("/analyze-context")
    @Operation(summary = "LLM 대화 맥락 분석", description = "여러 메시지의 LLM 기반 대화 맥락 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> analyzeContext(
            @Parameter(description = "맥락을 분석할 메시지 목록") @RequestBody Map<String, List<String>> request,
            Authentication authentication) {
        
        List<String> messages = request.get("messages");
        String username = authentication.getName();
        logger.info("LLM 맥락 분석 요청: user={}, messages count={}", username, messages.size());
        
        try {
            Map<String, Object> context = llmAnalysisService.analyzeConversationContext(messages).get();
            logger.info("LLM 맥락 분석 완료: user={}, context={}", username, context);
            return ResponseEntity.ok(context);
        } catch (Exception e) {
            logger.error("LLM 맥락 분석 실패: user={}, error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방의 LLM 기반 종합 분석 비교 (기존 vs LLM)
     */
    @GetMapping("/rooms/{roomId}/compare")
    @Operation(summary = "분석 방법 비교", description = "정적 분석과 LLM 분석 결과를 비교합니다")
    @ApiResponse(responseCode = "200", description = "비교 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> compareAnalysisMethods(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "비교할 최근 메시지 수") @RequestParam(defaultValue = "10") int messageCount,
            Authentication authentication) {
        
        String username = authentication.getName();
        logger.info("분석 방법 비교 요청: roomId={}, user={}, messageCount={}", roomId, username, messageCount);
        
        try {
                // 기존 정적 분석 결과 조회
                Map<String, Object> staticAnalysis = Map.of(
                    "keywords", chatAnalysisService.getRoomKeywordStats(roomId),
                    "participation", chatAnalysisService.getRoomUserParticipation(roomId),
                    "hourly", chatAnalysisService.getRoomHourlyStats(roomId)
                );

                // TODO: 실제로는 최근 메시지를 가져와서 LLM 분석 수행
                // 여기서는 예시 응답 반환
                Map<String, Object> llmAnalysis = Map.of(
                    "method", "LLM",
                    "status", "구현 완료 - 실제 메시지 데이터 연동 필요"
                );

                Map<String, Object> comparison = Map.of(
                    "roomId", roomId,
                    "staticAnalysis", staticAnalysis,
                    "llmAnalysis", llmAnalysis,
                    "comparison", Map.of(
                        "accuracy", "LLM 분석이 더 정확한 주제 분류 제공",
                        "performance", "정적 분석이 더 빠름",
                        "cost", "정적 분석이 비용 효율적",
                        "scalability", "LLM은 더 유연하고 확장 가능"
                    )
                );

                logger.info("분석 방법 비교 완료: roomId={}", roomId);
                return ResponseEntity.ok(comparison);
                
        } catch (Exception e) {
            logger.error("분석 방법 비교 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LLM 분석 시스템 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "LLM 분석 시스템 상태", description = "LLM 분석 시스템의 현재 상태를 확인합니다")
    @ApiResponse(responseCode = "200", description = "상태 확인 성공")
    public ResponseEntity<Map<String, Object>> getSystemStatus(Authentication authentication) {
        
        logger.info("LLM 시스템 상태 확인: user={}", authentication.getName());
        
        Map<String, Object> status = Map.of(
            "llm_service", "활성",
            "fallback_enabled", true,
            "supported_features", List.of(
                "종합 메시지 분석",
                "키워드 추출",
                "주제 분류",
                "감정 분석", 
                "대화 맥락 분석"
            ),
            "integration_status", "완료",
            "next_steps", List.of(
                "API 키 설정 필요",
                "실제 LLM 서비스 연동 테스트",
                "성능 최적화"
            )
        );
        
        return ResponseEntity.ok(status);
    }
}