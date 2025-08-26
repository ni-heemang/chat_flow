  🔹 3.1 채팅 + 분석 통합 뷰용 API

  1. 실시간 채팅 분석 WebSocket

  // WebSocket 연결
  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);

  // 분석 결과 구독
  stompClient.subscribe('/topic/analysis/{roomId}', (message) => {
    const analysisData = JSON.parse(message.body);
  });

  // 분석 새로고침 요청
  stompClient.send('/app/analysis/{roomId}/refresh', {}, JSON.stringify({}));

  2. 기본 채팅방 분석 데이터

  GET /api/analysis/rooms/{roomId}
  Authorization: Bearer {jwt-token}

  Response:
  {
    "basicStats": {
      "totalMessages": 150,
      "activeUsers": 8,
      "averageResponseTime": 45.5,
      "peakHour": 14
    },
    "keywordStats": {
      "프로젝트": 25,
      "회의": 18,
      "개발": 22,
      "완료": 15
    },
    "participationStats": {
      "user1": {
        "messageCount": 45,
        "responseTime": 30.2,
        "lastActive": "2024-08-22T16:30:00"
      },
      "user2": {
        "messageCount": 32,
        "responseTime": 52.1,
        "lastActive": "2024-08-22T16:28:00"
      }
    },
    "hourlyStats": {
      "9": 12,
      "10": 25,
      "11": 18,
      "14": 35,
      "15": 28
    },
    "generatedAt": "2024-08-22T16:35:00"
  }

  3. 주제별 분석 (새로운 고급 기능)

  GET /api/analysis/rooms/{roomId}/topics
  Authorization: Bearer {jwt-token}

  Response:
  {
    "topicDistribution": {
      "업무": 45,
      "일상": 12,
      "문제": 8,
      "기술": 32,
      "팀워크": 18
    },
    "emotionDistribution": {
      "긍정": 78,
      "부정": 15,
      "중립": 22
    },
    "totalMessages": 115,
    "analysisDate": "2024-08-22",
    "lastUpdated": "2024-08-22T16:35:00"
  }

  4. 대화 흐름 분석 (새로운 고급 기능)

  GET /api/analysis/rooms/{roomId}/conversation-flow
  Authorization: Bearer {jwt-token}

  Response:
  {
    "sessionCount": 5,
    "averageSessionDuration": 1800000,
    "totalActiveDuration": 9000000,
    "averageResponseTime": 45000,
    "participantStats": {
      "user1": {
        "sessionCount": 3,
        "totalMessages": 45,
        "averageResponseTime": 30000
      },
      "user2": {
        "sessionCount": 4,
        "totalMessages": 32,
        "averageResponseTime": 55000
      }
    },
    "hourlyPatterns": {
      "9": 8,
      "10": 15,
      "11": 12,
      "14": 22,
      "15": 18
    },
    "conversationBreaks": [
      {
        "startTime": "2024-08-22T10:30:00",
        "endTime": "2024-08-22T11:45:00",
        "duration": 4500000
      }
    ]
  }

  5. 종합 고급 분석

  GET /api/analysis/rooms/{roomId}/advanced
  Authorization: Bearer {jwt-token}

  Response:
  {
    "basicStats": { /* 위의 기본 분석 데이터 */ },
    "topicAnalysis": { /* 위의 주제 분석 데이터 */ },
    "conversationFlow": { /* 위의 대화 흐름 데이터 */ },
    "summary": {
      "mostActiveUser": "user1",
      "dominantTopic": "업무",
      "overallSentiment": "긍정",
      "peakActivityTime": "14:00-15:00",
      "averageSessionLength": "30분"
    },
    "generatedAt": "2024-08-22T16:35:00"
  }

  🔹 3.2 사용자 경험 개선용 API

  6. 메시지 검색

  GET /api/rooms/{roomId}/search?keyword={검색어}&page=0&size=20
  Authorization: Bearer {jwt-token}

  Response:
  {
    "content": [
      {
        "id": 123,
        "content": "프로젝트 진행 상황을 확인해주세요",
        "username": "user1",
        "name": "김개발",
        "timestamp": "2024-08-22T14:30:00",
        "messageType": "TEXT",
        "highlight": "프로젝트"
      }
    ],
    "page": {
      "size": 20,
      "number": 0,
      "totalElements": 45,
      "totalPages": 3
    }
  }

  7. 시스템 상태 체크 (에러 처리용)

  GET /api/health
  (인증 불필요)

  Response:
  {
    "service": "flowchat-backend",
    "version": "0.0.1-SNAPSHOT",
    "status": "UP",
    "timestamp": "2024-08-22T16:35:00"
  }

  🔹 3.3 고급 기능용 API

  8. 보고서 생성 (PDF 내보내기 기반)

  GET /api/reports/daily?date=2024-08-22
  Authorization: Bearer {jwt-token}

  Response:
  {
    "reportType": "DAILY",
    "totalMessages": 150,
    "startDate": "2024-08-22T00:00:00",
    "endDate": "2024-08-22T23:59:59",
    "userActivity": {
      "user1": 45,
      "user2": 32,
      "user3": 28
    },
    "messageTypeStats": {
      "TEXT": 140,
      "SYSTEM": 10
    },
    "hourlyActivity": {
      "9": 20, "10": 35, "11": 25,
      "14": 50, "15": 20
    },
    "roomActivity": {
      "1": 85, "2": 65
    },
    "topicAnalysis": {
      "topicDistribution": {
        "업무": 45, "기술": 32, "팀워크": 18
      },
      "emotionDistribution": {
        "긍정": 78, "중립": 22, "부정": 15
      }
    },
    "mostActiveHour": "14시",
    "mostActiveHourCount": 50,
    "summary": "📊 일일 보고서\n• 총 메시지 수: 150개\n• 참여 사용자: 3명\n• 가장 활발한 시간: 14시 (50개 메시지)\n•
  가장 활발한 사용자: user1 (45개 메시지)",
    "generatedAt": "2024-08-22T16:35:00"
  }

  9. 주간/월간 보고서

  GET /api/reports/weekly?weekStart=2024-08-19
  GET /api/reports/monthly?year=2024&month=8
  Authorization: Bearer {jwt-token}

  Response: (일일 보고서와 동일한 형태, reportType만 다름)

  10. 사용자별 개인 보고서

  GET /api/reports/user/{username}?start=2024-08-22T00:00:00&end=2024-08-22T23:59:59
  Authorization: Bearer {jwt-token}

  Response:
  {
    "username": "user1",
    "totalMessages": 45,
    "startDate": "2024-08-22T00:00:00",
    "endDate": "2024-08-22T23:59:59",
    "hourlyActivity": {
      "9": 10, "10": 15, "14": 20
    },
    "roomParticipation": {
      "1": 25, "2": 20
    }
  }

  🔹 기존 채팅 관련 API (참고용)

  11. 채팅방 목록

  GET /api/rooms
  Authorization: Bearer {jwt-token}

  Response:
  [
    {
      "id": 1,
      "name": "개발팀 채팅방",
      "description": "개발 관련 논의",
      "participantCount": 8,
      "createdAt": "2024-08-20T10:00:00",
      "lastMessageTime": "2024-08-22T16:30:00",
      "lastMessage": "프로젝트 진행상황 공유드립니다"
    }
  ]

  12. 채팅방 메시지 조회

  GET /api/rooms/{roomId}/messages?page=0&size=50
  Authorization: Bearer {jwt-token}

  Response:
  {
    "content": [
      {
        "id": 123,
        "roomId": 1,
        "userId": 1,
        "username": "user1",
        "name": "김개발",
        "content": "안녕하세요",
        "messageType": "TEXT",
        "timestamp": "2024-08-22T16:30:00",
        "isDeleted": false
      }
    ],
    "page": {
      "size": 50,
      "number": 0,
      "totalElements": 150,
      "totalPages": 3
    }
  }

  🔹 실시간 WebSocket API

  13. 채팅 메시지 전송

  // 메시지 전송
  stompClient.send('/app/send-message', {}, JSON.stringify({
    roomId: 1,
    content: "안녕하세요",
    messageType: "TEXT"
  }));

  // 메시지 수신
  stompClient.subscribe('/topic/room/1', (message) => {
    const newMessage = JSON.parse(message.body);
  });

  14. 타이핑 상태

  // 타이핑 상태 전송
  stompClient.send('/app/typing', {}, JSON.stringify({
    roomId: 1,
    isTyping: true
  }));

  // 타이핑 상태 수신
  stompClient.subscribe('/topic/room/1/typing', (message) => {
    const typingData = JSON.parse(message.body);
  });

  ---
  📝 추가 개발 권장사항

  1. 에러 처리: 모든 API는 표준 HTTP 상태 코드 사용
    - 401: 인증 실패
    - 403: 권한 부족
    - 404: 리소스 없음
    - 500: 서버 오류
  2. 로딩 상태: API 응답 시간이 1-3초 소요될 수 있으니 적절한 로딩 UI 구현
  3. 실시간 업데이트: WebSocket을 통한 실시간 분석 데이터 업데이트 활용
  4. 캐싱: 분석 데이터는 캐시되어 있어 빠른 응답 제공

  이 API 스펙으로 Phase 3의 모든 프론트엔드 기능을 구현할 수 있습니다!