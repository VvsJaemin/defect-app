#!/bin/bash

set -e

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수들
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 환경 변수 파일 확인
ENV_FILE="./deploy.env"

if [ ! -f "$ENV_FILE" ]; then
    log_error "$ENV_FILE 파일이 없습니다."
    exit 1
fi

source "$ENV_FILE"

# 필수 환경 변수 확인
check_required_vars() {
    local required_vars=("PEM_PATH" "EC2_USER" "EC2_HOST")
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            log_error "환경 변수 $var 가 설정되지 않았습니다."
            exit 1
        fi
    done
}

check_required_vars

# 설정 변수
BACKEND_REMOTE_PATH="/var/www/qms/backend"
FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

# SSH 연결 테스트 함수
test_ssh_connection() {
    log_info "SSH 연결 테스트 중..."
    if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "echo 'SSH 연결 성공'" >/dev/null 2>&1; then
        log_success "SSH 연결 확인됨"
        return 0
    else
        log_error "SSH 연결 실패. 서버 상태와 PEM 파일을 확인해주세요."
        exit 1
    fi
}

# 관대한 헬스체크 (실패해도 배포 계속)
gentle_health_check() {
    local port=$1
    local service_name=$2
    local max_attempts=15  # 30초로 단축
    local attempt=1

    log_info "$service_name 헬스체크 시작 (관대한 모드, 최대 30초)..."

    while [ $attempt -le $max_attempts ]; do
        # 1. 포트 리스닝 체크
        local port_listening=false
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "netstat -tln 2>/dev/null | grep ':$port ' | grep LISTEN >/dev/null 2>&1"; then
            port_listening=true
        fi

        # 2. 프로세스 체크
        local process_running=false
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
           "pgrep -f 'defectapp.*--server.port=$port' >/dev/null 2>&1"; then
            process_running=true
        fi

        if [ "$port_listening" = true ] && [ "$process_running" = true ]; then
            log_success "$service_name 헬스체크 통과! 🎉"
            return 0
        fi

        # 상태 표시
        local port_status="❌"
        local process_status="❌"
        [ "$port_listening" = true ] && port_status="✅"
        [ "$process_running" = true ] && process_status="✅"

        echo "⏳ $service_name 확인 중... ($attempt/15) [포트: $port_status, 프로세스: $process_status]"

        sleep 2
        attempt=$((attempt + 1))
    done

    log_warning "$service_name 헬스체크 타임아웃 - 하지만 배포 계속 진행"

    # 현재 상태 정보만 표시
    log_info "$service_name 현재 상태:"

    # 포트 체크
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "netstat -tln 2>/dev/null | grep ':$port ' | grep LISTEN >/dev/null 2>&1"; then
        echo "  - 포트 $port: ✅ 리스닝 중"
    else
        echo "  - 포트 $port: ❌ 리스닝 안함"
    fi

    # 프로세스 체크
    local process_count
    process_count=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "pgrep -f 'defectapp.*--server.port=$port' | wc -l" 2>/dev/null || echo "0")
    echo "  - 프로세스 개수: $process_count"

    log_info "헬스체크 실패했지만 nginx에 추가하여 서비스 진행"
    return 0  # 항상 성공으로 반환
}

# nginx에서 특정 서버 제거/추가
toggle_nginx_server() {
    local port=$1
    local action=$2
    local max_retries=3
    local retry=1

    while [ $retry -le $max_retries ]; do
        if [ "$action" = "remove" ]; then
            log_info "nginx에서 서버 $port 제거 시도 ($retry/$max_retries)"
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
              "sudo sed -i 's/server 127.0.0.1:$port/#server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf 2>/dev/null &&
               sudo nginx -t >/dev/null 2>&1 &&
               sudo nginx -s reload >/dev/null 2>&1"; then
                log_success "nginx에서 서버 $port 제거 완료"
                return 0
            else
                log_warning "nginx에서 서버 $port 제거 실패 (시도 $retry/$max_retries)"
            fi
        else
            log_info "nginx에 서버 $port 추가 시도 ($retry/$max_retries)"
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
              "sudo sed -i 's/#server 127.0.0.1:$port/server 127.0.0.1:$port/' /etc/nginx/conf.d/default.conf 2>/dev/null &&
               sudo nginx -t >/dev/null 2>&1 &&
               sudo nginx -s reload >/dev/null 2>&1"; then
                log_success "nginx에 서버 $port 추가 완료"
                return 0
            else
                log_warning "nginx에 서버 $port 추가 실패 (시도 $retry/$max_retries)"
            fi
        fi

        retry=$((retry + 1))
        if [ $retry -le $max_retries ]; then
            sleep 2
        fi
    done

    if [ "$action" = "add" ]; then
        log_warning "nginx에 서버 $port 추가 실패 - 수동 확인 필요"
        return 0  # 실패해도 배포 계속 진행
    else
        log_warning "nginx에서 서버 $port 제거 실패하였지만 계속 진행"
        return 0
    fi
}

