#!/bin/bash
set -e
export TZ=Asia/Seoul

# 색상 정의
RED='\033[0;31m' GREEN='\033[0;32m' YELLOW='\033[1;33m' BLUE='\033[0;34m' PURPLE='\033[0;35m' NC='\033[0m'

log_info() { echo -e "${BLUE}ℹ️ $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️ $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }
log_step() { echo -e "${PURPLE}🚀 $1${NC}"; }

ENV_FILE="./deploy.env"
if [ ! -f "$ENV_FILE" ]; then log_error "$ENV_FILE 파일이 없습니다."; exit 1; fi
source "$ENV_FILE"

BACKEND_REMOTE_PATH="/var/www/qms/backend"
FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

# 현재 nginx가 가리키는 포트 확인
get_current_active_port() {
    local config
    config=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "grep -E 'proxy_pass.*:80[0-9]+' /etc/nginx/sites-available/qms | head -1 || echo ''")
    if echo "$config" | grep -q ":8080"; then echo "8080"
    elif echo "$config" | grep -q ":8081"; then echo "8081"
    else echo "8080"; fi
}

get_target_port() { [ "$1" = "8080" ] && echo "8081" || echo "8080"; }
get_service_name() { [ "$1" = "8080" ] && echo "qms-server1" || echo "qms-server2"; }

# SSH 테스트
test_ssh_connection() {
    log_info "SSH 연결 테스트 중..."
    if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "echo 'SSH 연결 성공'" >/dev/null 2>&1; then
        log_success "SSH 연결 확인됨"
    else
        log_error "SSH 연결 실패. PEM 및 서버 상태 확인 필요."
        exit 1
    fi
}

# 헬스체크
health_check() {
    local port=$1 attempt=0 max_attempts=20
    log_info "포트 ${port} 헬스체크..."
    while [ $attempt -lt $max_attempts ]; do
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "curl -s --max-time 3 http://localhost:${port}/actuator/health | grep -q 'UP'"; then
            log_success "헬스체크 성공 (포트: $port, 시도: $((attempt+1)))"; return 0
        fi
        attempt=$((attempt+1))
        echo -n "."; sleep 3
    done
    echo ""; log_error "헬스체크 실패: $port"; return 1
}

# nginx 트래픽 스위치
switch_nginx_port() {
    local port=$1
    log_info "nginx 포트 ${port}로 전환..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo sed -i 's/proxy_pass http:\/\/localhost:[0-9]\+/proxy_pass http:\/\/localhost:${port}/' /etc/nginx/sites-available/qms \
        && sudo nginx -t && sudo nginx -s reload"
    log_success "nginx 포트 전환 완료: ${port}"
}

# 백엔드 서비스 시작
start_target_service() {
    local port=$1 service=$(get_service_name $port)
    log_info "${service} 시작 (포트: $port)..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo pkill -f 'defectapp.*--server.port=${port}' || true"
    sleep 3
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl start $service"
    health_check $port
}

# 기존 서비스 정리
cleanup_previous_service() {
    local port=$1 service=$(get_service_name $port)
    log_info "이전 서비스 정리: ${service} (포트: $port)"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl stop $service" >/dev/null 2>&1 || true
    log_success "이전 서비스 종료 완료"
}

# 파일 배포
deploy_files() {
    log_info "배포 파일 업로드..."
    rsync -az -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/
    rsync -az --delete -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/
    log_success "파일 배포 완료"
}

# 메인
main() {
    test_ssh_connection
    current_port=$(get_current_active_port)
    target_port=$(get_target_port $current_port)
    log_step "현재: $current_port, 배포 대상: $target_port"

    build_backend & BACKEND_PID=$!
    build_frontend & FRONTEND_PID=$!
    wait $BACKEND_PID $FRONTEND_PID

    deploy_files
    start_target_service $target_port
    switch_nginx_port $target_port
    cleanup_previous_service $current_port

    log_success "🎉 무중단 배포 완료! 포트 ${target_port} 활성화"
}

main "$@"
