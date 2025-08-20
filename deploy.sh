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

# 서비스 상태 및 로그 확인 함수
check_service_status() {
    local port=$1
    local service_name=$2

    echo "🔍 서비스 $service_name (포트 $port) 상태 확인..."

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
echo "=== 서비스 상태 ==="
sudo systemctl is-active $service_name 2>/dev/null || echo "$service_name: 비활성"
sudo systemctl status $service_name --no-pager -l || echo "$service_name 상태 확인 실패"

echo ""
echo "=== 프로세스 상태 ==="
ps aux | grep java | grep defectapp | grep -v grep || echo "Java 프로세스 없음"

echo ""
echo "=== 포트 상태 ==="
netstat -tln | grep :$port || echo "포트 $port 리스닝 없음"
ss -tln | grep :$port || echo "포트 $port (ss) 리스닝 없음"

echo ""
echo "=== systemd 로그 (최근 30줄) ==="
sudo journalctl -u $service_name -n 30 --no-pager || echo "journalctl 로그 없음"

echo ""
echo "=== 애플리케이션 로그 (최근 30줄) ==="
tail -n 30 ${BACKEND_REMOTE_PATH}/logs/app-$port.log 2>/dev/null || echo "앱 로그 파일 없음"

echo ""
echo "=== 에러 로그 ==="
tail -n 10 ${BACKEND_REMOTE_PATH}/logs/app-$port-error.log 2>/dev/null || echo "에러 로그 파일 없음"

echo ""
echo "=== 시스템 리소스 ==="
echo "디스크 사용량:"
df -h ${BACKEND_REMOTE_PATH} || df -h /
echo ""
echo "메모리 사용량:"
free -h
echo ""
echo "현재 시간: \$(date)"
EOF
}

# 빠른 헬스체크 함수 (한 번만 시도)
quick_health_check() {
    local port=$1

    echo "⚡ 포트 $port 빠른 헬스체크..."

    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "curl -f -m 5 http://localhost:$port/actuator/health > /dev/null 2>&1"; then
        echo "✅ 포트 $port 헬스체크 성공!"
        return 0
    else
        echo "❌ 포트 $port 헬스체크 실패"
        return 1
    fi
}

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
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME &&
   echo '✅ 권한 설정 완료'"

echo "==== [5/8] 무중단 배포 - 서버1 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "echo '🛑 서버1 (8080) 재시작 중...'
   sudo systemctl restart qms-server1
   echo '⏳ 서버1 시작 대기 중... (15초)'
   sleep 15"

# 서버1 상태 확인
check_service_status 8080 "qms-server1"

# 빠른 헬스체크 (실패해도 계속 진행)
if quick_health_check 8080; then
    echo "✅ 서버1 정상 동작 확인"
else
    echo "⚠️ 서버1 헬스체크 실패했지만 계속 진행..."
fi

echo "==== [6/8] 무중단 배포 - 서버2 재시작 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "echo '🛑 서버2 (8081) 재시작 중...'
   sudo systemctl restart qms-server2
   echo '⏳ 서버2 시작 대기 중... (15초)'
   sleep 15"

# 서버2 상태 확인
check_service_status 8081 "qms-server2"

# 빠른 헬스체크 (실패해도 계속 진행)
if quick_health_check 8081; then
    echo "✅ 서버2 정상 동작 확인"
else
    echo "⚠️ 서버2 헬스체크 실패했지만 계속 진행..."
fi

echo "==== [7/8] Nginx 리로드 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo nginx -t && sudo nginx -s reload && echo '✅ Nginx 리로드 완료'"

echo "==== [8/8] 최종 상태 확인 🔍 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'echo "📊 최종 서버 상태:"
   echo "- 8080: $(curl -s -o /dev/null -w "%{http_code}" -m 5 http://localhost:8080/actuator/health 2>/dev/null || echo "실패")"
   echo "- 8081: $(curl -s -o /dev/null -w "%{http_code}" -m 5 http://localhost:8081/actuator/health 2>/dev/null || echo "실패")"
   echo "- 외부: $(curl -s -o /dev/null -w "%{http_code}" -m 5 https://qms.jaemin.app/actuator/health 2>/dev/null || echo "실패")"
   echo ""
   echo "📈 최종 서비스 상태:"
   echo "- qms-server1: $(sudo systemctl is-active qms-server1)"
   echo "- qms-server2: $(sudo systemctl is-active qms-server2)"'

echo "🎉 배포 완료! (헬스체크 실패가 있어도 진행됨)"