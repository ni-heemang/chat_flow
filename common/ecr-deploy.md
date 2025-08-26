# Flow Chat ECR + EC2 간단 배포 가이드

ECR에 이미지를 업로드하고 EC2에서 간단하게 실행하는 방법입니다.

## 1. 사전 준비

### 1.1 AWS CLI 설정
```bash
aws configure
# Access Key ID, Secret Access Key, Region 입력
```

### 1.2 필요한 권한
- ECR: `AmazonEC2ContainerRegistryFullAccess`
- EC2: ECR 접근을 위한 IAM 역할 또는 자격증명

## 2. 로컬에서 ECR에 이미지 업로드

### 2.1 스크립트 실행 권한 부여
```bash
cd backend
chmod +x deploy-ecr.sh
```

### 2.2 ECR 배포 실행
```bash
./deploy-ecr.sh ap-northeast-2 123456789012 v1.0

# 파라미터 설명:
# ap-northeast-2: AWS 리전
# 123456789012: AWS 계정 ID
# v1.0: 이미지 태그 (선택사항, 기본값: latest)
```

배포 완료 후 ECR URI가 출력됩니다:
```
123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.0
```

## 3. EC2 인스턴스 설정

### 3.1 EC2 인스턴스 생성
- **AMI**: Ubuntu 22.04 LTS
- **Instance Type**: t3.medium 이상
- **IAM 역할**: ECR 접근 권한 포함
- **보안 그룹**: 
  - SSH (22): My IP
  - HTTP (8080): Anywhere
  - MySQL (3306): Custom (내부 통신용)

### 3.2 EC2에 SSH 접속
```bash
ssh -i your-key.pem ubuntu@your-ec2-public-ip
```

### 3.3 환경 변수 설정
```bash
export LLM_API_KEY="your-openai-api-key"
export MYSQL_ROOT_PASSWORD="secure-root-password"
export MYSQL_PASSWORD="secure-user-password"
```

### 3.4 설정 스크립트 실행
```bash
# 스크립트 다운로드
wget https://raw.githubusercontent.com/your-repo/flow_chat/main/backend/ec2-setup.sh
chmod +x ec2-setup.sh

# 실행
./ec2-setup.sh 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.0
```

## 4. 간단한 수동 설정 (스크립트 없이)

### 4.1 Docker 설치
```bash
sudo apt update
sudo apt install -y docker.io
sudo usermod -aG docker $USER
# 로그아웃 후 재로그인
```

### 4.2 MySQL 설치
```bash
sudo apt install -y mysql-server
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

### 4.3 AWS CLI 설치 및 설정
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

### 4.4 ECR 이미지 실행
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 다운로드
docker pull 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.0

# 컨테이너 실행
docker run -d \
    --name flowchat-backend \
    --restart unless-stopped \
    -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME="flowchat" \
    -e SPRING_DATASOURCE_PASSWORD="your-password" \
    -e LLM_API_KEY="your-openai-api-key" \
    -e SPRING_PROFILES_ACTIVE="prod" \
    123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.0
```

## 5. 프론트엔드 배포 (Nginx)

### 5.1 Nginx 설치
```bash
sudo apt install -y nginx nodejs npm
```

### 5.2 프론트엔드 빌드
```bash
cd /tmp
git clone https://github.com/your-repo/flow_chat.git
cd flow_chat/frontend

# API 엔드포인트 수정 (EC2 공용 IP로)
find src -name "*.js" -exec sed -i 's/localhost:8080/your-ec2-public-ip:8080/g' {} \;

npm install
npm run build
sudo cp -r dist/* /var/www/html/
```

### 5.3 Nginx 설정
```bash
sudo nano /etc/nginx/sites-available/default
```

```nginx
server {
    listen 80 default_server;
    root /var/www/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
sudo systemctl restart nginx
```

## 6. 업데이트 배포

새 버전 배포 시:

```bash
# 1. 로컬에서 새 이미지 빌드 & 푸시
./deploy-ecr.sh ap-northeast-2 123456789012 v1.1

# 2. EC2에서 업데이트
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
docker pull 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.1
docker stop flowchat-backend
docker rm flowchat-backend
docker run -d --name flowchat-backend --restart unless-stopped -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME="flowchat" \
    -e SPRING_DATASOURCE_PASSWORD="your-password" \
    -e LLM_API_KEY="your-openai-api-key" \
    123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:v1.1
```

## 7. 장점

✅ **간단함**: 스크립트 하나로 전체 설정 완료  
✅ **버전 관리**: ECR에서 이미지 태그로 버전 관리  
✅ **롤백 쉬움**: 이전 버전 이미지로 쉽게 되돌리기 가능  
✅ **보안**: 환경 변수로 민감 정보 관리  
✅ **확장성**: 여러 EC2 인스턴스에서 동일 이미지 실행 가능  

이 방법으로 매우 간단하게 배포하고 운영할 수 있습니다!