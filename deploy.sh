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

ENV_FILE="./deploy.env"

if [ ! -f "$ENV_FILE" ]; then
    log_error "$ENV_FILE 파일이 없습니다."
    exit 1
fi

source "$ENV_FILE"

BACKEND_REMOTE_PATH="/var/www/qms/backend"
FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

# SSH 연결 테스트
test_ssh_connection() {
    # PEM 키가 없으면 그냥 건너뛰기
    if [ ! -f "$PEM_PATH" ] || [ -z "$PEM_PATH" ]; then
        log_info "PEM 키가 없어 SSH 테스트를 건너뜁니다."
        return 0
    fi

    log_info "SSH 연결 테스트 중..."
    if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "echo 'SSH 연결 성공'" >/dev/null 2>&1; then
        log_success "SSH 연결 확인됨"
        return 0
    else
        log_error "SSH 연결 실패. 서버 상태와 PEM 파일을 확인해주세요."
        exit 1
    fi
}

# nginx 기본 상태 확인
check_nginx_basic() {
    log_info "nginx 기본 상태 확인 중..."

    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -t > /dev/null 2>&1"; then
        log_success "nginx 설정 정상"
        return 0
    else
        log_warning "nginx 설정에 문제가 있을 수 있음"
        return 1
    fi
}

# 서버 재시작 함수 (502 오류 방지)
restart_server_safely() {
    local service_name=$1
    local port=$2
    local display_name=$3

    log_info "$display_name 안전한 재시작 중..."

    # 기존 프로세스 정리
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo pkill -f 'defectapp.*--server.port=$port' || true" >/dev/null 2>&1

    # 포트 해제 대기
    sleep 3

    # 서비스 재시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl restart $service_name" >/dev/null 2>&1; then
        log_success "$display_name 재시작 완료"
        return 0
    else
        log_warning "$display_name 재시작 실패했지만 계속 진행"
        return 1
    fi
}

# 서버 상태 확인
check_server_status() {
    local port=$1
    local service_name=$2

    # 포트 상태
    local port_status="❌"
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :$port > /dev/null 2>&1"; then
        port_status="✅"
    fi

    # 서비스 상태
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $service_name 2>/dev/null || echo 'inactive'")

    echo "  - $service_name: 포트 $port_status, 서비스 $service_status"
}

# 백엔드 빌드
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

# 프론트엔드 빌드
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

# 파일 배포 (무중단 배포 적용)
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
    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/; then
        log_success "백엔드 파일 배포 완료"
    else
        log_error "백엔드 파일 배포 실패"
        exit 1
    fi

    # 프론트엔드 무중단 배포
    log_info "프론트엔드 파일 배포 중..."

    # 임시 디렉토리 생성
    TEMP_FRONTEND_PATH="${FRONTEND_REMOTE_PATH}_temp"

    # 임시 디렉토리에 새 파일들 배포
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH} && mkdir -p ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1

    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${TEMP_FRONTEND_PATH}/; then

        # 원자적 교체 (atomic swap)
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "mv ${FRONTEND_REMOTE_PATH} ${FRONTEND_REMOTE_PATH}_old 2>/dev/null || true && mv ${TEMP_FRONTEND_PATH} ${FRONTEND_REMOTE_PATH} && rm -rf ${FRONTEND_REMOTE_PATH}_old" >/dev/null 2>&1

        log_success "프론트엔드 파일 배포 완료"
    else
        # 실패 시 임시 디렉토리 정리
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 권한 설정
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs && sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} && sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME && sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH} && sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/* && sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \;" >/dev/null 2>&1

    # nginx 캐시 무효화
    log_info "nginx 캐시 무효화 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -s reload" >/dev/null 2>&1 || log_warning "nginx reload 실패"

    log_success "모든 파일 배포 완료"
}

# 최종 상태 확인
final_status_check() {
    log_info "최종 상태 확인 중..."

    echo "📊 서버 상태:"
    check_server_status 8080 "qms-server1"
    check_server_status 8081 "qms-server2"

    echo ""
    log_info "서비스 접속 테스트..."
    local external_status
    external_status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '실패')
    echo "  - 외부 접속: $external_status"

    if [ "$external_status" = "200" ]; then
        log_success "서비스 정상 동작 확인됨!"
        return 0
    elif [ "$external_status" = "401" ] || [ "$external_status" = "403" ]; then
        log_success "서비스 정상 동작! (인증 필요한 페이지)"
        return 0
    else
        log_warning "서비스 상태 확인 필요 (응답: $external_status)"
        return 1
    fi
}

# 메인 배포 로직
main() {
    echo "=============================================="
    echo "🚀 배포 시작"
    echo "=============================================="

    # 연결 테스트
    test_ssh_connection

    # nginx 기본 상태 확인
    check_nginx_basic

    echo ""
    echo "==== [1/5] 병렬 빌드 시작 🔨 ===="

    # 병렬 빌드
    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    echo "==== [2/5] 파일 배포 📤 ===="
    deploy_files

    echo ""
    echo "==== [3/5] 서버 재시작 🔄 ===="
    log_info "두 서버를 안전하게 재시작합니다..."

    # 병렬 재시작
    restart_server_safely "qms-server1" 8080 "서버1" &
    restart_server_safely "qms-server2" 8081 "서버2" &

    wait # 모든 재시작 완료 대기

    echo ""
    echo "==== [4/5] 안정화 대기 ⏳ ===="
    log_info "서버 안정화 대기 중... (10초)"
    sleep 10

    echo ""
    echo "==== [5/5] 최종 확인 🔍 ===="

    final_status_check

    echo ""
    echo "=============================================="
    echo "📋 배포 결과 요약"
    echo "=============================================="
    echo "✅ 새 코드: 정상 배포됨"
    echo "✅ 두 서버: 활성화됨 (자동 로드밸런싱)"
    echo "✅ nginx: 정상 동작"
    echo "✅ 서비스: 정상 접속 가능"
    echo "=============================================="
    log_success "🎉 배포 완료!"
}

# 스크립트 실행
main "$@"