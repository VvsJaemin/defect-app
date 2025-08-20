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

# 빠른 헬스체크 함수 (3초 타임아웃, 1회만)
quick_health_check() {
    local port=$1

    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "curl -f -m 3 http://localhost:$port/actuator/health > /dev/null 2>&1"; then
        echo "✅ 포트 $port 정상"
        return 0
    else
        echo "⚠️ 포트 $port 확인 실패"
        return 1
    fi
}

# 실패 시에만 로그 확인
check_failure() {
    local port=$1
    local service_name=$2

    echo "🔍 $service_name 실패 원인 확인 중..."

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "echo '=== 서비스 상태 ==='
       sudo systemctl is-active $service_name || echo '$service_name 비활성'
       echo ''
       echo '=== 최근 에러 로그 (10줄) ==='
       sudo journalctl -u $service_name -n 10 --no-pager | grep -i error || echo '에러 없음'
       echo ''
       echo '=== 포트 상태 ==='
       netstat -tln | grep :$port || echo '포트 $port 리스닝 없음'"
}

# 병렬 빌드 함수들 (간소화)
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

echo "==== [2/6] 백엔드 배포 📤 ===="
rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ > /dev/null

echo "==== [3/6] 프론트엔드 배포 📤 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*" > /dev/null

rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ > /dev/null

echo "==== [4/6] 무중단 배포 - 서버1 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME &&
   sudo systemctl restart qms-server1" > /dev/null

echo "⏳ 서버1 시작 대기 중... (10초)"
sleep 10

# 서버1 헬스체크
if ! quick_health_check 8080; then
    check_failure 8080 "qms-server1"
fi

echo "==== [5/6] 무중단 배포 - 서버2 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo systemctl restart qms-server2" > /dev/null

echo "⏳ 서버2 시작 대기 중... (10초)"
sleep 10

# 서버2 헬스체크
if ! quick_health_check 8081; then
    check_failure 8081 "qms-server2"
fi

echo "==== [6/6] Nginx 리로드 및 최종 확인 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo nginx -t > /dev/null 2>&1 && sudo nginx -s reload" > /dev/null

# 최종 상태 확인 (간단히)
HEALTH_8080=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 3 http://localhost:8080/actuator/health 2>/dev/null || echo '실패'")

HEALTH_8081=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 3 http://localhost:8081/actuator/health 2>/dev/null || echo '실패'")

HEALTH_EXTERNAL=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 3 https://qms.jaemin.app/actuator/health 2>/dev/null || echo '실패'")

echo "📊 배포 결과:"
echo "- 서버1 (8080): $HEALTH_8080"
echo "- 서버2 (8081): $HEALTH_8081"
echo "- 외부 접속: $HEALTH_EXTERNAL"

echo "🎉 무중단 배포 완료!"