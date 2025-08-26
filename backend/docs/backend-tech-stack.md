# 실시간 채팅 및 분석 백엔드 기술 스택

## 1. 핵심 백엔드 스택

### 1.1 프레임워크 및 언어
- **Spring Boot 3.x**: 메인 백엔드 프레임워크
- **Java 17**: 프로그래밍 언어

### 1.2 데이터베이스
- **MySQL**: 관계형 데이터베이스 (단일 DB 사용으로 간소화)

### 1.3 데이터 액세스
- **Spring Data JPA**: 데이터베이스 연동

### 1.4 실시간 통신
- **Spring WebSocket**: 실시간 양방향 통신
- **STOMP Protocol**: 메시지 브로커 프로토콜
- **SockJS**: WebSocket 폴백 지원

### 1.5 보안
- **Spring Security**: 기본 보안 설정
- **JWT**: 토큰 기반 인증
- **WebSocket Security**: 실시간 통신 보안

### 1.6 API 문서화
- **SpringDoc**: API 문서 자동 생성

## 2. 실시간 채팅 기능 구현

### 2.1 채팅방 관리
- **Room Entity**: 채팅방 정보 저장
- **User Entity**: 사용자 정보 및 세션 관리
- **Message Entity**: 실시간 메시지 저장

### 2.2 실시간 메시지 처리
- **WebSocket Handler**: 연결/해제 처리
- **Message Controller**: STOMP 메시지 라우팅
- **Session Management**: 사용자 세션 추적

### 2.3 메시지 브로커 설정
```java
// WebSocket 설정 예시
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // 메시지 브로커 및 엔드포인트 설정
}
```

## 3. 채팅 분석 기능 구현

### 3.1 실시간 분석 데이터 생성
- **Message Listener**: 실시간 메시지 수신 시 분석 트리거
- **Analysis Service**: 채팅 내용 실시간 분석
- **Event Publishing**: 분석 결과를 프론트엔드로 전송

### 3.2 텍스트 분석
- **Java String API**: 기본 문자열 처리
- **Collections Framework**: 단어 빈도 계산
- **정규표현식**: 텍스트 파싱 및 정리

### 3.3 키워드 추출
```java
// 실시간 키워드 추출 예시
@EventListener
public void analyzeMessage(MessageReceivedEvent event) {
    // 메시지 분석 후 결과를 WebSocket으로 전송
}
```

### 3.4 시간대별 분석
- **LocalDateTime**: 날짜/시간 처리
- **Stream API**: 실시간 데이터 집계
- **Scheduled Tasks**: 주기적 분석 보고서 생성

## 4. 개발 도구

### 4.1 빌드 도구
- **Maven**: 의존성 관리 및 빌드

### 4.2 테스트
- **JUnit 5**: 단위 테스트
- **Spring Boot Test**: 통합 테스트
- **WebSocket Test**: 실시간 통신 테스트

### 4.3 모니터링
- **Spring Boot Actuator**: 기본 모니터링
- **WebSocket Session Monitoring**: 실시간 연결 모니터링

## 5. 의존성 목록

### 5.1 필수 의존성
```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    
    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>0.9.1</version>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- API 문서 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.0.2</version>
    </dependency>
</dependencies>
```

## 6. 프로젝트 구조

```
backend/
├── src/main/java/com/flowchat/
│   ├── FlowChatApplication.java         # 메인 클래스
│   ├── config/
│   │   ├── WebSocketConfig.java         # WebSocket 설정
│   │   ├── SecurityConfig.java          # 보안 설정
│   │   └── JwtConfig.java              # JWT 설정
│   ├── controller/
│   │   ├── ChatRoomController.java      # 채팅방 관리 REST API
│   │   ├── MessageController.java       # WebSocket 메시지 처리
│   │   ├── UserController.java          # 사용자 관리
│   │   └── AnalysisController.java      # 분석 결과 조회 API
│   ├── handler/
│   │   └── WebSocketEventHandler.java   # WebSocket 이벤트 처리
│   ├── service/
│   │   ├── ChatRoomService.java         # 채팅방 서비스
│   │   ├── MessageService.java          # 메시지 처리 서비스
│   │   ├── UserService.java             # 사용자 서비스
│   │   ├── ChatAnalysisService.java     # 실시간 채팅 분석
│   │   └── ReportService.java           # 분석 보고서 생성
│   ├── entity/
│   │   ├── User.java                    # 사용자 엔티티
│   │   ├── ChatRoom.java                # 채팅방 엔티티
│   │   ├── ChatMessage.java             # 채팅 메시지 엔티티
│   │   └── AnalysisResult.java          # 분석 결과 엔티티
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ChatRoomRepository.java
│   │   ├── ChatMessageRepository.java
│   │   └── AnalysisResultRepository.java
│   ├── dto/
│   │   ├── MessageRequest.java          # 메시지 전송 요청
│   │   ├── MessageResponse.java         # 메시지 응답
│   │   ├── ChatRoomRequest.java         # 채팅방 생성 요청
│   │   ├── AnalysisResponse.java        # 분석 결과 응답
│   │   └── UserResponse.java            # 사용자 정보 응답
│   └── event/
│       ├── MessageReceivedEvent.java    # 메시지 수신 이벤트
│       └── AnalysisCompletedEvent.java  # 분석 완료 이벤트
├── src/main/resources/
│   ├── application.yml                  # 설정 파일
│   └── static/                          # 정적 리소스
└── src/test/java/                       # 테스트 코드
```

