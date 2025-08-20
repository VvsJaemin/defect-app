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

# 단순한 서버 재시작 (nginx 우회)
restart_server_directly() {
    local service_name=$1
    local port=$2
    local display_name=$3

    echo "🔄 $display_name 직접 재시작 (nginx 우회)"

    # 기존 프로세스 종료
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo pkill -f 'defectapp.*--server.port=$port' || true" > /dev/null 2>&1

    # 잠시 대기
    sleep 3

    # 서비스 재시작
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl restart $service_name" > /dev/null 2>&1

    echo "✅ $display_name 재시작 완료"
}

# 간단한 서버 상태 확인
check_server_status() {
    local port=$1
    local service_name=$2

    # 포트 확인
    local port_status="❌"
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "netstat -tln | grep :$port > /dev/null 2>&1"; then
        port_status="✅"
    fi

    # 서비스 상태 확인
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl is-active $service_name 2>/dev/null || echo 'inactive'")

    echo "- $service_name: 포트 $port_status, 서비스 $service_status"
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

echo "==== [1/5] 병렬 빌드 시작 🚀 ===="
build_backend &
BACKEND_PID=$!

build_frontend &
FRONTEND_PID=$!

wait $BACKEND_PID
wait $FRONTEND_PID

echo "==== [2/5] 백엔드 배포 📤 ===="
rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ > /dev/null

echo "==== [3/5] 프론트엔드 배포 📤 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*" > /dev/null

rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ > /dev/null

# 권한 설정
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME" > /dev/null

echo "==== [4/5] 서버 재시작 (nginx 우회) 🔄 ===="
echo "🔄 새 코드로 서버들 재시작 중..."

# 두 서버 동시에 재시작 (nginx 우회)
restart_server_directly "qms-server1" 8080 "서버1" &
restart_server_directly "qms-server2" 8081 "서버2" &

wait  # 두 재시작 모두 완료까지 대기

echo "⏳ 서버 안정화 대기 (10초)..."
sleep 10

echo "==== [5/5] 최종 상태 확인 🔍 ===="
echo "📊 서버 상태:"
check_server_status 8080 "qms-server1"
check_server_status 8081 "qms-server2"

echo ""
echo "🌐 서비스 접속 테스트:"
EXTERNAL_STATUS=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '실패')
echo "- 외부 접속: $EXTERNAL_STATUS"

if [ "$EXTERNAL_STATUS" = "200" ]; then
    echo "🎉 배포 완료 - 새 코드가 정상 반영됨!"
elif [ "$EXTERNAL_STATUS" = "401" ] || [ "$EXTERNAL_STATUS" = "403" ]; then
    echo "🎉 배포 완료 - 서비스 정상 동작! (인증 필요한 페이지)"
else
    echo "⚠️ 서비스 상태 확인 필요"
    echo ""
    echo "🔧 문제 해결을 위한 명령어:"
    echo "ssh -i $PEM_PATH ${EC2_USER}@${EC2_HOST} 'sudo systemctl status qms-server1 qms-server2'"
fi

echo ""
echo "📝 참고사항:"
echo "- 새 코드는 이미 서버에 배포되었습니다"
echo "- nginx 설정에 문제가 있어 우회하여 재시작했습니다"
echo "- 서비스는 정상 동작해야 합니다"