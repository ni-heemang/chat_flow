# Flow Chat 백엔드 서버 실행 방법

Flow Chat 백엔드를 EC2에서 실행하는 여러 방법을 제공합니다.

---

## 🚀 방법 1: ECR + Docker (권장)

**가장 간단하고 실용적인 방법입니다.**

### 사전 준비
- AWS CLI 설치 및 설정
- Docker 설치
- ECR 접근 권한이 있는 IAM 역할 또는 자격증명

### 1.1 로컬에서 ECR에 이미지 업로드
```bash
cd backend
chmod +x deploy-ecr.sh

# ECR에 이미지 업로드
./deploy-ecr.sh ap-northeast-2 123456789012 v1.0
# 파라미터: <region> <account-id> <tag>
```

### 1.2 EC2에서 실행
```bash
# 환경변수 설정
export LLM_API_KEY="your-openai-api-key"
export MYSQL_ROOT_PASSWORD="secure-root-password"
export MYSQL_PASSWORD="secure-user-password"

# 전체 환경 자동 설정
chmod +x ec2-setup.sh
./ec2-setup.sh 037684266277.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:latest
```

### 1.3 수동 Docker 실행 (간단한 방법)
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
docker login --username AWS --password-stdin 037684266277.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 실행
docker run -d \
    --name flowchat-backend \
    --restart unless-stopped \
    -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME="flowchat" \
    -e SPRING_DATASOURCE_PASSWORD="your-password" \
    -e LLM_API_KEY="your-openai-api-key" \
    -e SPRING_PROFILES_ACTIVE="prod" \
    -e CORS_ALLOWED_ORIGINS="http://your-ec2-public-ip,https://your-ec2-public-ip" \
    -e WEBSOCKET_ALLOWED_ORIGINS="http://your-ec2-public-ip,https://your-ec2-public-ip" \
    123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:latest
```

**접속 테스트**: `http://your-ec2-public-ip:8080`

---

## 🐳 방법 2: Docker Compose

### 2.1 환경 변수 설정
```bash
cd backend
cp .env.example .env
# .env 파일에서 실제 값들 설정
```

### 2.2 실행
```bash
docker-compose up -d
```

### 2.3 상태 확인
```bash
docker-compose ps
docker-compose logs -f backend
```

**접속 테스트**: `http://localhost:8080`

---

## ⚙️ 방법 3: 직접 실행 (개발용)

### 3.1 사전 준비
```bash
# Java 17 설치
sudo apt install openjdk-17-jdk -y

# MySQL 설치 및 설정
sudo apt install mysql-server -y
sudo mysql_secure_installation

# 데이터베이스 생성
sudo mysql -u root -p
```

MySQL에서:
```sql
CREATE DATABASE flowchat;
CREATE USER 'flowchat'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON flowchat.* TO 'flowchat'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3.2 애플리케이션 실행
```bash
# 소스 코드 클론
git clone https://github.com/your-repo/flow_chat.git
cd flow_chat/backend

# 환경 변수 설정
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="flowchat"
export SPRING_DATASOURCE_PASSWORD="your-password"
export LLM_API_KEY="your-openai-api-key"

# 빌드 및 실행
chmod +x gradlew
./gradlew build -x test
java -jar build/libs/flowchat-0.0.1-SNAPSHOT.jar
```

**접속 테스트**: `http://localhost:8080`

---

## 🔄 업데이트 배포

### ECR 방식 업데이트
```bash
# 1. 로컬에서 새 이미지 빌드 & 푸시
./deploy-ecr.sh ap-northeast-2 123456789012 v1.1

# 2. EC2에서 업데이트
docker stop flowchat-backend
docker rm flowchat-backend
docker pull 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.1
docker run -d --name flowchat-backend --restart unless-stopped -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME="flowchat" \
    -e SPRING_DATASOURCE_PASSWORD="your-password" \
    -e LLM_API_KEY="your-openai-api-key" \
    123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.1
```

### Docker Compose 업데이트
```bash
git pull origin main
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## 🎯 프론트엔드 배포 (ECR 환경)

ECR로 백엔드를 배포할 때는 프론트엔드 소스가 EC2에 없으므로, 다음 두 가지 방법 중 선택할 수 있습니다.

### 방법 1: Git Clone 후 빌드 (권장)
```bash
# Git 설치 및 소스 클론
sudo apt install -y git nodejs npm nginx
git clone https://github.com/your-repo/flow_chat.git
cd flow_chat/frontend

