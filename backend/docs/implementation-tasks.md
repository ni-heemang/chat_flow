# 실시간 채팅 + 분석 백엔드 구현 태스크 목록

## Phase 1: 실시간 채팅 시스템 구현 (필수)

### 1.1 프로젝트 설정 및 초기화
- [x] **Spring Boot 프로젝트 생성**: Gradle 기반 Spring Boot 3.x 프로젝트 초기화
- [x] **의존성 설정**: build.gradle에 필수 의존성 추가
  - Spring Boot Starter Web, JPA, WebSocket, Security, Validation
  - MySQL Connector, JWT, SpringDoc OpenAPI
- [x] **프로젝트 구조 생성**: com.flowchat 패키지 하위 폴더 구조 생성
  - config, controller, handler, service, entity, repository, dto, event
- [x] **application.yml 설정**: 데이터베이스, 서버 포트, CORS 등 기본 설정

### 1.2 데이터베이스 설계 및 엔티티 구현
- [x] **MySQL 데이터베이스 생성**: flowchat 데이터베이스 생성
- [x] **User 엔티티**: 사용자 정보 관리
  ```java
  @Entity
  public class User {
      private Long id;
      private String username;
      private String password;
      private String nickname;
      private LocalDateTime createdAt;
      private LocalDateTime lastLoginAt;
      private Boolean isActive;
  }
  ```
- [x] **ChatRoom 엔티티**: 채팅방 정보 관리
  ```java
  @Entity
  public class ChatRoom {
      private Long id;
      private String name;
      private String description;
      private Integer maxParticipants;
      private Integer currentParticipants;
      private Long createdBy;
      private LocalDateTime createdAt;
      private Boolean isActive;
      private Boolean isPublic;
  }
  ```
- [x] **ChatMessage 엔티티**: 채팅 메시지 저장
  ```java
  @Entity
  public class ChatMessage {
      private Long id;
      private Long roomId;
      private Long userId;
      private String content;
      private LocalDateTime timestamp;
      private MessageType messageType;
      private Boolean isDeleted;
      private LocalDateTime editedAt;
  }
  ```
- [x] **AnalysisResult 엔티티**: 분석 결과 저장
- [x] **Repository 인터페이스**: 각 엔티티에 대한 JPA Repository 생성

### 1.3 사용자 인증 및 보안 시스템
- [x] **JWT 설정**: JwtConfig.java 구현
  - JWT 토큰 생성/검증 유틸리티
  - 토큰 만료시간, 시크릿 키 설정
  - 리프레시 토큰 지원
- [x] **Spring Security 설정**: SecurityConfig.java 구현
  - JWT 기반 인증 필터 체인
  - WebSocket 보안 설정
  - CORS 설정
  - JWT 인증 필터 및 예외 처리
- [x] **사용자 관리 API**: UserController.java 구현
  ```java
  @RestController
  @RequestMapping("/api/users")
  public class UserController {
      @PostMapping("/register")      // 사용자 등록
      @PostMapping("/login")         // 로그인
      @PostMapping("/refresh")       // 토큰 갱신
      @GetMapping("/profile")        // 프로필 조회
      @PutMapping("/profile")        // 프로필 업데이트
      @PutMapping("/password")       // 비밀번호 변경
  }
  ```
- [x] **UserService**: 사용자 등록, 로그인, JWT 토큰 발급 로직
- [x] **DTO 클래스**: UserRequest, UserResponse, LoginRequest, PasswordChangeRequest 등

### 1.4 실시간 WebSocket 통신 구현
- [x] **WebSocket 설정**: WebSocketConfig.java 구현
  ```java
  @Configuration
  @EnableWebSocketMessageBroker
  public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
      // STOMP 브로커 설정
      // 엔드포인트 매핑: /ws
      // 애플리케이션 대상: /app
      // 브로커 대상: /topic, /queue
  }
  ```
- [x] **WebSocket 이벤트 핸들러**: WebSocketEventHandler.java 구현
  - 연결/해제 이벤트 처리
  - 사용자 세션 관리
  - JWT 토큰 기반 인증 처리
