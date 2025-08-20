#!/bin/bash

set -e

ENV_FILE="./deploy.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ $ENV_FILE 파일이 없습니다."
  exit 1
fi

source "$ENV_FILE"

BACKEND_REMOTE_PATH="/var/www/qms/backend"
FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

# 병렬 빌드를 위한 함수들
build_backend() {
    echo "🔨 [백엔드] 빌드 시작..."
    cd backend || { echo "❌ backend 디렉토리가 없습니다."; exit 1; }
    ./gradlew build -x test --parallel --build-cache || { echo "❌ 백엔드 빌드 실패"; exit 1; }
    cd ..
    echo "✅ [백엔드] 빌드 완료"
}

build_frontend() {
    echo "🔨 [프론트엔드] 빌드 시작..."
    cd frontend || { echo "❌ frontend 디렉토리가 없습니다."; exit 1; }

    if [ -f "node_modules/.cache-timestamp" ] && [ -f "package-lock.json" ]; then
        LOCK_HASH=$(md5sum package-lock.json | cut -d' ' -f1)
        CACHED_HASH=$(cat node_modules/.cache-timestamp 2>/dev/null || echo "")

        if [ "$LOCK_HASH" != "$CACHED_HASH" ]; then
            echo "📦 의존성 변경 감지, npm install 실행..."
            npm ci --prefer-offline --no-audit
            echo "$LOCK_HASH" > node_modules/.cache-timestamp
        else
            echo "♻️  캐시된 node_modules 사용"
        fi
    else
        echo "📦 첫 설치, npm install 실행..."
        npm ci --prefer-offline --no-audit
        md5sum package-lock.json | cut -d' ' -f1 > node_modules/.cache-timestamp
    fi

    npm run build || { echo "❌ 프론트엔드 빌드 실패"; exit 1; }
    cd ..
    echo "✅ [프론트엔드] 빌드 완료"
}

# 헬스체크 함수
health_check() {
    local port=$1
    local max_attempts=30
    local attempt=1

    echo "🏥 포트 $port 헬스체크 시작..."

    while [ $attempt -le $max_attempts ]; do
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "curl -f http://localhost:$port/actuator/health > /dev/null 2>&1"; then
            echo "✅ 포트 $port 서비스 준비 완료 (시도: $attempt/$max_attempts)"
            return 0
        fi

        echo "⏳ 포트 $port 대기 중... ($attempt/$max_attempts)"
        sleep 3
        attempt=$((attempt + 1))
    done

    echo "❌ 포트 $port 헬스체크 실패"
    return 1
}

echo "==== [1/8] 병렬 빌드 시작 🚀 ===="
build_backend &
BACKEND_PID=$!

build_frontend &
FRONTEND_PID=$!

wait $BACKEND_PID
wait $FRONTEND_PID

echo "==== [2/8] 백엔드 배포 📤 ===="
rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/

echo "==== [3/8] 프론트엔드 배포 📤 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*"

rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/

echo "==== [4/8] 권한 및 디렉토리 설정 📁 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME"

echo "==== [5/8] 무중단 배포 - 서버1 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "echo '🛑 서버1 (8080) 재시작 중...'
   sudo systemctl restart qms-server1
   sleep 5"

# 서버1 헬스체크
health_check 8080 || exit 1

echo "==== [6/8] 무중단 배포 - 서버2 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "echo '🛑 서버2 (8081) 재시작 중...'
   sudo systemctl restart qms-server2
   sleep 5"

# 서버2 헬스체크
health_check 8081 || exit 1

echo "==== [7/8] Nginx 리로드 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo nginx -t && sudo nginx -s reload"

echo "==== [8/8] 최종 상태 확인 🔍 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'echo "📊 서버 상태:"
   echo "- 8080: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "실패")"
   echo "- 8081: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "실패")"
   echo "- 외부: $(curl -s -o /dev/null -w "%{http_code}" https://qms.jaemin.app/actuator/health 2>/dev/null || echo "실패")"
   echo ""
   echo "📈 서비스 상태:"
   sudo systemctl status qms-server1 --no-pager -l || echo "서버1 상태 확인 실패"
   sudo systemctl status qms-server2 --no-pager -l || echo "서버2 상태 확인 실패"'

echo "🎉 무중단 배포 완료!"