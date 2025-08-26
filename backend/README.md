# Flow Chat Backend - Docker Compose 배포

## 개요
Docker Compose를 사용하여 Spring Boot 백엔드와 MySQL 데이터베이스를 함께 실행합니다.

## 사전 요구사항
- Docker
- Docker Compose

## 설정

### 1. 환경 변수 파일 생성
`.env.example` 파일을 `.env`로 복사하고 필요한 값들을 설정하세요:

```bash
cp .env.example .env
```

`.env` 파일 내용:
```env
# Database Configuration
DB_URL=jdbc:mysql://mysql:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=flowchat
DB_PASSWORD=your_secure_password_here
DB_ROOT_PASSWORD=your_root_password_here

# LLM Configuration
LLM_API_KEY=your-openai-api-key-here

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### 2. 실행

#### 개발 환경에서 실행
```bash
docker-compose up -d
```

#### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 백엔드만
docker-compose logs -f backend

# MySQL만
docker-compose logs -f mysql
```

#### 서비스 상태 확인
```bash
docker-compose ps
```

### 3. 접속 정보
- **백엔드 API**: http://localhost:8080
- **MySQL**: localhost:3306
- **Health Check**: http://localhost:8080/actuator/health

### 4. 중지 및 정리
```bash
# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터 완전 삭제)
docker-compose down -v
```

## 주요 특징

1. **환경 변수 주입**: DB 설정과 LLM API 키를 환경 변수로 주입
2. **헬스 체크**: MySQL이 완전히 시작된 후 백엔드가 시작되도록 설정
3. **볼륨 마운트**: MySQL 데이터 영속성 보장
4. **네트워크 격리**: 컨테이너 간 통신을 위한 전용 네트워크

## 운영 환경 배포

운영 환경에서는 다음을 추가로 고려하세요:

1. **보안 강화**: 강력한 비밀번호 사용
2. **SSL/TLS**: HTTPS 설정
3. **백업**: 정기적인 데이터베이스 백업
4. **모니터링**: 로그 및 메트릭 수집
5. **리소스 제한**: 컨테이너 리소스 제한 설정

```yaml
# 운영 환경용 추가 설정 예시
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
        reservations:
          memory: 256M
          cpus: "0.25"
```