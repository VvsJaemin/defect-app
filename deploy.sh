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

# 강화된 헬스체크 함수 (재시도 로직 포함)
wait_for_service() {
    local port=$1
    local service_name=$2
    local max_attempts=45  # 90초 대기
    local attempt=1

    echo "🏥 $service_name 준비 대기 중..."

    while [ $attempt -le $max_attempts ]; do
        # 먼저 포트가 리스닝 상태인지 확인
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "netstat -tln | grep :$port > /dev/null 2>&1"; then

            # 포트가 열려있으면 health 엔드포인트 확인
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
               "curl -f -m 5 http://localhost:$port/actuator/health > /dev/null 2>&1"; then
                echo "✅ $service_name 완전히 준비됨 ($attempt회 시도)"
                return 0
            fi
        fi

        # 진행 상황 표시 (5초마다)
        if [ $((attempt % 5)) -eq 0 ]; then
            echo "⏳ $service_name 대기 중... ($attempt/$max_attempts)"
        fi

        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ $service_name 시작 실패 - 로그 확인 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "echo '=== 서비스 상태 ==='
       sudo systemctl status $service_name --no-pager -l | tail -10
       echo ''
       echo '=== 애플리케이션 로그 (최근 20줄) ==='
       tail -n 20 ${BACKEND_REMOTE_PATH}/logs/app-$port.log 2>/dev/null || echo '로그 없음'"

    return 1
}

# nginx에서 특정 서버 제거 (무중단을 위해)
remove_server_from_nginx() {
    local port=$1
    echo "🚫 nginx에서 서버 $port 일시 제거"

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "sudo sed -i 's/server 127.0.0.1:$port/#server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf &&
       sudo nginx -s reload" > /dev/null 2>&1 || echo "nginx 업데이트 실패"
}

# nginx에 서버 다시 추가
add_server_to_nginx() {
    local port=$1
    echo "✅ nginx에 서버 $port 다시 추가"

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "sudo sed -i 's/#server 127.0.0.1:$port/server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf &&
       sudo nginx -s reload" > /dev/null 2>&1 || echo "nginx 업데이트 실패"
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

echo "==== [1/7] 병렬 빌드 시작 🚀 ===="
build_backend &
BACKEND_PID=$!

build_frontend &
FRONTEND_PID=$!

wait $BACKEND_PID
wait $FRONTEND_PID

echo "==== [2/7] 백엔드 배포 📤 ===="
rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ > /dev/null

echo "==== [3/7] 프론트엔드 배포 📤 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*" > /dev/null

rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ > /dev/null

echo "==== [4/7] 무중단 배포 - 서버1 재시작 🔄 ===="
# 서버1을 nginx에서 제거 (트래픽이 서버2로만 가도록)
remove_server_from_nginx 8080

# 권한 설정 후 서버1 재시작
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME &&
   sudo systemctl restart qms-server1" > /dev/null

# 서버1이 완전히 준비될 때까지 대기
if wait_for_service 8080 "qms-server1"; then
    # 서버1을 다시 nginx에 추가
    add_server_to_nginx 8080
else
    echo "⚠️ 서버1 시작 실패했지만 계속 진행..."
    add_server_to_nginx 8080  # 실패해도 다시 추가
fi

echo "==== [5/7] 무중단 배포 - 서버2 재시작 🔄 ===="
# 서버2를 nginx에서 제거 (트래픽이 서버1으로만 가도록)
remove_server_from_nginx 8081

# 서버2 재시작
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo systemctl restart qms-server2" > /dev/null

# 서버2가 완전히 준비될 때까지 대기
if wait_for_service 8081 "qms-server2"; then
    # 서버2를 다시 nginx에 추가
    add_server_to_nginx 8081
else
    echo "⚠️ 서버2 시작 실패했지만 계속 진행..."
    add_server_to_nginx 8081  # 실패해도 다시 추가
fi

echo "==== [6/7] Nginx 최종 설정 확인 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo nginx -t && sudo nginx -s reload" > /dev/null

echo "==== [7/7] 최종 상태 확인 🔍 ===="
# 최종 상태 확인 (간단히)
HEALTH_8080=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 5 http://localhost:8080/actuator/health 2>/dev/null || echo '실패'")

HEALTH_8081=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 5 http://localhost:8081/actuator/health 2>/dev/null || echo '실패'")

HEALTH_EXTERNAL=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "curl -s -o /dev/null -w '%{http_code}' -m 5 https://qms.jaemin.app/actuator/health 2>/dev/null || echo '실패'")

echo "📊 배포 결과:"
echo "- 서버1 (8080): $HEALTH_8080"
echo "- 서버2 (8081): $HEALTH_8081"
echo "- 외부 접속: $HEALTH_EXTERNAL"

echo "🎉 진정한 무중단 배포 완료!"