# 환경변수 설정 (.env 파일)
cat > .env << EOF
VITE_API_BASE_URL=http://$(curl -s http://checkip.amazonaws.com):8080
VITE_WS_BASE_URL=ws://$(curl -s http://checkip.amazonaws.com):8080
EOF

# 프론트엔드 빌드
npm install
npm run build

# Nginx에 배포
sudo cp -r dist/* /var/www/html/
```

### 방법 2: 프론트엔드도 ECR로 배포
프론트엔드용 Dockerfile과 배포 스크립트를 만들어 ECR로 관리할 수도 있습니다.

```bash
# frontend/Dockerfile 생성
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80

# ECR 배포 (backend와 동일한 방식)
./deploy-frontend-ecr.sh ap-northeast-2 123456789012 latest
```

### Nginx 설정 파일 (공통)
```bash
sudo nano /etc/nginx/sites-available/default
```

```nginx
server {
    listen 80 default_server;
    root /var/www/html;
    index index.html;
    
    # React Router를 위한 설정
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 백엔드 API 프록시
    location /api/ {
        proxy_pass http://localhost:8080/;
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
    }
}
```

```bash
sudo systemctl restart nginx
sudo systemctl enable nginx
```

**접속 테스트**: `http://your-ec2-public-ip`

### 완전 자동화 스크립트 (방법 1 기준)
EC2에서 한 번에 프론트엔드를 설정하는 스크립트:

```bash
#!/bin/bash
# frontend-deploy.sh

REPO_URL=${1:-https://github.com/your-repo/flow_chat.git}
EC2_PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)

echo "🚀 프론트엔드 배포 시작"
echo "Public IP: $EC2_PUBLIC_IP"

# 필요한 패키지 설치
sudo apt update
sudo apt install -y git nodejs npm nginx

# 소스 코드 클론
git clone $REPO_URL
cd flow_chat/frontend

# 환경변수 설정
cat > .env << EOF
VITE_API_BASE_URL=http://${EC2_PUBLIC_IP}:8080
VITE_WS_BASE_URL=ws://${EC2_PUBLIC_IP}:8080
EOF

# 빌드 및 배포
npm install
npm run build
sudo cp -r dist/* /var/www/html/

# Nginx 설정
sudo tee /etc/nginx/sites-available/default > /dev/null << EOF
server {
    listen 80 default_server;
    root /var/www/html;
    index index.html;
    
    location / {
        try_files \$uri \$uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header Origin \$http_origin;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# 서비스 시작
sudo systemctl restart nginx
sudo systemctl enable nginx

echo "✅ 프론트엔드 배포 완료!"
echo "접속 URL: http://${EC2_PUBLIC_IP}"
```

**사용법**:
```bash
chmod +x frontend-deploy.sh
./frontend-deploy.sh https://github.com/your-repo/flow_chat.git
```

---

## 🔍 문제 해결

### 로그 확인
```bash
# Docker 컨테이너 로그
docker logs -f flowchat-backend

# 시스템 서비스 로그 (systemd 사용 시)
sudo journalctl -u flowchat-backend -f

# Nginx 로그
sudo tail -f /var/log/nginx/error.log
```

### 서비스 상태 확인
```bash
# Docker 컨테이너 상태
docker ps | grep flowchat

# 포트 사용 상태
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :3306

# MySQL 연결 테스트
mysql -u flowchat -p -h localhost flowchat
```

### 일반적인 문제들

1. **MySQL 연결 실패**
   ```bash
   # MySQL 서비스 상태 확인
   sudo systemctl status mysql
   
   # MySQL 재시작
   sudo systemctl restart mysql
   ```

2. **포트 충돌**
   ```bash
   # 8080 포트 사용 중인 프로세스 확인
   sudo lsof -i :8080
   
   # 프로세스 종료
   sudo kill -9 <PID>
   ```

3. **메모리 부족**
   ```bash
   # 메모리 사용량 확인
   free -h
   
   # Docker 컨테이너 리소스 제한
   docker run --memory=512m --cpus=0.5 ...
   ```

---

## 💡 권장사항

- **운영환경**: ECR + Docker 방식 사용
- **개발환경**: Docker Compose 또는 직접 실행
- **CI/CD**: GitHub Actions와 ECR 연동
- **모니터링**: CloudWatch 또는 Grafana 설정
- **백업**: 정기적인 MySQL 백업 설정

이 가이드를 따라하면 Flow Chat 백엔드를 안정적으로 운영할 수 있습니다.