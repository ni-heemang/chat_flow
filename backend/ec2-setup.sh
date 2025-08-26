#!/bin/bash

# EC2 인스턴스에서 Flow Chat 실행 스크립트
# 사용법: ./ec2-setup.sh <ecr-image-uri>

set -e

ECR_IMAGE_URI=${1}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-flowchat2024!}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-flowchatuser2024!}
LLM_API_KEY=${LLM_API_KEY}

if [ -z "$ECR_IMAGE_URI" ]; then
    echo "ECR 이미지 URI를 입력해주세요."
    echo "사용법: $0 <ecr-image-uri>"
    echo "예시: $0 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/flowchat-backend:latest"
    exit 1
fi

if [ -z "$LLM_API_KEY" ]; then
    echo "⚠️  LLM_API_KEY 환경변수를 설정해주세요."
    echo "export LLM_API_KEY='your-openai-api-key'"
    exit 1
fi

echo "🚀 Flow Chat EC2 설정 시작"
echo "ECR 이미지: $ECR_IMAGE_URI"

# 1. 시스템 업데이트
echo "📋 시스템 업데이트..."
sudo apt update && sudo apt upgrade -y

# 2. Docker 설치
echo "🐳 Docker 설치..."
if ! command -v docker &> /dev/null; then
    sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io
    sudo usermod -aG docker $USER
    echo "Docker 설치 완료. 로그아웃 후 다시 로그인하세요."
fi

# 3. AWS CLI 설치
echo "☁️  AWS CLI 설치..."
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    sudo apt install -y unzip
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
fi

# 4. MySQL 설치 및 설정
echo "🗄️  MySQL 설치 및 설정..."
if ! command -v mysql &> /dev/null; then
    sudo apt install -y mysql-server
    sudo systemctl start mysql
    sudo systemctl enable mysql
    
    # MySQL 초기 설정
    sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';"
    sudo mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS flowchat;"
    sudo mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "CREATE USER IF NOT EXISTS 'flowchat'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';"
    sudo mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "GRANT ALL PRIVILEGES ON flowchat.* TO 'flowchat'@'localhost';"
    sudo mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "FLUSH PRIVILEGES;"
fi

# 5. ECR 로그인 (IAM 역할 또는 자격증명 필요)
echo "🔐 ECR 로그인..."
REGION=$(echo $ECR_IMAGE_URI | cut -d'.' -f4)
aws ecr get-login-password --region $REGION | sudo docker login --username AWS --password-stdin $(echo $ECR_IMAGE_URI | cut -d'/' -f1)

# 6. 이미지 다운로드
echo "📥 Docker 이미지 다운로드..."
sudo docker pull $ECR_IMAGE_URI

# 7. 기존 컨테이너 정리
echo "🧹 기존 컨테이너 정리..."
sudo docker stop flowchat-backend 2>/dev/null || true
sudo docker rm flowchat-backend 2>/dev/null || true

# 8. 컨테이너 실행
echo "🚀 Flow Chat Backend 컨테이너 실행..."
sudo docker run -d \
    --name flowchat-backend \
    --restart unless-stopped \
    -p 8080:8080 \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://$(hostname -I | awk '{print $1}'):3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME="flowchat" \
    -e SPRING_DATASOURCE_PASSWORD="${MYSQL_PASSWORD}" \
    -e LLM_API_KEY="${LLM_API_KEY}" \
    -e SPRING_PROFILES_ACTIVE="prod" \
    -e CORS_ALLOWED_ORIGINS="http://$(curl -s http://checkip.amazonaws.com),https://$(curl -s http://checkip.amazonaws.com)" \
    -e WEBSOCKET_ALLOWED_ORIGINS="http://$(curl -s http://checkip.amazonaws.com),https://$(curl -s http://checkip.amazonaws.com)" \
    --network host \
    $ECR_IMAGE_URI

# 9. 서비스 상태 확인
echo "🔍 서비스 상태 확인..."
sleep 10
sudo docker ps | grep flowchat-backend
echo ""
echo "✅ 설정 완료!"
echo "백엔드 API: http://$(curl -s http://checkip.amazonaws.com):8080"
echo "Health Check: http://$(curl -s http://checkip.amazonaws.com):8080/actuator/health"
echo ""
echo "로그 확인: docker logs -f flowchat-backend"
echo "컨테이너 상태: docker ps"