- [x] **메시지 컨트롤러**: MessageController.java 구현
  ```java
  @Controller
  public class MessageController {
      @MessageMapping("/send-message")    // 메시지 전송
      @MessageMapping("/join-room")       // 채팅방 입장
      @MessageMapping("/leave-room")      // 채팅방 퇴장
  }
  ```
- [x] **실시간 메시지 처리**: MessageService.java 구현
  - 메시지 저장 및 브로드캐스트
  - 채팅방 사용자 관리

### 1.5 채팅방 관리 시스템
- [x] **채팅방 REST API**: ChatRoomController.java 구현
  ```java
  @RestController
  @RequestMapping("/api/chatrooms")
  public class ChatRoomController {
      @GetMapping("")               // 채팅방 목록 조회
      @PostMapping("")              // 채팅방 생성
      @PostMapping("/{roomId}/join") // 채팅방 입장
      @PostMapping("/{roomId}/leave") // 채팅방 퇴장
      @PutMapping("/{roomId}")      // 채팅방 수정
      @DeleteMapping("/{roomId}")   // 채팅방 삭제
      @GetMapping("/my")            // 내 채팅방 목록
      @GetMapping("/stats")         // 채팅방 통계
  }
  ```
- [x] **ChatRoomService**: 채팅방 비즈니스 로직
  - 채팅방 생성/조회/입장/퇴장
  - 참여자 수 관리
  - 권한 검증
  - 검색 및 필터링 기능
  - 통계 조회 기능
- [x] **DTO 클래스**: ChatRoomRequest, ChatRoomResponse 등

## Phase 2: 실시간 분석 기능 구현 (핵심)

### 2.1 채팅 메시지 실시간 분석 시스템
- [x] **이벤트 시스템 구현**: Spring Event 기반 비동기 처리
  ```java
  public class MessageReceivedEvent {
      private ChatMessage message;
      private Long roomId;
  }
  ```
- [x] **ChatAnalysisService**: 실시간 채팅 분석 서비스
  ```java
  @Service
  public class ChatAnalysisService {
      @EventListener
      @Async
      public void analyzeMessage(MessageReceivedEvent event) {
          // 키워드 추출
          // 감정 분석
          // 통계 업데이트
      }
  }
  ```
- [x] **키워드 추출 로직**: Java 기본 API 활용
  - 불용어 필터링 (조사, 접속사 등 제거)
  - 단어 빈도 계산 (HashMap 활용)
  - 상위 10개 키워드 추출
- [x] **시간대별 분석**: LocalDateTime 기반 시간 패턴 분석
- [x] **참여도 분석**: 사용자별 메시지 수, 활동 시간 분석

### 2.2 분석 결과 실시간 전송
- [x] **분석 결과 WebSocket 전송**: SimpMessagingTemplate 활용
  ```java
  @Service
  public class AnalysisNotificationService {
      public void sendAnalysisUpdate(Long roomId, AnalysisData data) {
          messagingTemplate.convertAndSend("/topic/analysis/" + roomId, data);
      }
  }
  ```
- [x] **분석 데이터 DTO**: Chart.js 형식에 맞는 데이터 구조
- [x] **실시간 업데이트 주기**: 10초마다 또는 새 메시지 10개마다

### 2.3 분석 결과 조회 API
- [x] **분석 컨트롤러**: AnalysisController.java 구현
  ```java
  @RestController
  @RequestMapping("/api/analysis")
  public class AnalysisController {
      @GetMapping("/{roomId}")  // 분석 결과 조회
  }
  ```
- [x] **분석 결과 저장**: AnalysisResult 엔티티에 주기적 저장
- [x] **캐싱 구현**: @Cacheable 어노테이션으로 성능 최적화

## Phase 3: 고급 분석 및 리포트 기능 (개선)

### 3.1 심화 분석 기능
- [x] **주제별 분류**: 간단한 규칙 기반 분류
  - 업무 관련 키워드 분류
  - 일상 대화 분류
  - 문제/이슈 관련 분류
- [x] **감정 분석**: 긍정/부정/중립 분류
- [x] **대화 흐름 분석**: 메시지 간격, 대화 지속 시간 분석

