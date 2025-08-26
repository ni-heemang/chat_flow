#!/bin/bash

# Flow Chat Backend - ECR 배포 스크립트
# 사용법: ./deploy-ecr.sh [region] [account-id]

set -e

# 기본값 설정
REGION=${1:-ap-northeast-2}
ACCOUNT_ID=${2}
REPOSITORY_NAME="flowchat-backend"
IMAGE_TAG=${3:-latest}

if [ -z "$ACCOUNT_ID" ]; then
    echo "AWS Account ID를 입력해주세요."
    echo "사용법: $0 [region] <account-id> [tag]"
    echo "예시: $0 ap-northeast-2 123456789012 v1.0"
    exit 1
fi

ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
FULL_IMAGE_NAME="${ECR_URI}/${REPOSITORY_NAME}:${IMAGE_TAG}"

echo "🚀 Flow Chat Backend ECR 배포 시작"
echo "Region: $REGION"
echo "Account ID: $ACCOUNT_ID"
echo "Repository: $REPOSITORY_NAME"
echo "Image Tag: $IMAGE_TAG"
echo "Full Image: $FULL_IMAGE_NAME"

# 1. ECR 로그인
echo "📋 ECR 로그인 중..."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR_URI

# 2. ECR 리포지토리 생성 (존재하지 않는 경우)
echo "📦 ECR 리포지토리 확인/생성 중..."
if ! aws ecr describe-repositories --repository-names $REPOSITORY_NAME --region $REGION --output json >/dev/null 2>&1; then
    echo "리포지토리가 존재하지 않습니다. 생성 중..."
    aws ecr create-repository --repository-name $REPOSITORY_NAME --region $REGION --output json > /dev/null
    echo "리포지토리 '$REPOSITORY_NAME' 생성 완료"
else
    echo "리포지토리 '$REPOSITORY_NAME' 확인됨"
fi

# 3. Docker 이미지 빌드 (AMD64 플랫폼)
echo "🔨 Docker 이미지 빌드 중..."
docker build --platform linux/amd64 -t $REPOSITORY_NAME:$IMAGE_TAG .

# 4. 이미지 태그 지정
echo "🏷️  이미지 태깅 중..."
docker tag $REPOSITORY_NAME:$IMAGE_TAG $FULL_IMAGE_NAME

# 5. ECR에 푸시
echo "📤 ECR에 푸시 중..."
docker push $FULL_IMAGE_NAME

echo "✅ 배포 완료!"
echo "이미지 URI: $FULL_IMAGE_NAME"
echo ""
echo "EC2에서 실행하려면:"
echo "docker pull $FULL_IMAGE_NAME"
echo "docker run -d -p 8080:8080 --name flowchat-backend \\"
echo "  -e SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/flowchat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true' \\"
echo "  -e SPRING_DATASOURCE_USERNAME='flowchat' \\"
echo "  -e SPRING_DATASOURCE_PASSWORD='your-password' \\"
echo "  -e LLM_API_KEY='your-api-key' \\"
echo "  $FULL_IMAGE_NAME"