## 7. 개발 환경 설정

### 7.1 필수 설치
- Java 17
- Maven
- MySQL
- IDE (IntelliJ IDEA 권장)

### 7.2 실행 방법
```bash
# 프로젝트 빌드
mvn clean install

# 애플리케이션 실행
mvn spring-boot:run

# WebSocket 테스트 (브라우저에서)
# ws://localhost:8080/ws 엔드포인트 연결
```

## 8. 구현 우선순위

### Phase 1 - 실시간 채팅 시스템 (필수)
1. **사용자 인증 및 세션 관리**
   - JWT 기반 인증
   - 사용자 등록/로그인

2. **채팅방 기능**
   - 채팅방 생성/입장/퇴장
   - 채팅방 목록 조회

3. **실시간 메시징**
   - WebSocket 연결 관리
   - 실시간 메시지 송수신
   - 메시지 저장

4. **기본 UI 연동**
   - REST API 제공
   - WebSocket 이벤트 처리

### Phase 2 - 실시간 분석 기능 (핵심)
1. **실시간 메시지 분석**
   - 메시지 수신 시 즉시 분석
   - 키워드 추출 및 빈도 분석
   - 감정 분석 (기본)

2. **분석 결과 실시간 전송**
   - WebSocket으로 프론트엔드에 분석 데이터 전송
   - 실시간 차트 데이터 제공

3. **시간대별 분석**
   - 실시간 활동량 분석
   - 주요 키워드 트렌드

### Phase 3 - 고급 분석 및 리포트 (개선)
1. **심화 분석 기능**
   - 주제별 분류
   - 사용자별 참여도 분석
   - 대화 흐름 분석

2. **분석 보고서**
   - 일/주/월별 보고서
   - 시각화 데이터
   - 보고서 내보내기

이 우선순위로 실시간 채팅을 먼저 구축하고, 채팅 데이터를 실시간으로 분석하여 프론트엔드에 전달하는 시스템을 구현할 수 있습니다.

## 9. API 명세서 (프론트엔드 연동용)

### 9.1 REST API 엔드포인트

#### 사용자 관리 API
```http
# 사용자 등록
POST /api/users/register
Content-Type: application/json
{
  "username": "string",
  "password": "string",
  "nickname": "string"
}

# 응답
{
  "id": "long",
  "username": "string",
  "nickname": "string",
  "createdAt": "2024-01-01T00:00:00Z"
}

# 로그인
POST /api/users/login
Content-Type: application/json
{
  "username": "string",
  "password": "string"
}

# 응답
{
  "token": "jwt_token_string",
  "user": {
    "id": "long",
    "username": "string",
    "nickname": "string"
  }
}
```

#### 채팅방 관리 API
```http
# 채팅방 목록 조회
GET /api/chatrooms
Authorization: Bearer {jwt_token}

# 응답
[
  {
    "id": "long",
    "name": "string",
    "description": "string",
    "participantCount": "int",
    "createdAt": "2024-01-01T00:00:00Z"
  }
]

# 채팅방 생성
POST /api/chatrooms
Authorization: Bearer {jwt_token}
Content-Type: application/json
{
  "name": "string",
  "description": "string",
  "maxParticipants": "int"
}

# 응답
{
  "id": "long",
  "name": "string",
  "description": "string",
  "createdBy": "long",
  "createdAt": "2024-01-01T00:00:00Z"
}

# 채팅방 입장
POST /api/chatrooms/{roomId}/join
Authorization: Bearer {jwt_token}

# 응답
{
  "success": "boolean",
  "message": "string"
}
```