### 3.2 보고서 생성 시스템
- [x] **ReportService**: 일/주/월별 보고서 생성
- [x] **Scheduled Tasks**: @Scheduled로 주기적 보고서 생성
  ```java
  @Component
  public class ReportScheduler {
      @Scheduled(cron = "0 0 0 * * ?")  // 매일 자정
      public void generateDailyReport() {
          // 일일 보고서 생성
      }
  }
  ```
- [ ] **PDF 생성**: iText 라이브러리 활용 (선택사항)

## 개발 환경 및 도구 설정

### 개발 도구 설정
- [ ] **IDE 설정**: IntelliJ IDEA 프로젝트 설정 (Gradle 기반)
- [ ] **Gradle Wrapper 설정**: gradlew 실행 권한 설정 및 버전 확인
- [ ] **MySQL 연결**: MySQL Workbench 또는 DBeaver로 DB 관리
- [ ] **API 문서화**: SpringDoc으로 Swagger UI 자동 생성
  - http://localhost:8080/swagger-ui.html
- [ ] **로깅 설정**: Logback 설정으로 적절한 로그 레벨 구성

## 각 Phase별 완료 기준

### Phase 1 완료 기준
- 사용자가 회원가입/로그인할 수 있음
- 채팅방을 생성하고 입장할 수 있음
- 실시간으로 메시지를 주고받을 수 있음
- 메시지가 데이터베이스에 저장됨
- JWT 인증이 정상 작동함

### Phase 2 완료 기준
- 채팅 메시지가 실시간으로 분석됨
- 키워드, 시간 패턴, 참여도 분석이 작동함
- 분석 결과가 WebSocket으로 프론트엔드에 전송됨
- REST API로 분석 결과 조회 가능

### Phase 3 완료 기준
- 심화 분석 기능 (주제별 분류, 감정 분석) 작동
- 주기적 보고서 생성 기능
- 성능 최적화 완료

### Gradle 실행 명령어
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 의존성 확인
./gradlew dependencies

# 프로젝트 정리
./gradlew clean

# 빌드 없이 실행 (개발용)
./gradlew bootRun --args='--spring.profiles.active=dev'

# JAR 파일 생성 후 실행
./gradlew bootJar
java -jar build/libs/flowchat-0.0.1-SNAPSHOT.jar
```

## 예상 개발 기간
- **Phase 1**: 3-4주 (기본 채팅 시스템)
- **Phase 2**: 2-3주 (실시간 분석 기능)
- **Phase 3**: 1-2주 (고급 분석 및 최적화)
- **총 예상 기간**: 6-9주

## 주요 기술적 고려사항

### 1. WebSocket 세션 관리
```java
@Component
public class WebSocketSessionManager {
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    
    public void addUserToRoom(String roomId, String sessionId) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                   .add(sessionId);
    }
}
```

### 2. 실시간 분석 성능 최적화
- 분석 작업을 별도 스레드풀에서 비동기 처리
- 일정 개수의 메시지가 쌓이면 배치로 분석
- 메모리 기반 임시 저장 후 주기적 DB 저장

### 3. 에러 처리 및 로깅
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFound(
        ChatRoomNotFoundException e) {
        // 표준화된 에러 응답 반환
    }
}
```

### 4. 보안 고려사항
- SQL Injection 방지: @Query 파라미터 바인딩
- XSS 방지: 메시지 내용 HTML 이스케이프
- WebSocket 인증: JWT 토큰 검증
- 권한 체크: 채팅방 입장 권한 확인

## 배포 준비
- [ ] **Docker 설정**: Dockerfile 및 docker-compose.yml 작성
- [ ] **Gradle Docker 플러그인**: 컨테이너 이미지 빌드 자동화
- [ ] **프로파일 설정**: dev, prod 환경별 설정 분리
- [ ] **헬스체크**: Actuator 엔드포인트 설정
- [ ] **로그 수집**: 운영 환경 로그 설정

### Docker 빌드 설정
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/flowchat-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Docker 빌드 명령어
./gradlew bootJar
docker build -t flowchat-backend .
docker run -p 8080:8080 flowchat-backend
```

이 태스크 목록을 통해 체계적으로 실시간 채팅 + 분석 백엔드 시스템을 구현할 수 있습니다.