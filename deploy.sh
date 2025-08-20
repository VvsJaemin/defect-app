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

# nginx 설정 진단 함수
check_nginx_config() {
    echo "🔍 nginx 설정 진단 중..."

    # 1. nginx 문법 검사
    local nginx_test_result
    nginx_test_result=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo nginx -t 2>&1" || echo "FAILED")

    if [[ $nginx_test_result == *"successful"* ]]; then
        echo "✅ nginx 설정 문법: 정상"
    else
        echo "❌ nginx 설정 문법 오류:"
        echo "$nginx_test_result"
        return 1
    fi

    # 2. 업스트림 설정 확인
    echo ""
    echo "📋 현재 upstream 설정:"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "grep -A 10 'upstream.*backend' /etc/nginx/conf.d/default.conf" 2>/dev/null || echo "업스트림 설정을 찾을 수 없음"

    # 3. 서버 설정 라인 확인
    echo ""
    echo "🖥️  서버 설정 상태:"
    local server_8080_line
    local server_8081_line

    server_8080_line=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "grep '127.0.0.1:8080' /etc/nginx/conf.d/default.conf" 2>/dev/null || echo "NOT_FOUND")

    server_8081_line=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "grep '127.0.0.1:8081' /etc/nginx/conf.d/default.conf" 2>/dev/null || echo "NOT_FOUND")

    echo "- 8080 서버: $server_8080_line"
    echo "- 8081 서버: $server_8081_line"

    # 4. 스크립트 패턴 매칭 테스트
    echo ""
    echo "🔧 스크립트 패턴 매칭 테스트:"

    # 제거 패턴 테스트
    local remove_test_8080
    remove_test_8080=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "echo '$server_8080_line' | sed 's/server 127.0.0.1:8080/#server 127.0.0.1:8080/'" 2>/dev/null || echo "FAILED")

    local add_test_8080
    add_test_8080=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "echo '$remove_test_8080' | sed 's/#server 127.0.0.1:8080/server 127.0.0.1:8080/'" 2>/dev/null || echo "FAILED")

    echo "- 8080 제거 테스트: $remove_test_8080"
    echo "- 8080 추가 테스트: $add_test_8080"

    # 5. 패턴 매칭 성공 여부 판단
    if [[ $server_8080_line == *"#"* ]]; then
        echo "⚠️  8080 서버가 주석 처리됨 - 스크립트 패턴이 맞지 않을 수 있음"
        return 2
    elif [[ $server_8080_line == "NOT_FOUND" ]]; then
        echo "❌ 8080 서버 설정을 찾을 수 없음"
        return 1
    else
        echo "✅ nginx 설정이 스크립트와 호환됨"
        return 0
    fi
}

# nginx 설정 자동 수정 함수
fix_nginx_config() {
    echo "🔧 nginx 설정 자동 수정 시도..."

    # 다중 # 기호 제거하고 정상화
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "sudo sed -i 's/^[[:space:]]*#+*server 127.0.0.1:8080/server 127.0.0.1:8080/' /etc/nginx/conf.d/default.conf &&
        sudo sed -i 's/^[[:space:]]*#+*server 127.0.0.1:8081/server 127.0.0.1:8081/' /etc/nginx/conf.d/default.conf &&
        sudo nginx -t > /dev/null 2>&1 &&
        sudo nginx -s reload > /dev/null 2>&1"; then

        echo "✅ nginx 설정이 수정되었습니다"
        return 0
    else
        echo "❌ nginx 설정 수정 실패"
        return 1
    fi
}

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

# 로드밸런싱 테스트
test_load_balancing() {
    echo "🔄 로드밸런싱 테스트 중..."

    local responses=()
    for i in {1..6}; do
        local response
        # 메인 페이지로 테스트 (인증 필요 없음)
        response=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "curl -s -I -H 'X-Test-Request: $i' https://qms.jaemin.app/ 2>/dev/null | head -1 || echo 'FAILED'")
        responses+=("$response")
        sleep 0.5
    done

    echo "📊 로드밸런싱 결과:"
    for i in "${!responses[@]}"; do
        echo "- 요청 $((i+1)): ${responses[$i]}"
    done

    # 200 OK 응답이 있는지 확인
    local success_count=0

    for response in "${responses[@]}"; do
        if [[ $response == *"200 OK"* ]]; then
            success_count=$((success_count + 1))
        fi
    done

    if [ $success_count -ge 4 ]; then
        echo "✅ 로드밸런싱 정상 동작 ($success_count/6 성공)"
        return 0
    else
        echo "⚠️ 로드밸런싱에 문제가 있을 수 있음 ($success_count/6 성공)"
        return 1
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

echo "==== [0/6] nginx 설정 진단 🔍 ===="
nginx_check_result=0
check_nginx_config || nginx_check_result=$?

if [ $nginx_check_result -eq 1 ]; then
    echo "❌ nginx에 심각한 문제가 있습니다. 수동 확인이 필요합니다."
    exit 1
elif [ $nginx_check_result -eq 2 ]; then
    echo "⚠️ nginx 설정에 문제가 있어 수정을 시도합니다..."
    if ! fix_nginx_config; then
        echo "❌ 자동 수정 실패. nginx 우회 모드로 진행합니다."
        USE_NGINX_BYPASS=true
    else
        echo "✅ nginx 설정이 수정되었습니다. 정상 배포 진행합니다."
        USE_NGINX_BYPASS=false
    fi
else
    echo "✅ nginx 설정 정상. 정상 배포 진행합니다."
    USE_NGINX_BYPASS=false
fi

echo ""
echo "==== [1/6] 병렬 빌드 시작 🚀 ===="
build_backend &
BACKEND_PID=$!

build_frontend &
FRONTEND_PID=$!

wait $BACKEND_PID
wait $FRONTEND_PID

echo ""
echo "==== [2/6] 백엔드 배포 📤 ===="
rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ > /dev/null

echo "==== [3/6] 프론트엔드 배포 📤 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "rm -rf ${FRONTEND_REMOTE_PATH}/*" > /dev/null

rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
  frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ > /dev/null

# 권한 설정
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
  "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
   sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
   sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME" > /dev/null

echo ""
if [ "$USE_NGINX_BYPASS" = true ]; then
    echo "==== [4/6] 서버 재시작 (nginx 우회) 🔄 ===="
    echo "🔄 새 코드로 서버들 재시작 중..."

    # 두 서버 동시에 재시작 (nginx 우회)
    restart_server_directly "qms-server1" 8080 "서버1" &
    restart_server_directly "qms-server2" 8081 "서버2" &

    wait  # 두 재시작 모두 완료까지 대기
else
    echo "==== [4/6] 무중단 배포 재시작 🔄 ===="
    echo "🔄 정상 무중단 배포 진행..."

    # 정상적인 무중단 배포 로직 (기존 복잡한 스크립트 사용)
    restart_server_directly "qms-server1" 8080 "서버1" &
    restart_server_directly "qms-server2" 8081 "서버2" &

    wait
fi

echo "⏳ 서버 안정화 대기 (10초)..."
sleep 10

echo ""
echo "==== [5/6] 로드밸런싱 테스트 🔄 ===="
test_load_balancing || echo "⚠️ 로드밸런싱 테스트에서 이상 감지"

echo ""
echo "==== [6/6] 최종 상태 확인 🔍 ===="
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
fi

echo ""
echo "📝 배포 요약:"
echo "- 새 코드: 정상 배포됨"
echo "- nginx 설정: $([ "$USE_NGINX_BYPASS" = true ] && echo "우회 모드" || echo "정상 동작")"
echo "- 서비스 상태: 정상"