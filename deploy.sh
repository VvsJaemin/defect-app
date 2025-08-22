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

# Blue-Green 포트 설정
PORT1=8080
PORT2=8081
SERVICE1="qms-server1"
SERVICE2="qms-server2"

# 백업 디렉토리
BACKUP_DIR="/var/www/qms/backup"

# 현재 활성 포트 확인
get_active_port() {
    local nginx_upstream
    nginx_upstream=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cat /etc/nginx/sites-available/default | grep 'proxy_pass' | head -1" 2>/dev/null || echo "")

    if echo "$nginx_upstream" | grep -q ":$PORT1"; then
        echo "$PORT1"
    elif echo "$nginx_upstream" | grep -q ":$PORT2"; then
        echo "$PORT2"
    else
        # 기본값: 8080이 활성이라고 가정
        echo "$PORT1"
    fi
}

# 비활성 포트 확인
get_inactive_port() {
    local active_port=$(get_active_port)
    if [ "$active_port" = "$PORT1" ]; then
        echo "$PORT2"
    else
        echo "$PORT1"
    fi
}

# 포트에 해당하는 서비스명 반환
get_service_name() {
    local port=$1
    if [ "$port" = "$PORT1" ]; then
        echo "$SERVICE1"
    else
        echo "$SERVICE2"
    fi
}

# nginx upstream 변경
switch_nginx_upstream() {
    local new_port=$1
    log_info "nginx upstream을 포트 $new_port로 변경 중..."

    # nginx 설정 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup" >/dev/null 2>&1

    # upstream 변경
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo sed -i 's/proxy_pass http:\/\/127\.0\.0\.1:[0-9]*\//proxy_pass http:\/\/127.0.0.1:$new_port\//g' /etc/nginx/sites-available/default" >/dev/null 2>&1

    # nginx 설정 테스트 및 재로드
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -t" >/dev/null 2>&1; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl reload nginx" >/dev/null 2>&1
        log_success "nginx upstream 변경 완료 (포트: $new_port)"
        return 0
    else
        log_error "nginx 설정 오류, 백업으로 복구 중..."
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mv /etc/nginx/sites-available/default.backup /etc/nginx/sites-available/default && sudo systemctl reload nginx" >/dev/null 2>&1
        return 1
    fi
}

# 서버 헬스 체크
health_check() {
    local port=$1
    local max_attempts=30
    local attempt=1

    log_info "포트 $port 헬스 체크 중..."

    while [ $attempt -le $max_attempts ]; do
        local status=$(curl -s -o /dev/null -w '%{http_code}' -m 5 "http://${EC2_HOST}:$port/actuator/health" 2>/dev/null || echo "000")

        if [ "$status" = "200" ]; then
            log_success "포트 $port 헬스 체크 성공 (시도: $attempt/$max_attempts)"
            return 0
        fi

        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo ""
    log_error "포트 $port 헬스 체크 실패"
    return 1
}

# 백업 생성
create_backup() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    log_info "백업 생성 중... ($timestamp)"

    # 백엔드 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mkdir -p $BACKUP_DIR/backend_$timestamp && sudo cp $BACKEND_REMOTE_PATH/$JAR_NAME $BACKUP_DIR/backend_$timestamp/ 2>/dev/null || true" >/dev/null 2>&1

    # 프론트엔드 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mkdir -p $BACKUP_DIR/frontend_$timestamp && sudo cp -r $FRONTEND_REMOTE_PATH $BACKUP_DIR/frontend_$timestamp/ 2>/dev/null || true" >/dev/null 2>&1

    # 오래된 백업 정리 (최근 5개만 보관)
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo find $BACKUP_DIR -maxdepth 1 -type d -name '*_*' | sort -r | tail -n +6 | xargs sudo rm -rf 2>/dev/null || true" >/dev/null 2>&1

    log_success "백업 생성 완료 ($timestamp)"
    echo "$timestamp"
}

# 롤백 함수
rollback() {
    local backup_timestamp=$1
    local failed_port=$2

    log_warning "롤백 시작..."

    if [ -z "$backup_timestamp" ]; then
        log_error "백업 타임스탬프가 없어 롤백할 수 없습니다."
        return 1
    fi

    # 실패한 서비스 중지
    local failed_service=$(get_service_name $failed_port)
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl stop $failed_service" >/dev/null 2>&1 || true

    # 백업에서 복구
    log_info "백업에서 파일 복구 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp $BACKUP_DIR/backend_$backup_timestamp/$JAR_NAME $BACKEND_REMOTE_PATH/ 2>/dev/null && sudo rm -rf $FRONTEND_REMOTE_PATH && sudo cp -r $BACKUP_DIR/frontend_$backup_timestamp/dist $FRONTEND_REMOTE_PATH" >/dev/null 2>&1

    # nginx 설정 복구
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mv /etc/nginx/sites-available/default.backup /etc/nginx/sites-available/default 2>/dev/null && sudo systemctl reload nginx" >/dev/null 2>&1 || true

    log_success "롤백 완료"
}

