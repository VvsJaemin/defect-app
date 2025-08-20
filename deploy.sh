
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

# 포트 리스닝 체크 (더 간단하고 빠름)
quick_health_check() {
    local port=$1
    local service_name=$2
    local max_attempts=15  # 30초
    local attempt=1

    echo "🏥 $service_name 포트 체크..."

    while [ $attempt -le $max_attempts ]; do
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "netstat -tln | grep :$port > /dev/null 2>&1"; then
            echo "✅ $service_name 포트 $port 리스닝 중"
            return 0
        fi

        echo "⏳ $service_name 포트 대기 중... ($attempt/15)"
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "⚠️ $service_name 포트 $port 타임아웃"
    return 1
}

# nginx에서 특정 서버 제거/추가
toggle_nginx_server() {
    local port=$1
    local action=$2  # "remove" or "add"

    if [ "$action" = "remove" ]; then
        echo "🚫 nginx에서 서버 $port 제거"
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
          "sudo sed -i 's/server 127.0.0.1:$port/#server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf &&
           sudo nginx -s reload" > /dev/null 2>&1
    else
        echo "✅ nginx에 서버 $port 추가"
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
          "sudo sed -i 's/#server 127.0.0.1:$port/server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf &&
           sudo nginx -s reload" > /dev/null 2>&1
    fi
}

# 병렬 빌드 함수들
build_backend() {
    echo "🔨 [백엔드] 빌드 시작..."
    cd backend || { echo "❌ backend 디렉토리가 없습니다."; exit 1; }
    ./gradlew build -x test --parallel --build-cache -q || { echo "❌ 백엔드 빌드 실패"; exit 1; }
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
            npm ci --prefer-offline --no-audit --silent
            echo "$LOCK_HASH" > node_modules/.cache-timestamp
        else
            echo "♻️  캐시된 node_modules 사용"
        fi
    else
        echo "📦 첫 설치, npm install 실행..."
        npm ci --prefer-offline --no-audit --silent
        md5sum package-lock.json | cut -d' ' -f1 > node_modules/.cache-timestamp
    fi

    npm run build --silent || { echo "❌ 프론트엔드 빌드 실패"; exit 1; }
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

echo "==== [2/6] 파일 배포 📤 ===="
# 백엔드 배포
rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ > /dev/null

# 프론트엔드 배포
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/* &&
   sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME" > /dev/null

rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ > /dev/null

echo "==== [3/6] 서버1 재시작 🔄 ===="
toggle_nginx_server 8080 "remove"

ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo systemctl restart qms-server1" > /dev/null

echo "⏳ 서버1 기본 대기 (3초)..."
sleep 3

quick_health_check 8080 "서버1"
toggle_nginx_server 8080 "add"

echo "==== [4/6] 서버2 재시작 🔄 ===="
toggle_nginx_server 8081 "remove"

ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo systemctl restart qms-server2" > /dev/null

echo "⏳ 서버2 기본 대기 (3초)..."
sleep 3

quick_health_check 8081 "서버2"
toggle_nginx_server 8081 "add"

echo "==== [5/6] nginx 최종 확인 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo nginx -t && sudo nginx -s reload" > /dev/null

echo "==== [6/6] 최종 상태 확인 🔍 ===="
# 간단한 포트 체크로 최종 확인
PORT_8080=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "netstat -tln | grep :8080 > /dev/null 2>&1 && echo 'OK' || echo 'FAIL'")

PORT_8081=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "netstat -tln | grep :8081 > /dev/null 2>&1 && echo 'OK' || echo 'FAIL'")

echo "📊 배포 결과:"
echo "- 서버1 (8080): $PORT_8080"
echo "- 서버2 (8081): $PORT_8081"

echo "🎉 무중단 배포 완료!"