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

    # package-lock.json 체크섬으로 캐시 확인
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

echo "==== [1/6] 병렬 빌드 시작 🚀 ===="
build_backend &
BACKEND_PID=$!

build_frontend &
FRONTEND_PID=$!

wait $BACKEND_PID
wait $FRONTEND_PID

echo "==== [2/6] 백엔드 배포 📤 ===="
rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ || {
    echo "❌ 백엔드 전송 실패"; exit 1;
}

echo "==== [2-1/6] 백엔드 JAR 권한 및 소유권 설정 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
  sudo chown ubuntu:ubuntu ${BACKEND_REMOTE_PATH}/$JAR_NAME
  sudo chmod 755 ${BACKEND_REMOTE_PATH}/$JAR_NAME
EOF

echo "==== [3/6] 프론트엔드 배포 전 기존 파일 삭제 🗑️ ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${FRONTEND_REMOTE_PATH}/*" || {
  echo "❌ 기존 프론트엔드 파일 삭제 실패"; exit 1;
}

echo "==== [4/6] 프론트엔드 배포 📤 ===="
rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ || {
    echo "❌ 프론트엔드 전송 실패"; exit 1;
}

echo "==== [5/6] 백엔드 무중단 재시작 및 대기 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
  sudo systemctl restart qms
  sleep 10
EOF

echo "==== [6/6] Nginx 무중단 reload 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
  sudo nginx -t
  if [ \$? -ne 0 ]; then
    echo "❌ Nginx 설정 문법 오류 발생. reload 중단."
    exit 1
  fi
  sudo nginx -s reload
  echo "✅ Nginx 무중단 reload 완료"
EOF

echo "==== [6-1/6] 백엔드 서비스 로그 확인 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo journalctl -u qms -n 20 --no-pager"

echo "🎉 전체 배포 완료!"
