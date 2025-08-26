# Flow Chat Backend

## 개요
실시간 채팅 시스템의 백엔드 API 서버입니다. Spring Boot를 기반으로 구축되었으며, WebSocket을 통한 실시간 통신, JWT 인증, AI 기반 채팅 분석 기능을 제공합니다.

## 주요 기능
- REST API 서버 (Spring Boot)
- 실시간 채팅 (WebSocket/STOMP)
- JWT 기반 사용자 인증 및 권한 관리
- AI 기반 메시지 분석 및 인사이트
- 채팅방 생성 및 관리
- 사용자 프로필 관리

## 기술 스택
- **프레임워크**: Spring Boot 3.2
- **언어**: Java 17
- **데이터베이스**: MySQL 8.0
- **ORM**: Spring Data JPA, Hibernate
- **인증**: Spring Security, JWT
- **WebSocket**: STOMP
- **API 문서**: OpenAPI 3.0 (Swagger)
- **빌드 도구**: Gradle
- **AI API**: OpenAI GPT

## 개발 환경 설정

### 사전 요구사항
- Java 17 이상
- MySQL 8.0
- Gradle 7.6 이상

### 데이터베이스 설정
```sql
CREATE DATABASE flowchat_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'flowchat'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON flowchat_dev.* TO 'flowchat'@'localhost';
FLUSH PRIVILEGES;
```

### 환경 변수 설정
`src/main/resources/application-{env}.yml` 파일을 생성하고 설정:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/flowchat_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: flowchat
    password: your_password
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

llm:
  api:
    key: your_openai_api_key_here

logging:
  level:
    com.flowchat: DEBUG
```

### 실행
```bash
# 개발 환경 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build
```

## API 문서
서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 배포 방법

### Docker 배포
1. 애플리케이션 빌드
```bash
./gradlew build
```

2. Docker 이미지 생성
```bash
docker build -t flowchat-backend .
```

3. Docker 컨테이너 실행
```bash
docker run -d \
  --name flowchat-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  -e "SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  -e "SPRING_DATASOURCE_USERNAME=flowchat" \
  -e "SPRING_DATASOURCE_PASSWORD=your_password" \
  -e "LLM_API_KEY=your_openai_api_key" \
  -e "SPRING_PROFILES_ACTIVE=prod" \
  flowchat-backend:latest
```

### AWS ECR 배포
1. ECR 이미지 빌드 및 푸시 스크립트 실행
```bash
chmod +X deploy-ecr.sh
./deploy-ecr.sh ap-northeast-2 {account_id} latest
```

2. EC2에서 환경변수 설정
```bash
export SPRING_DATASOURCE_PASSWORD=
export LLM_API_KEY=
```

3. EC2에서 실행
```bash
docker pull {your-account-id}.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:latest
docker run -d \
  --name flowchat-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  -e "SPRING_DATASOURCE_URL=jdbc:mysql://{DB_HOST}:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  -e "SPRING_DATASOURCE_USERNAME=flowchat" \
  -e "SPRING_DATASOURCE_PASSWORD=your_secure_password" \
  -e "LLM_API_KEY=your_openai_api_key" \
  -e "SPRING_PROFILES_ACTIVE=prod" \
  {your-account-id}.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:latest
```

## 프로젝트 구조
```
src/main/java/com/flowchat/
├── config/             # 설정 클래스 (Security, WebSocket 등)
├── controller/         # REST 컨트롤러
├── service/            # 비즈니스 로직
├── repository/         # 데이터 접근 계층
├── entity/             # JPA 엔티티
├── dto/               # 데이터 전송 객체
├── handler/           # WebSocket 이벤트 핸들러
├── scheduler/         # 스케줄러
└── FlowChatApplication.java
```

## 주요 엔드포인트
- **인증**: `/api/users/login`, `/api/users/register`
- **채팅방**: `/api/chatrooms`
- **메시지**: `/api/messages`
- **분석**: `/api/analysis`
- **WebSocket**: `/ws`
- **헬스체크**: `/actuator/health`

## 환경별 프로파일
- **개발**: `application-dev.yml`
- **테스트**: `application-test.yml`
- **운영**: 환경 변수로 설정 주입

## 보안 설정
- JWT 기반 인증
- CORS 설정
- Spring Security 적용
- 민감 정보는 환경 변수로 관리

## 모니터링
- Spring Boot Actuator 적용
- 헬스체크 엔드포인트 제공
- 로그 레벨 설정 가능

## 개발 가이드라인
- Controller-Service-Repository 패턴 사용
- DTO를 통한 데이터 전송
- 예외 처리는 전역 예외 핸들러 활용
- API 문서는 OpenAPI 3.0 스펙 준수