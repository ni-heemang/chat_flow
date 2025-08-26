package com.flowchat.controller;

import com.flowchat.entity.AnalysisResult;
import com.flowchat.repository.AnalysisResultRepository;
import com.flowchat.service.ChatAnalysisService;
import com.flowchat.service.TopicClassificationService;
import com.flowchat.service.ConversationFlowService;
import com.flowchat.service.ChatRoomService;
import com.flowchat.service.UserService;
import com.flowchat.dto.ChatRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "채팅 분석", description = "실시간 채팅 분석 API")
public class ChatAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(ChatAnalysisController.class);

    @Autowired
    private ChatAnalysisService chatAnalysisService;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private TopicClassificationService topicClassificationService;

    @Autowired
    private ConversationFlowService conversationFlowService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    /**
     * 채팅방 키워드 통계 조회
     */
    @GetMapping("/rooms/{roomId}/keywords")
    @Operation(summary = "채팅방 키워드 통계", description = "채팅방의 실시간 키워드 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomKeywordStats(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("키워드 통계 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomKeywordStats(roomId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("키워드 통계 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방 사용자 참여도 통계 조회
     */
    @GetMapping("/rooms/{roomId}/participation")
    @Operation(summary = "채팅방 참여도 통계", description = "채팅방의 사용자별 참여도 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomUserParticipation(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("참여도 통계 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomUserParticipation(roomId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("참여도 통계 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방 시간대별 활동 통계 조회
     */
    @GetMapping("/rooms/{roomId}/hourly")
    @Operation(summary = "채팅방 시간대별 통계", description = "채팅방의 시간대별 활동 패턴 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomHourlyStats(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("시간대별 통계 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomHourlyStats(roomId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("시간대별 통계 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방 종합 분석 결과 조회
     */
    @GetMapping("/rooms/{roomId}/summary")
    @Operation(summary = "채팅방 종합 분석", description = "채팅방의 전체 분석 결과를 종합하여 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomAnalysisSummary(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("종합 분석 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> summary = Map.of(
                "keywords", chatAnalysisService.getRoomKeywordStats(roomId),
                "participation", chatAnalysisService.getRoomUserParticipation(roomId),
                "hourlyActivity", chatAnalysisService.getRoomHourlyStats(roomId)
            );
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("종합 분석 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방 분석 데이터 초기화 (관리자/테스트용)
     */
    @DeleteMapping("/rooms/{roomId}/clear")
    @Operation(summary = "분석 데이터 초기화", description = "채팅방의 모든 분석 데이터를 초기화합니다 (테스트용)")
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, String>> clearRoomAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("분석 데이터 초기화: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            chatAnalysisService.clearRoomAnalysis(roomId);
            
            Map<String, String> response = Map.of(
                "success", "true",
                "message", "채팅방 분석 데이터가 초기화되었습니다",
                "roomId", roomId.toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("분석 데이터 초기화 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅방 종합 분석 결과 조회 (캐싱 적용)
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "채팅방 종합 분석 결과", description = "채팅방의 전체 분석 결과를 조회합니다 (캐싱 적용)")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @Cacheable(value = "roomAnalysisSummary", key = "#roomId")
    public ResponseEntity<Map<String, Object>> getRoomAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("종합 분석 조회 (캐싱): roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> analysis = Map.of(
                "keywords", chatAnalysisService.getRoomKeywordStats(roomId),
                "participation", chatAnalysisService.getRoomUserParticipation(roomId),
                "hourlyActivity", chatAnalysisService.getRoomHourlyStats(roomId)
            );
            
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            logger.error("종합 분석 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 저장된 분석 결과 이력 조회
     */
    @GetMapping("/rooms/{roomId}/history")
    @Operation(summary = "분석 결과 이력", description = "채팅방의 저장된 분석 결과 이력을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Page<AnalysisResult>> getAnalysisHistory(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "분석 타입") @RequestParam(required = false) AnalysisResult.AnalysisType analysisType,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        logger.info("분석 이력 조회: roomId={}, type={}, user={}", roomId, analysisType, authentication.getName());
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<AnalysisResult> results;
            
            if (analysisType != null) {
                results = analysisResultRepository.findByRoomIdAndAnalysisTypeOrderByCreatedAtDesc(
                    roomId, analysisType, pageable);
            } else {
                results = analysisResultRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("분석 이력 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 기간별 분석 결과 조회
     */
    @GetMapping("/rooms/{roomId}/period")
    @Operation(summary = "기간별 분석 결과", description = "특정 기간의 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<List<AnalysisResult>> getAnalysisByPeriod(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "시작 날짜시간") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜시간") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "분석 타입") @RequestParam(required = false) AnalysisResult.AnalysisType analysisType,
            Authentication authentication) {
        
        logger.info("기간별 분석 조회: roomId={}, start={}, end={}, type={}, user={}", 
                   roomId, startDate, endDate, analysisType, authentication.getName());
        
        try {
            List<AnalysisResult> results;
            
            if (analysisType != null) {
                results = analysisResultRepository.findByRoomIdAndAnalysisTypeAndAnalysisPeriodStartBetweenOrderByCreatedAtDesc(
                    roomId, analysisType, startDate, endDate);
            } else {
                results = analysisResultRepository.findByRoomIdAndAnalysisPeriodStartBetweenOrderByCreatedAtDesc(
                    roomId, startDate, endDate);
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("기간별 분석 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 분석 통계 요약
     */
    @GetMapping("/rooms/{roomId}/stats")
    @Operation(summary = "분석 통계 요약", description = "채팅방의 분석 통계 요약 정보를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getAnalysisStats(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("분석 통계 요약 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            long totalAnalysisCount = analysisResultRepository.countByRoomId(roomId);
            long keywordAnalysisCount = analysisResultRepository.countByRoomIdAndAnalysisType(
                roomId, AnalysisResult.AnalysisType.KEYWORD_FREQUENCY);
            long timePatternCount = analysisResultRepository.countByRoomIdAndAnalysisType(
                roomId, AnalysisResult.AnalysisType.TIME_PATTERN);
            long participationCount = analysisResultRepository.countByRoomIdAndAnalysisType(
                roomId, AnalysisResult.AnalysisType.USER_PARTICIPATION);
            
            AnalysisResult latestResult = analysisResultRepository.findTopByRoomIdOrderByCreatedAtDesc(roomId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("roomId", roomId);
            stats.put("totalAnalysisCount", totalAnalysisCount);
            stats.put("keywordAnalysisCount", keywordAnalysisCount);
            stats.put("timePatternCount", timePatternCount);
            stats.put("participationCount", participationCount);
            stats.put("latestAnalysisDate", latestResult != null ? latestResult.getCreatedAt() : null);
            Map<String, Object> participationStats = chatAnalysisService.getRoomUserParticipation(roomId);
            stats.put("hasRealtimeData", (Integer) participationStats.get("totalUsers") > 0);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("분석 통계 요약 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 주제별 분석 결과 조회
     */
    @GetMapping("/rooms/{roomId}/topics")
    @Operation(summary = "주제별 분석 결과", description = "채팅방의 주제 분류 및 감정 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getTopicAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("주제별 분석 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            // 최근 주제 분류 결과 조회
            List<AnalysisResult> topicResults = analysisResultRepository
                .findByRoomIdAndAnalysisTypeOrderByCreatedAtDesc(roomId, AnalysisResult.AnalysisType.TOPIC_CLASSIFICATION)
                .stream()
                .limit(50)
                .collect(Collectors.toList());
            
            Map<String, Integer> topicCounts = new HashMap<>();
            Map<String, Integer> emotionCounts = new HashMap<>();
            
            // 주제 및 감정 분포 계산
            for (AnalysisResult result : topicResults) {
                try {
                    String data = result.getAnalysisData();
                    // 간단한 JSON 파싱 (실제로는 Jackson 사용 권장)
                    if (data.contains("\"topic\":")) {
                        String topic = extractJsonValue(data, "topic");
                        String emotion = extractJsonValue(data, "emotion");
                        
                        topicCounts.merge(topic, 1, Integer::sum);
                        emotionCounts.merge(emotion, 1, Integer::sum);
                    }
                } catch (Exception e) {
                    logger.warn("주제 분석 데이터 파싱 실패: {}", e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("topicDistribution", topicCounts);
            response.put("emotionDistribution", emotionCounts);
            response.put("totalAnalyzedMessages", topicResults.size());
            response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("주제별 분석 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 대화 흐름 분석 결과 조회
     */
    @GetMapping("/rooms/{roomId}/conversation-flow")
    @Operation(summary = "대화 흐름 분석", description = "채팅방의 대화 흐름 및 패턴 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getConversationFlow(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("대화 흐름 분석 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            Map<String, Object> flowStats = conversationFlowService.getConversationFlowStats(roomId);
            Map<String, Object> patterns = conversationFlowService.analyzeConversationPatterns(roomId);
            String activity = conversationFlowService.getConversationActivity(roomId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("flowStatistics", flowStats);
            response.put("conversationPatterns", patterns);
            response.put("currentActivity", activity);
            response.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("대화 흐름 분석 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 심화 분석 종합 결과 조회
     */
    @GetMapping("/rooms/{roomId}/advanced")
    @Operation(summary = "심화 분석 종합 결과", description = "주제 분류, 감정 분석, 대화 흐름을 포함한 심화 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @Cacheable(value = "advancedAnalysisSummary", key = "#roomId")
    public ResponseEntity<Map<String, Object>> getAdvancedAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("심화 분석 종합 조회: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            // 주제 분석
            ResponseEntity<Map<String, Object>> topicResponse = getTopicAnalysis(roomId, authentication);
            Map<String, Object> topicData = topicResponse.getStatusCode().is2xxSuccessful() ? 
                topicResponse.getBody() : new HashMap<>();
            
            // 대화 흐름 분석
            ResponseEntity<Map<String, Object>> flowResponse = getConversationFlow(roomId, authentication);
            Map<String, Object> flowData = flowResponse.getStatusCode().is2xxSuccessful() ? 
                flowResponse.getBody() : new HashMap<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("topicAnalysis", topicData);
            response.put("conversationFlow", flowData);
            response.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("심화 분석 종합 조회 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 분석 데이터 재구축 (기존 메시지 기반)
     */
    @PostMapping("/rooms/{roomId}/rebuild")
    @Operation(summary = "분석 데이터 재구축", description = "채팅방의 기존 메시지를 기반으로 분석 데이터를 재구축합니다")
    @ApiResponse(responseCode = "200", description = "재구축 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, String>> rebuildRoomAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("분석 데이터 재구축: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            chatAnalysisService.rebuildRoomAnalysis(roomId);
            
            Map<String, String> response = Map.of(
                "success", "true",
                "message", "분석 데이터가 재구축되었습니다",
                "roomId", roomId.toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("분석 데이터 재구축 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 모든 활성 채팅방의 분석 데이터 재구축
     */
    @PostMapping("/rebuild-all")
    @Operation(summary = "모든 채팅방 분석 데이터 재구축", description = "모든 활성 채팅방의 분석 데이터를 재구축합니다")
    @ApiResponse(responseCode = "200", description = "재구축 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, String>> rebuildAllRoomAnalysis(Authentication authentication) {
        
        logger.info("모든 채팅방 분석 데이터 재구축: user={}", authentication.getName());
        
        try {
            chatAnalysisService.rebuildAllRoomAnalysis();
            
            Map<String, String> response = Map.of(
                "success", "true",
                "message", "모든 채팅방의 분석 데이터가 재구축되었습니다"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("모든 채팅방 분석 데이터 재구축 실패: error={}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 분석 데이터 초기화 시 심화 분석 데이터도 함께 삭제
     */
    @DeleteMapping("/rooms/{roomId}/clear-advanced")
    @Operation(summary = "심화 분석 데이터 초기화", description = "채팅방의 심화 분석 데이터를 초기화합니다")
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, String>> clearAdvancedAnalysis(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            Authentication authentication) {
        
        logger.info("심화 분석 데이터 초기화: roomId={}, user={}", roomId, authentication.getName());
        
        try {
            // 대화 흐름 데이터 초기화
            conversationFlowService.clearConversationData(roomId);
            
            // 캐시 무효화
            evictAdvancedAnalysisCache(roomId);
            
            Map<String, String> response = Map.of(
                "success", "true",
                "message", "심화 분석 데이터가 초기화되었습니다",
                "roomId", roomId.toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("심화 분석 데이터 초기화 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 기간별 키워드 분석 조회
     */
    @GetMapping("/rooms/{roomId}/keywords/period")
    @Operation(summary = "기간별 키워드 분석", description = "지정된 기간의 키워드 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomKeywordStatsByPeriod(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "기간 (일 단위: 1, 7, 30, null=전체)") @RequestParam(required = false) Integer days,
            Authentication authentication) {
        
        logger.info("기간별 키워드 분석 조회: roomId={}, days={}, user={}", roomId, days, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomKeywordStatsByPeriod(roomId, days);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("기간별 키워드 분석 조회 실패: roomId={}, days={}, error={}", roomId, days, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 기간별 참여도 분석 조회
     */
    @GetMapping("/rooms/{roomId}/participation/period")
    @Operation(summary = "기간별 참여도 분석", description = "지정된 기간의 참여도 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomUserParticipationByPeriod(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "기간 (일 단위: 1, 7, 30, null=전체)") @RequestParam(required = false) Integer days,
            Authentication authentication) {
        
        logger.info("기간별 참여도 분석 조회: roomId={}, days={}, user={}", roomId, days, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomUserParticipationByPeriod(roomId, days);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("기간별 참여도 분석 조회 실패: roomId={}, days={}, error={}", roomId, days, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 기간별 시간대별 활동 분석 조회
     */
    @GetMapping("/rooms/{roomId}/hourly/period")
    @Operation(summary = "기간별 시간대별 분석", description = "지정된 기간의 시간대별 활동 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomHourlyStatsByPeriod(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "기간 (일 단위: 1, 7, 30, null=전체)") @RequestParam(required = false) Integer days,
            Authentication authentication) {
        
        logger.info("기간별 시간대별 분석 조회: roomId={}, days={}, user={}", roomId, days, authentication.getName());
        
        try {
            Map<String, Object> stats = chatAnalysisService.getRoomHourlyStatsByPeriod(roomId, days);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("기간별 시간대별 분석 조회 실패: roomId={}, days={}, error={}", roomId, days, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 기간별 종합 분석 조회
     */
    @GetMapping("/rooms/{roomId}/summary/period")
    @Operation(summary = "기간별 종합 분석", description = "지정된 기간의 전체 분석 결과를 종합하여 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getRoomAnalysisSummaryByPeriod(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "기간 (일 단위: 1, 7, 30, null=전체)") @RequestParam(required = false) Integer days,
            Authentication authentication) {
        
        logger.info("기간별 종합 분석 조회: roomId={}, days={}, user={}", roomId, days, authentication.getName());
        
        try {
            Map<String, Object> summary = Map.of(
                "keywords", chatAnalysisService.getRoomKeywordStatsByPeriod(roomId, days),
                "participation", chatAnalysisService.getRoomUserParticipationByPeriod(roomId, days),
                "hourlyActivity", chatAnalysisService.getRoomHourlyStatsByPeriod(roomId, days),
                "period", days != null ? days + "일" : "전체 기간"
            );
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("기간별 종합 분석 조회 실패: roomId={}, days={}, error={}", roomId, days, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자가 참여 중인 채팅방 목록 조회 (분석용 드롭다운)
     */
    @GetMapping("/user/joined-rooms")
    @Operation(summary = "사용자 참여 채팅방 목록 조회", description = "분석창에서 사용할 사용자가 참여 중인 채팅방 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<List<ChatRoomResponse>> getUserJoinedRooms(Authentication authentication) {
        logger.debug("분석용 사용자 참여 채팅방 목록 조회 요청: user={}", authentication.getName());
        
        try {
            Long userId = getCurrentUserId(authentication);
            List<ChatRoomResponse> rooms = chatRoomService.getUserJoinedRooms(userId);
            
            logger.debug("분석용 사용자 참여 채팅방 목록 조회 완료: user={}, count={}", authentication.getName(), rooms.size());
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            logger.error("분석용 사용자 참여 채팅방 목록 조회 실패: user={}, error={}", authentication.getName(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * JSON에서 값을 추출하는 헬퍼 메소드
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "기타";
        
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "기타";
        
        return json.substring(start, end);
    }

    /**
     * 심화 분석 캐시 무효화
     */
    @CacheEvict(value = "advancedAnalysisSummary", key = "#roomId")
    private void evictAdvancedAnalysisCache(Long roomId) {
        logger.debug("심화 분석 캐시 무효화: roomId={}", roomId);
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