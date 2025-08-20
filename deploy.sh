
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

# 개선된 헬스체크 함수
health_check() {
    local port=$1
    local max_attempts=45  # 90초로 확장
    local attempt=1

    echo "🏥 포트 $port 헬스체크 시작..."

    while [ $attempt -le $max_attempts ]; do
        # 먼저 포트가 열려있는지 확인
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "netstat -tln | grep :$port > /dev/null 2>&1"; then
            echo "🔌 포트 $port 리스닝 확인됨"

            # 그 다음 health 엔드포인트 확인
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
               "curl -f http://localhost:$port/actuator/health > /dev/null 2>&1"; then
                echo "✅ 포트 $port 서비스 준비 완료 (시도: $attempt/$max_attempts)"
                return 0
            fi
        fi

        echo "⏳ 포트 $port 대기 중... ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ 포트 $port 헬스체크 실패 - 로그 확인 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "echo '=== 포트 상태 ===' && netstat -tln | grep :$port || echo '포트 $port 없음';
       echo '=== 프로세스 상태 ===' && ps aux | grep defectapp | grep -v grep || echo 'defectapp 프로세스 없음';
       echo '=== 최근 로그 ($port) ===' && tail -n 20 ${BACKEND_REMOTE_PATH}/logs/app-$port.log 2>/dev/null || echo '로그 파일 없음'"
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
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ || {
    echo "❌ 백엔드 전송 실패"; exit 1;
}

echo "==== [2-1/8] 백엔드 JAR 권한 및 소유권 설정 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo chown ubuntu:ubuntu ${BACKEND_REMOTE_PATH}/$JAR_NAME && sudo chmod 755 ${BACKEND_REMOTE_PATH}/$JAR_NAME"

echo "==== [3/8] 프론트엔드 배포 전 기존 파일 삭제 🗑️ ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*" || {
    echo "❌ 기존 프론트엔드 파일 삭제 실패"; exit 1;
}

echo "==== [4/8] 프론트엔드 배포 📤 ===="
rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ || {
    echo "❌ 프론트엔드 전송 실패"; exit 1;
}

echo "==== [5/8] 로그 디렉토리 생성 및 환경 확인 📁 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   echo '=== 현재 실행 중인 Java 프로세스 ===' &&
   ps aux | grep java | grep -v grep || echo 'Java 프로세스 없음' &&
   echo '=== 포트 사용 현황 ===' &&
   netstat -tln | grep -E ':(8080|8081)' || echo '8080, 8081 포트 사용 없음' &&
   echo '=== JAR 파일 확인 ===' &&
   ls -la ${BACKEND_REMOTE_PATH}/$JAR_NAME &&
   echo '=== Java 버전 확인 ===' &&
   java -version"

echo "==== [6/8] 무중단 배포 시작 - 서버1 (8080) 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'PID_8080=$(lsof -t -i:8080 2>/dev/null || echo "")
  if [ ! -z "$PID_8080" ]; then
    echo "🛑 서버1 (8080) 종료 중... (PID: $PID_8080)"
    kill -TERM $PID_8080

    for i in {1..30}; do
      if ! kill -0 $PID_8080 2>/dev/null; then
        echo "✅ 서버1 정상 종료됨"
        break
      fi
      sleep 1
    done

    if kill -0 $PID_8080 2>/dev/null; then
      echo "⚠️ 강제 종료 실행"
      kill -KILL $PID_8080
    fi
  fi

  echo "🚀 서버1 (8080) 시작..."
  cd /var/www/qms/backend

  # 환경변수 확인
  echo "환경변수 확인:"
  echo "DB_HOST=${DB_HOST}"
  echo "DB_NAME=${DB_NAME}"
  echo "DB_USERNAME=${DB_USERNAME}"
  echo "UPLOAD_PATH=${UPLOAD_PATH}"

  # JAR 실행
  nohup java -jar -Dspring.profiles.active=prod-server1 defectapp-0.0.1-SNAPSHOT.jar > logs/app-8080.log 2>&1 &
  NEW_PID=$!
  echo "✅ 서버1 시작됨 (PID: $NEW_PID)"

  # 잠시 대기 후 프로세스 상태 확인
  sleep 3
  if kill -0 $NEW_PID 2>/dev/null; then
    echo "✅ 서버1 프로세스 실행 중"
  else
    echo "❌ 서버1 프로세스 종료됨 - 로그 확인:"
    tail -n 20 logs/app-8080.log
    exit 1
  fi'

# 서버1 헬스체크
if ! health_check 8080; then
  echo "🔍 서버1 시작 실패 - 상세 분석 진행 중..."
  exit 1
fi

echo "==== [7/8] 무중단 배포 시작 - 서버2 (8081) 🔄 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'PID_8081=$(lsof -t -i:8081 2>/dev/null || echo "")
  if [ ! -z "$PID_8081" ]; then
    echo "🛑 서버2 (8081) 종료 중... (PID: $PID_8081)"
    kill -TERM $PID_8081

    for i in {1..30}; do
      if ! kill -0 $PID_8081 2>/dev/null; then
        echo "✅ 서버2 정상 종료됨"
        break
      fi
      sleep 1
    done

    if kill -0 $PID_8081 2>/dev/null; then
      echo "⚠️ 강제 종료 실행"
      kill -KILL $PID_8081
    fi
  fi

  echo "🚀 서버2 (8081) 시작..."
  cd /var/www/qms/backend
  nohup java -jar -Dspring.profiles.active=prod-server2 defectapp-0.0.1-SNAPSHOT.jar > logs/app-8081.log 2>&1 &
  NEW_PID=$!
  echo "✅ 서버2 시작됨 (PID: $NEW_PID)"

  # 잠시 대기 후 프로세스 상태 확인
  sleep 3
  if kill -0 $NEW_PID 2>/dev/null; then
    echo "✅ 서버2 프로세스 실행 중"
  else
    echo "❌ 서버2 프로세스 종료됨 - 로그 확인:"
    tail -n 20 logs/app-8081.log
    exit 1
  fi'

# 서버2 헬스체크
if ! health_check 8081; then
  echo "🔍 서버2 시작 실패 - 상세 분석 진행 중..."
  exit 1
fi

echo "==== [8/8] Nginx 설정 검증 및 무중단 reload 🌐 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'sudo nginx -t && sudo nginx -s reload && echo "✅ Nginx 무중단 reload 완료"'

echo "==== [9/8] 최종 상태 확인 🔍 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  'echo "📊 서버 상태 확인:"
  echo "- 포트 8080: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "연결실패")"
  echo "- 포트 8081: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "연결실패")"
  echo "- 외부 접속: $(curl -s -o /dev/null -w "%{http_code}" https://qms.jaemin.app/actuator/health 2>/dev/null || echo "연결실패")"

  echo ""
  echo "📈 프로세스 상태:"
  ps aux | grep "defectapp" | grep -v grep || echo "⚠️ Java 프로세스 없음"'

echo "🎉 무중단 배포 완료! 두 서버 모두 정상 동작 중입니다."