#### 분석 데이터 API
```http
# 채팅방 분석 결과 조회
GET /api/analysis/{roomId}
Authorization: Bearer {jwt_token}
Query Parameters:
  - period: "realtime" | "hour" | "day" | "week"
  - type: "keywords" | "time" | "participation" | "all"

# 응답
{
  "roomId": "long",
  "period": "string",
  "generatedAt": "2024-01-01T00:00:00Z",
  "keywords": [
    {
      "word": "string",
      "count": "int",
      "percentage": "double"
    }
  ],
  "timePattern": {
    "labels": ["00:00", "01:00", "02:00"],
    "datasets": [
      {
        "label": "메시지 수",
        "data": [10, 5, 2],
        "backgroundColor": "#2563EB"
      }
    ]
  },
  "participation": [
    {
      "userId": "long",
      "nickname": "string",
      "messageCount": "int",
      "percentage": "double"
    }
  ]
}
```

### 9.2 WebSocket 이벤트 명세

#### 클라이언트 → 서버 이벤트

```javascript
// 채팅방 입장
socket.emit('join-room', {
  roomId: "long",
  userId: "long"
});

// 메시지 전송
socket.emit('send-message', {
  roomId: "long",
  content: "string",
  timestamp: "2024-01-01T00:00:00Z"
});

// 분석 데이터 요청
socket.emit('request-analysis', {
  roomId: "long",
  type: "keywords" | "time" | "participation"
});

// 채팅방 퇴장
socket.emit('leave-room', {
  roomId: "long",
  userId: "long"
});
```

#### 서버 → 클라이언트 이벤트

```javascript
// 새 메시지 수신
socket.on('new-message', {
  id: "long",
  roomId: "long",
  userId: "long",
  nickname: "string",
  content: "string",
  timestamp: "2024-01-01T00:00:00Z"
});

// 사용자 입장/퇴장 알림
socket.on('user-joined', {
  userId: "long",
  nickname: "string",
  roomId: "long"
});

socket.on('user-left', {
  userId: "long",
  nickname: "string",
  roomId: "long"
});

// 실시간 분석 데이터 업데이트
socket.on('analysis-update', {
  roomId: "long",
  type: "keywords" | "time" | "participation",
  data: {
    // 분석 데이터 (REST API와 동일한 구조)
  },
  updatedAt: "2024-01-01T00:00:00Z"
});

// 채팅방 현재 접속자 목록
socket.on('room-users', {
  roomId: "long",
  users: [
    {
      userId: "long",
      nickname: "string",
      joinedAt: "2024-01-01T00:00:00Z"
    }
  ]
});

// 연결 상태 이벤트
socket.on('connect', () => {
  console.log('Connected to server');
});

socket.on('disconnect', () => {
  console.log('Disconnected from server');
});

// 에러 이벤트
socket.on('error', {
  code: "string",
  message: "string",
  timestamp: "2024-01-01T00:00:00Z"
});
```

### 9.3 에러 응답 형식

#### HTTP 에러 응답
```json
{
  "error": {
    "code": "string",
    "message": "string",
    "details": "string",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

#### 주요 에러 코드
- `AUTH_REQUIRED`: 인증이 필요함 (401)
- `INVALID_TOKEN`: 유효하지 않은 JWT 토큰 (401)
- `ROOM_NOT_FOUND`: 채팅방을 찾을 수 없음 (404)
- `ROOM_FULL`: 채팅방이 가득참 (409)
- `PERMISSION_DENIED`: 권한이 없음 (403)
- `VALIDATION_ERROR`: 입력 데이터 검증 실패 (400)

### 9.4 WebSocket 연결 설정

#### 프론트엔드 연결 예시
```javascript
import io from 'socket.io-client';

const socket = io('http://localhost:8080', {
  auth: {
    token: localStorage.getItem('jwt_token')
  },
  transports: ['websocket', 'polling']
});

// 연결 확인
socket.on('connect', () => {
  console.log('Connected:', socket.id);
});
```

#### 백엔드 CORS 설정
```yaml
# application.yml
cors:
  allowed-origins: 
    - http://localhost:5173  # Vite 개발 서버
    - http://localhost:3000  # CRA 개발 서버
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

이 API 명세서를 통해 프론트엔드에서 백엔드와 정확히 연동할 수 있습니다.