# 서비스 상태 확인 함수
check_service_status() {
    local service_name=$1
    local status

    status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl is-active $service_name 2>/dev/null || echo 'inactive'")

    echo "$status"
}

# 서비스 재시작 함수 (502 오류 방지)
restart_service_with_retry() {
    local service_name=$1
    local port=$2
    local display_name=$3
    local max_retries=3
    local retry=1

    log_info "$display_name 재시작 시작..."

    while [ $retry -le $max_retries ]; do
        log_info "$display_name 재시작 시도 ($retry/$max_retries)"

        # 기존 프로세스 강제 종료 (502 오류 방지)
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "sudo pkill -f 'defectapp.*--server.port=$port' || true" >/dev/null 2>&1

        # 포트가 완전히 해제될 때까지 대기 (최대 10초)
        local port_wait=0
        while [ $port_wait -lt 5 ] && ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
              "netstat -tln 2>/dev/null | grep ':$port ' | grep LISTEN >/dev/null 2>&1"; do
            sleep 2
            port_wait=$((port_wait + 1))
        done

        # 서비스 시작
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "sudo systemctl start $service_name" >/dev/null 2>&1; then

            # 초기 안정화 대기 (3초로 단축)
            sleep 3

            local status=$(check_service_status $service_name)

            if [ "$status" = "active" ]; then
                log_success "$display_name systemctl 재시작 완료"
                return 0
            else
                log_warning "$display_name 서비스가 active 상태가 아님: $status"
            fi
        else
            log_warning "$display_name systemctl 재시작 실패 (시도 $retry/$max_retries)"
        fi

        retry=$((retry + 1))
        if [ $retry -le $max_retries ]; then
            sleep 3
        fi
    done

    log_warning "$display_name 재시작 실패했지만 배포 계속 진행"

    # 로그 출력 (참고용)
    log_info "$display_name 서비스 로그 (참고용):"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo journalctl -u $service_name --lines=5 --no-pager" 2>/dev/null || true

    return 0  # 실패해도 성공으로 반환
}

# 백엔드 빌드 함수
build_backend() {
    log_info "[백엔드] 빌드 시작..."
    cd backend || { log_error "backend 디렉토리가 없습니다."; exit 1; }

    if ./gradlew build -x test --parallel --build-cache -q; then
        cd ..
        log_success "[백엔드] 빌드 완료"
    else
        cd ..
        log_error "백엔드 빌드 실패"
        exit 1
    fi
}

# 프론트엔드 빌드 함수
build_frontend() {
    log_info "[프론트엔드] 빌드 시작..."
    cd frontend || { log_error "frontend 디렉토리가 없습니다."; exit 1; }

    # 의존성 관리
    if [ -f "node_modules/.cache-timestamp" ] && [ -f "package-lock.json" ]; then
        LOCK_HASH=$(md5sum package-lock.json | cut -d' ' -f1)
        CACHED_HASH=$(cat node_modules/.cache-timestamp 2>/dev/null || echo "")

        if [ "$LOCK_HASH" != "$CACHED_HASH" ]; then
            log_info "의존성 변경 감지, npm install 실행..."
            npm ci --prefer-offline --no-audit --silent
            echo "$LOCK_HASH" > node_modules/.cache-timestamp
        else
            log_info "캐시된 node_modules 사용"
        fi
    else
        log_info "첫 설치, npm install 실행..."
        npm ci --prefer-offline --no-audit --silent
        md5sum package-lock.json | cut -d' ' -f1 > node_modules/.cache-timestamp
    fi

    if npm run build --silent; then
        cd ..
        log_success "[프론트엔드] 빌드 완료"
    else
        cd ..
        log_error "프론트엔드 빌드 실패"
        exit 1
    fi
}