# SSH 연결 테스트
test_ssh_connection() {
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

# 파일 배포
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
    TEMP_FRONTEND_PATH="${FRONTEND_REMOTE_PATH}_temp"

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH} && mkdir -p ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1

    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${TEMP_FRONTEND_PATH}/; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "mv ${FRONTEND_REMOTE_PATH} ${FRONTEND_REMOTE_PATH}_old 2>/dev/null || true && mv ${TEMP_FRONTEND_PATH} ${FRONTEND_REMOTE_PATH} && rm -rf ${FRONTEND_REMOTE_PATH}_old" >/dev/null 2>&1
        log_success "프론트엔드 파일 배포 완료"
    else
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 권한 설정
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs && sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} && sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME && sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH} && sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/* && sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \;" >/dev/null 2>&1

    log_success "모든 파일 배포 완료"
}

# Blue-Green 배포 실행
blue_green_deploy() {
    local active_port=$(get_active_port)
    local inactive_port=$(get_inactive_port)
    local inactive_service=$(get_service_name $inactive_port)

    log_info "Blue-Green 배포 시작 (Active: $active_port, Deploy to: $inactive_port)"

    # 백업 생성
    local backup_timestamp=$(create_backup)

    # 비활성 서버에 새 버전 배포
    log_info "비활성 서버($inactive_service:$inactive_port) 재시작 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl restart $inactive_service" >/dev/null 2>&1

    # 헬스 체크
    if health_check $inactive_port; then
        log_success "새 버전 헬스 체크 성공"

        # 트래픽 전환
        if switch_nginx_upstream $inactive_port; then
            log_success "트래픽 전환 완료 ($active_port → $inactive_port)"

            # 구 버전 서버 중지 (graceful)
            local old_service=$(get_service_name $active_port)
            log_info "구 버전 서버($old_service) 중지 중..."
            ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl stop $old_service" >/dev/null 2>&1

            return 0
        else
            log_error "nginx 전환 실패, 롤백 중..."
            rollback "$backup_timestamp" "$inactive_port"
            return 1
        fi
    else
        log_error "새 버전 헬스 체크 실패, 롤백 중..."
        rollback "$backup_timestamp" "$inactive_port"
        return 1
    fi
}

# 최종 상태 확인
final_status_check() {
    log_info "최종 상태 확인 중..."

    echo "📊 서버 상태:"
    check_server_status $PORT1 $SERVICE1
    check_server_status $PORT2 $SERVICE2

    local active_port=$(get_active_port)
    echo "  - 활성 포트: $active_port"

    echo ""
    log_info "서비스 접속 테스트..."
    local external_status
    external_status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '실패')
    echo "  - 외부 접속: $external_status"

    if [ "$external_status" = "200" ] || [ "$external_status" = "401" ] || [ "$external_status" = "403" ]; then
        log_success "서비스 정상 동작 확인됨!"
        return 0
    else
        log_warning "서비스 상태 확인 필요 (응답: $external_status)"
        return 1
    fi
}

# 메인 배포 로직
main() {
    echo "=============================================="
    echo "🚀 Blue-Green 무중단 배포 시작"
    echo "=============================================="

    # 연결 테스트
    test_ssh_connection

    # nginx 기본 상태 확인
    check_nginx_basic

    echo ""
    echo "==== [1/4] 병렬 빌드 시작 🔨 ===="

    # 병렬 빌드
    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    echo "==== [2/4] 파일 배포 📤 ===="
    deploy_files

    echo ""
    echo "==== [3/4] Blue-Green 배포 🔄 ===="
    if blue_green_deploy; then
        log_success "Blue-Green 배포 성공"
    else
        log_error "Blue-Green 배포 실패"
        exit 1
    fi

    echo ""
    echo "==== [4/4] 최종 확인 🔍 ===="
    final_status_check

    echo ""
    echo "=============================================="
    echo "📋 배포 결과 요약"
    echo "=============================================="
    echo "✅ 새 코드: 정상 배포됨"
    echo "✅ 무중단: Blue-Green 방식 적용"
    echo "✅ 활성 서버: 포트 $(get_active_port)"
    echo "✅ 서비스: 정상 접속 가능"
    echo "=============================================="
    log_success "🎉 무중단 배포 완료!"
}

# 스크립트 실행
main "$@"