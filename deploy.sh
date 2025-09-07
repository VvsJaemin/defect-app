#!/bin/bash

set -e
export TZ=Asia/Seoul

# 색상 코드 정의
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; PURPLE='\033[0;35m'; NC='\033[0m'

# 로그 함수
log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }
log_step() { echo -e "${PURPLE}🚀 $1${NC}"; }

ENV_FILE="./deploy.env"
if [ ! -f "$ENV_FILE" ]; then log_error "$ENV_FILE 파일이 없습니다."; exit 1; fi
source "$ENV_FILE"

BACKEND_REMOTE_PATH="/var/www/qms/backend"
FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

get_current_active_port() {
    local config
    config=$(ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "grep 'proxy_pass' /etc/nginx/sites-available/qms || true")
    if echo "$config" | grep -q ":8080"; then echo "8080"; else echo "8081"; fi
}

get_target_port() {
    [ "$1" = "8080" ] && echo "8081" || echo "8080"
}

get_service_name() { [ "$1" = "8080" ] && echo "qms-server1" || echo "qms-server2"; }

test_ssh_connection() {
    log_info "SSH 연결 테스트..."
    ssh -i "$PEM_PATH" -o ConnectTimeout=10 ${EC2_USER}@${EC2_HOST} "echo 'SSH OK'" >/dev/null || { log_error "SSH 연결 실패"; exit 1; }
    log_success "SSH 연결 확인됨"
}

health_check() {
    local port=$1 max_attempts=20 attempt=0
    log_info "포트 ${port} 헬스체크 중..."
    while [ $attempt -lt $max_attempts ]; do
        if ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 http://localhost:${port}/actuator/health | grep -q 'UP'"; then
            log_success "헬스체크 성공 (포트: $port)"
            return 0
        fi
        attempt=$((attempt + 1)); echo -n "."; sleep 3
    done
    echo ""; log_error "포트 ${port} 헬스체크 실패"; return 1
}

switch_nginx_port() {
    local port=$1
    log_info "nginx 포트 ${port}로 전환..."
    ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo sed -i 's/proxy_pass http:\/\/localhost:[0-9]\+/proxy_pass http:\/\/localhost:${port}/' /etc/nginx/sites-available/qms
        sudo nginx -t && sudo nginx -s reload
    "
    log_success "nginx 전환 완료"
}

rollback() {
    local current_port=$(get_current_active_port)
    local prev_port=$(get_target_port $current_port)
    log_warning "롤백: 포트 ${prev_port}로 전환"
    switch_nginx_port $prev_port
}

build_backend() {
    log_info "[백엔드] 빌드 시작"; cd backend
    ./gradlew build -x test --parallel --build-cache -q || { log_error "백엔드 빌드 실패"; exit 1; }
    cd ..; log_success "[백엔드] 빌드 완료"
}

build_frontend() {
    log_info "[프론트엔드] 빌드 시작"; cd frontend
    npm ci --prefer-offline --no-audit --silent
    npm run build --silent || { log_error "프론트엔드 빌드 실패"; exit 1; }
    cd ..; log_success "[프론트엔드] 빌드 완료"
}

deploy_files() {
    log_info "파일 배포 시작"

    # 백엔드
    rsync -az -e "ssh -i $PEM_PATH" backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/
    log_success "백엔드 업로드 완료"

    # 프론트엔드 임시 배포
    TMP_DIR="${FRONTEND_REMOTE_PATH}_tmp"
    ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf $TMP_DIR && mkdir -p $TMP_DIR"
    rsync -az -e "ssh -i $PEM_PATH" frontend/dist/ ${EC2_USER}@${EC2_HOST}:$TMP_DIR/
    ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo chown -R www-data:www-data $TMP_DIR
        mv ${FRONTEND_REMOTE_PATH} ${FRONTEND_REMOTE_PATH}_backup || true
        mv $TMP_DIR $FRONTEND_REMOTE_PATH
    "
    log_success "프론트엔드 배포 완료 (atomic)"
}

start_target_service() {
    local port=$1
    local service=$(get_service_name $port)
    log_info "새 서비스 시작: $service (포트: $port)"
    ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        nohup java -jar ${BACKEND_REMOTE_PATH}/$JAR_NAME --server.port=${port} > ${BACKEND_REMOTE_PATH}/logs/$service.log 2>&1 &
    "
    health_check $port || { log_error "새 서비스 헬스체크 실패"; rollback; exit 1; }
}

cleanup_previous_service() {
    local port=$1 service=$(get_service_name $port)
    log_info "이전 서비스 종료: $service"
    ssh -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo pkill -f 'defectapp.*--server.port=$port' || true"
    log_success "이전 서비스 종료 완료"
}

main() {
    start_time=$(date +%s)
    echo "================================================"
    echo "🔄 QMS 무중단 배포 시작: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "================================================"

    test_ssh_connection
    current_port=$(get_current_active_port)
    target_port=$(get_target_port $current_port)
    log_step "현재 포트: $current_port → 배포 대상: $target_port"

    log_step "STEP 1: 빌드"
    build_backend & BACKEND_PID=$!
    build_frontend & FRONTEND_PID=$!
    wait $BACKEND_PID $FRONTEND_PID

    log_step "STEP 2: 파일 배포"
    deploy_files

    log_step "STEP 3: 새 서비스 시작"
    start_target_service $target_port

    log_step "STEP 4: nginx 전환"
    switch_nginx_port $target_port

    log_step "STEP 5: 이전 서비스 종료"
    cleanup_previous_service $current_port

    # 배포 시간
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    log_success "🎉 배포 완료 (소요 시간: ${duration}초)"
}

trap 'log_error "배포 중단됨"; exit 1' INT TERM
main "$@"
