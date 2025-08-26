⏺ Backend REST API 엔드포인트 스펙

  🔗 Base URL

  http://localhost:8080

  🔐 인증

  모든 API는 JWT Bearer Token 인증 필요:
  Authorization: Bearer {accessToken}

  📊 분석 결과 조회 API

  1. 종합 분석 결과 조회

  GET /api/analysis/rooms/{roomId}/summary
  응답:
  {
    "keywords": {
      "roomId": 2,
      "topKeywords": [
        {"keyword": "분석", "count": 5},
        {"keyword": "실시간", "count": 3}
      ],
      "totalKeywords": 20,
      "lastUpdated": "2025-08-22T16:47:36"
    },
    "participation": {
      "roomId": 2,
      "userParticipation": [
        {"username": "testuser", "messageCount": 15}
      ],
      "totalUsers": 1,
      "lastUpdated": "2025-08-22T16:47:36"
    },
    "hourlyActivity": {
      "roomId": 2,
      "hourlyActivity": [
        {"hour": 0, "messageCount": 0},
        {"hour": 16, "messageCount": 1}
      ],
      "lastUpdated": "2025-08-22T16:47:36"
    }
  }

  2. 키워드 분석 결과

  GET /api/analysis/rooms/{roomId}/keywords
  응답:
  {
    "roomId": 2,
    "topKeywords": [
      {"keyword": "분석", "count": 5},
      {"keyword": "실시간", "count": 3},
      {"keyword": "키워드", "count": 2}
    ],
    "totalKeywords": 20,
    "lastUpdated": "2025-08-22T16:47:36"
  }

  3. 참여도 분석 결과

  GET /api/analysis/rooms/{roomId}/participation
  응답:
  {
    "roomId": 2,
    "userParticipation": [
      {"username": "testuser", "messageCount": 15},
      {"username": "user2", "messageCount": 8}
    ],
    "totalUsers": 2,
    "lastUpdated": "2025-08-22T16:47:36"
  }

  4. 시간대별 활동 분석

  GET /api/analysis/rooms/{roomId}/hourly
  응답:
  {
    "roomId": 2,
    "hourlyActivity": [
      {"hour": 0, "messageCount": 0},
      {"hour": 1, "messageCount": 0},
      {"hour": 16, "messageCount": 12},
      {"hour": 23, "messageCount": 0}
    ],
    "lastUpdated": "2025-08-22T16:47:36"
  }

  5. 분석 통계 요약

  GET /api/analysis/rooms/{roomId}/stats
  응답:
  {
    "roomId": 2,
    "totalAnalysisCount": 25,
    "keywordAnalysisCount": 10,
    "timePatternCount": 8,
    "participationCount": 7,
    "latestAnalysisDate": "2025-08-22T16:49:59.575638",
    "hasRealtimeData": true
  }

  6. 분석 결과 이력 (페이징)

  GET /api/analysis/rooms/{roomId}/history?page={page}&size={size}&analysisType={type}

  쿼리 파라미터:
  - page (optional): 페이지 번호 (기본값: 0)
  - size (optional): 페이지 크기 (기본값: 20)
  - analysisType (optional): 분석 타입 필터
    - KEYWORD_FREQUENCY
    - TIME_PATTERN
    - USER_PARTICIPATION

  응답:
  {
    "content": [
      {
        "id": 1,
        "roomId": 2,
        "analysisType": "KEYWORD_FREQUENCY",
        "analysisData": "{\"keywords\":[\"분석\",\"실시간\"]}",
        "messageCount": 5,
        "participantCount": 3,
        "analysisPeriodStart": "2025-08-22T16:00:00",
        "analysisPeriodEnd": "2025-08-22T17:00:00",
        "createdAt": "2025-08-22T16:49:59",
        "updatedAt": "2025-08-22T16:49:59"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {"sorted": true, "unsorted": false}
    },
    "totalElements": 25,
    "totalPages": 2,
    "first": true,
    "last": false,
    "numberOfElements": 20
  }

  7. 기간별 분석 결과

  GET /api/analysis/rooms/{roomId}/period?startDate={startDate}&endDate={endDate}&analysisType={type}

  쿼리 파라미터:
  - startDate (required): 시작 날짜시간 (ISO 8601 형식)
  - endDate (required): 종료 날짜시간 (ISO 8601 형식)
  - analysisType (optional): 분석 타입 필터

  예시:
  GET /api/analysis/rooms/2/period?startDate=2025-08-22T00:00:00&endDate=2025-08-22T23:59:59

  응답:
  [
    {
      "id": 1,
      "roomId": 2,
      "analysisType": "KEYWORD_FREQUENCY",
      "analysisData": "{\"keywords\":[\"분석\",\"실시간\"]}",
      "messageCount": 5,
      "participantCount": 3,
      "analysisPeriodStart": "2025-08-22T16:00:00",
      "analysisPeriodEnd": "2025-08-22T17:00:00",
      "createdAt": "2025-08-22T17:00:00"
    }
  ]

  8. 캐시된 종합 분석 결과

  GET /api/analysis/rooms/{roomId}
  응답: (summary와 동일한 형식)

  9. 분석 데이터 초기화 (개발/테스트용)

  DELETE /api/analysis/rooms/{roomId}/clear
  응답:
  {
    "success": "true",
    "message": "채팅방 분석 데이터가 초기화되었습니다",
    "roomId": "2"
  }

  🔌 WebSocket 실시간 분석 데이터

  구독 엔드포인트

  /topic/analysis/{roomId}

  실시간 메시지 형식

  키워드 분석 업데이트:
  {
    "type": "KEYWORD_UPDATE",
    "roomId": 2,
    "timestamp": "2025-08-22T16:40:38",
    "keywords": {
      "labels": ["분석", "실시간", "키워드"],
      "datasets": [{
        "label": "키워드 빈도",
        "data": [5, 3, 2],
        "backgroundColor": ["#FF6384", "#36A2EB", "#FFCE56"]
      }]
    }
  }

  참여도 분석 업데이트:
  {
    "type": "PARTICIPATION_UPDATE",
    "roomId": 2,
    "timestamp": "2025-08-22T16:40:38",
    "participation": {
      "labels": ["user1", "user2"],
      "datasets": [{
        "label": "메시지 수",
        "data": [15, 8],
        "backgroundColor": ["#FF6384", "#36A2EB"]
      }]
    }
  }

  시간대별 활동 분석 업데이트:
  {
    "type": "HOURLY_UPDATE",
    "roomId": 2,
    "timestamp": "2025-08-22T16:40:38",
    "hourlyActivity": {
      "labels": ["00", "01", "02", "...", "23"],
      "datasets": [{
        "label": "시간대별 메시지 수",
        "data": [0, 0, 0, "...", 12],
        "borderColor": "#36A2EB",
        "fill": false
      }]
    }
  }

  ❌ 에러 응답

  인증 실패 (401):
  {
    "timestamp": "2025-08-22T07:47:02.739265",
    "status": 401,
    "error": "Unauthorized",
    "message": "인증에 실패했습니다",
    "path": "/api/analysis/rooms/2"
  }

  존재하지 않는 리소스 (404):
  {
    "timestamp": "2025-08-22T07:47:16.118+00:00",
    "status": 404,
    "error": "Not Found",
    "path": "/api/analysis/rooms/999"
  }

  잘못된 날짜 형식 (400):
  {
    "timestamp": "2025-08-22T07:47:16.118+00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "잘못된 날짜 형식입니다",
    "path": "/api/analysis/rooms/2/period"
  }