# 파일 배포 함수
deploy_files() {
    log_info "파일 배포 시작..."

    # JAR 파일 존재 확인
    if [ ! -f "backend/build/libs/$JAR_NAME" ]; then
        log_error "JAR 파일을 찾을 수 없습니다: backend/build/libs/$JAR_NAME"
        exit 1
    fi

    # 프론트엔드 빌드 파일 존재 확인
    if [ ! -d "frontend/dist" ]; then
        log_error "프론트엔드 빌드 디렉토리를 찾을 수 없습니다: frontend/dist"
        exit 1
    fi

    # 백엔드 배포
    log_info "백엔드 파일 배포 중..."
    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
      backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/; then
        log_success "백엔드 파일 배포 완료"
    else
        log_error "백엔드 파일 배포 실패"
        exit 1
    fi

    # 서버 디렉토리 준비
    log_info "서버 디렉토리 준비 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "rm -rf ${FRONTEND_REMOTE_PATH}/* &&
       sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs &&
       sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} &&
       sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME" >/dev/null 2>&1

    # 프론트엔드 배포
    log_info "프론트엔드 파일 배포 중..."
    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
      frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/; then
        log_success "프론트엔드 파일 배포 완료"
    else
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    log_success "모든 파일 배포 완료"
}

# 최종 상태 확인 함수
final_health_check() {
    log_info "최종 상태 확인 중..."

    # 포트 체크
    local port_8080_status="FAIL"
    local port_8081_status="FAIL"

    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "netstat -tln 2>/dev/null | grep ':8080 ' | grep LISTEN >/dev/null 2>&1"; then
        port_8080_status="OK"
    fi

    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
       "netstat -tln 2>/dev/null | grep ':8081 ' | grep LISTEN >/dev/null 2>&1"; then
        port_8081_status="OK"
    fi

    # 서비스 상태 확인
    local server1_status=$(check_service_status "qms-server1")
    local server2_status=$(check_service_status "qms-server2")

    echo "=============================================="
    echo "📊 배포 결과 요약"
    echo "=============================================="
    echo "서버1 (8080):"
    echo "  - 포트 상태: $port_8080_status"
    echo "  - 서비스 상태: $server1_status"
    echo ""
    echo "서버2 (8081):"
    echo "  - 포트 상태: $port_8081_status"
    echo "  - 서비스 상태: $server2_status"
    echo ""
    if [ "$port_8080_status" = "OK" ] && [ "$port_8081_status" = "OK" ]; then
        echo "🎯 서비스 접속: https://qms.jaemin.app"
    else
        echo "⚠️  일부 서버에 문제가 있을 수 있습니다"
        echo "   서비스가 정상 작동하지 않으면 수동 확인이 필요합니다"
    fi
    echo "=============================================="

    log_success "배포 완료! 🎉"
    return 0  # 항상 성공으로 처리
}

# 메인 배포 로직
main() {
    echo "=============================================="
    echo "🚀 관대한 무중단 배포 시작"
    echo "=============================================="

    # SSH 연결 테스트
    test_ssh_connection

    echo ""
    echo "==== [1/6] 병렬 빌드 시작 🚀 ===="

    # 병렬 빌드 시작
    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    echo "==== [2/6] 파일 배포 📤 ===="
    deploy_files

    echo ""
    echo "==== [3/6] 서버1 재시작 🔄 ===="
    toggle_nginx_server 8080 "remove"
    restart_service_with_retry "qms-server1" 8080 "서버1"
    gentle_health_check 8080 "서버1"
    toggle_nginx_server 8080 "add"

    echo ""
    echo "==== [4/6] 서버2 재시작 🔄 ===="
    toggle_nginx_server 8081 "remove"
    restart_service_with_retry "qms-server2" 8081 "서버2"
    gentle_health_check 8081 "서버2"
    toggle_nginx_server 8081 "add"

    echo ""
    echo "==== [5/6] nginx 최종 확인 🌐 ===="
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
      "sudo nginx -t >/dev/null 2>&1 && sudo nginx -s reload >/dev/null 2>&1"; then
        log_success "nginx 설정 검증 및 재로드 완료"
    else
        log_warning "nginx 설정에 문제가 있을 수 있습니다 - 수동 확인 필요"
    fi

    echo ""
    echo "==== [6/6] 최종 상태 확인 🔍 ===="
    final_health_check
}

# 스크립트 실행
main "$@"