# Flow Chat Frontend

## 개요
실시간 채팅 시스템의 프론트엔드 애플리케이션입니다. React와 Vite를 기반으로 구축되었으며, WebSocket을 통한 실시간 통신과 AI 기반 채팅 분석 기능을 제공합니다.

## 주요 기능
- 실시간 채팅 시스템 (WebSocket/STOMP)
- 채팅방 생성 및 관리
- AI 기반 메시지 분석 및 인사이트
- 사용자 인증 및 권한 관리

## 기술 스택
- **프레임워크**: React 18
- **빌드 도구**: Vite
- **UI 라이브러리**: Material-UI (MUI)
- **상태 관리**: Zustand
- **HTTP 클라이언트**: Axios
- **WebSocket**: STOMP.js, SockJS
- **라우팅**: React Router
- **스타일링**: CSS-in-JS, Emotion

## 개발 환경 설정

### 사전 요구사항
- Node.js 16.x 이상
- npm 또는 yarn

### 설치 및 실행
```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build

# 빌드 미리보기
npm run preview
```

## 배포 방법

### 프로덕션 빌드
```bash
npm run build
```

### Nginx 배포
1. 프로젝트 빌드
```bash
npm run build
```

2. EC2 Nginx 설정 파일 생성 (/etc/nginx/sites-available/flowchat)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/html;
    index index.html;

    # SPA 라우팅 지원
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 프록시
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 프록시
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header Origin $http_origin;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

3. 빌드 파일 배포
```bash
sudo cp -r dist/* /var/www/html/
sudo nginx -t
sudo systemctl reload nginx
```

## 프로젝트 구조
```
src/
├── components/          # 재사용 가능한 컴포넌트
│   ├── chat/           # 채팅 관련 컴포넌트
│   ├── analysis/       # 분석 관련 컴포넌트
│   └── common/         # 공통 컴포넌트
├── pages/              # 페이지 컴포넌트
├── services/           # API 서비스
├── hooks/              # 커스텀 훅
├── store/              # 상태 관리
├── utils/              # 유틸리티 함수
└── styles/             # 스타일 파일
```

## API 통신
- 개발 환경: `http://localhost:8080/api`
- 프로덕션 환경: 현재 도메인의 `/api` 경로로 자동 설정

## WebSocket 연결
- 개발 환경: `http://localhost:8080/ws`
- 프로덕션 환경: 현재 도메인의 `/ws` 경로로 자동 설정

## 개발 가이드라인
- 컴포넌트는 기능별로 분리하여 작성
- 상태 관리는 Zustand를 사용
- API 호출은 services 디렉토리의 모듈을 통해 수행
- WebSocket 통신은 useSocket 훅을 사용