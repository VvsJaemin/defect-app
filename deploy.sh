#!/bin/bash

set -e

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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
FRONTEND_REMOTE_PATH="/var/www/qms/frontend"
JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

# 현재 활성 환경 확인 (blue 또는 green)
get_current_active_env() {
    local nginx_config
    nginx_config=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "cat /etc/nginx/sites-available/qms" 2>/dev/null || echo "")

    if echo "$nginx_config" | grep -q "proxy_pass.*:8080"; then
        echo "blue"
    elif echo "$nginx_config" | grep -q "proxy_pass.*:8081"; then
        echo "green"
    else
        echo "blue"  # 기본값
    fi
}

# 타겟 환경 결정
get_target_env() {
    local current_env=$1
    if [ "$current_env" = "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

# 환경별 포트 매핑
get_port_for_env() {
    local env=$1
    if [ "$env" = "blue" ]; then
        echo "8080"
    else
        echo "8081"
    fi
}

# 환경별 서비스명 매핑
get_service_for_env() {
    local env=$1
    if [ "$env" = "blue" ]; then
        echo "qms-server1"
    else
        echo "qms-server2"
    fi
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

# 헬스체크
health_check() {
    local port=$1
    local max_attempts=30
    local attempt=0

    log_info "포트 ${port}에서 헬스체크 시작..."

    while [ $attempt -lt $max_attempts ]; do
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s http://localhost:${port}/actuator/health > /dev/null 2>&1"; then
            log_success "포트 ${port} 헬스체크 성공 (시도: $((attempt + 1)))"
            return 0
        fi

        attempt=$((attempt + 1))
        log_info "헬스체크 재시도 중... ($attempt/$max_attempts)"
        sleep 5
    done

    log_error "포트 ${port} 헬스체크 실패"
    return 1
}

# nginx 설정 교체
switch_nginx_config() {
    local target_env=$1
    local target_port=$(get_port_for_env $target_env)

    log_info "nginx를 ${target_env} 환경 (포트: ${target_port})으로 전환 중..."

    # nginx 설정 파일 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp /etc/nginx/sites-available/qms /etc/nginx/sites-available/qms.backup.$(date +%Y%m%d_%H%M%S)" >/dev/null 2>&1

    # 새로운 nginx 설정 생성
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
    sudo tee /etc/nginx/sites-available/qms > /dev/null <<EOF
server {
    listen 80;
    server_name qms.jaemin.app;
    return 301 https://\\\$server_name\\\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name qms.jaemin.app;

    # SSL 설정 (기존 설정 유지)
    ssl_certificate /etc/letsencrypt/live/qms.jaemin.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/qms.jaemin.app/privkey.pem;

    # 프론트엔드 정적 파일
    location / {
        root ${FRONTEND_REMOTE_PATH}/dist;
        try_files \\\$uri \\\$uri/ /index.html;
        add_header Cache-Control 'no-cache, no-store, must-revalidate';
        expires -1;
    }

    # API 프록시 - ${target_env} 환경
    location /api/ {
        proxy_pass http://localhost:${target_port};
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
EOF
    "

    # nginx 설정 테스트
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -t" >/dev/null 2>&1; then
        # nginx 리로드
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -s reload" >/dev/null 2>&1
        log_success "nginx 설정 전환 완료: ${target_env} 환경 (포트: ${target_port})"
        return 0
    else
        log_error "nginx 설정 테스트 실패. 이전 설정으로 롤백합니다."
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp /etc/nginx/sites-available/qms.backup.* /etc/nginx/sites-available/qms && sudo nginx -s reload" >/dev/null 2>&1
        return 1
    fi
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

    # 의존성 관리 (기존과 동일)
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

# 타겟 환경에 배포
deploy_to_target_env() {
    local target_env=$1
    local target_service=$(get_service_for_env $target_env)
    local target_port=$(get_port_for_env $target_env)

    log_info "${target_env} 환경으로 배포 시작..."

    # JAR 파일 존재 확인
    if [ ! -f "backend/build/libs/$JAR_NAME" ]; then
        log_error "JAR 파일을 찾을 수 없습니다: backend/build/libs/$JAR_NAME"
        exit 1
    fi

    # 백엔드 배포
    log_info "${target_env} 환경에 백엔드 파일 배포 중..."
    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/; then
        log_success "${target_env} 환경 백엔드 파일 배포 완료"
    else
        log_error "${target_env} 환경 백엔드 파일 배포 실패"
        exit 1
    fi

    # 프론트엔드 배포 (기존과 동일한 방식)
    log_info "${target_env} 환경에 프론트엔드 파일 배포 중..."

    TEMP_FRONTEND_PATH="${FRONTEND_REMOTE_PATH}/dist_temp"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH} && mkdir -p ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1

    if rsync -az --timeout=60 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${TEMP_FRONTEND_PATH}/; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "mv ${FRONTEND_REMOTE_PATH}/dist ${FRONTEND_REMOTE_PATH}/dist_old 2>/dev/null || true && mv ${TEMP_FRONTEND_PATH} ${FRONTEND_REMOTE_PATH}/dist && rm -rf ${FRONTEND_REMOTE_PATH}/dist_old" >/dev/null 2>&1
        log_success "${target_env} 환경 프론트엔드 파일 배포 완료"
    else
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1
        log_error "${target_env} 환경 프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 권한 설정
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs
        sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH}
        sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME
        sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH}
        sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/dist/*
        sudo find ${FRONTEND_REMOTE_PATH}/dist -type d -exec chmod 755 {} \\;
    " >/dev/null 2>&1

    log_success "${target_env} 환경 배포 완료"
}

# 타겟 서비스 시작
start_target_service() {
    local target_env=$1
    local target_service=$(get_service_for_env $target_env)
    local target_port=$(get_port_for_env $target_env)

    log_info "${target_env} 환경 서비스 시작 중..."

    # 기존 프로세스 정리
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo pkill -f 'defectapp.*--server.port=$target_port' || true" >/dev/null 2>&1
    sleep 3

    # 서비스 시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl start $target_service" >/dev/null 2>&1; then
        log_success "${target_env} 환경 서비스 시작 완료"

        # 헬스체크 수행
        if health_check $target_port; then
            return 0
        else
            log_error "${target_env} 환경 헬스체크 실패"
            return 1
        fi
    else
        log_error "${target_env} 환경 서비스 시작 실패"
        return 1
    fi
}

# 이전 환경 정리
cleanup_previous_env() {
    local previous_env=$1
    local previous_service=$(get_service_for_env $previous_env)

    log_info "${previous_env} 환경 정리 중..."

    # 이전 환경 서비스 중지 (선택사항)
    # 필요에 따라 주석 해제
    # ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl stop $previous_service" >/dev/null 2>&1

    log_success "${previous_env} 환경 정리 완료"
}

# 롤백 기능
rollback() {
    local current_env=$1
    local previous_env

    if [ "$current_env" = "blue" ]; then
        previous_env="green"
    else
        previous_env="blue"
    fi

    log_warning "롤백을 수행합니다: ${current_env} -> ${previous_env}"

    if switch_nginx_config $previous_env; then
        log_success "롤백 완료"
        return 0
    else
        log_error "롤백 실패"
        return 1
    fi
}

# 최종 상태 확인
final_status_check() {
    log_info "최종 상태 확인 중..."

    echo "📊 서버 상태:"

    # 포트 상태 확인
    for port in 8080 8081; do
        local port_status="❌"
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :$port > /dev/null 2>&1"; then
            port_status="✅"
        fi

        local service_name
        if [ "$port" = "8080" ]; then
            service_name="qms-server1 (blue)"
        else
            service_name="qms-server2 (green)"
        fi

        local service_status
        service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $(get_service_for_env $([ $port -eq 8080 ] && echo 'blue' || echo 'green')) 2>/dev/null || echo 'inactive'")

        echo "  - $service_name: 포트 $port_status, 서비스 $service_status"
    done

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
    echo "🔄 블루-그린 배포 시작"
    echo "=============================================="

    # 연결 테스트
    test_ssh_connection

    # 현재 활성 환경 확인
    local current_env=$(get_current_active_env)
    local target_env=$(get_target_env $current_env)

    log_info "현재 활성 환경: ${current_env}"
    log_info "배포 대상 환경: ${target_env}"

    echo ""
    echo "==== [1/6] 병렬 빌드 시작 🔨 ===="

    # 병렬 빌드
    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    echo "==== [2/6] ${target_env} 환경 배포 📤 ===="
    deploy_to_target_env $target_env

    echo ""
    echo "==== [3/6] ${target_env} 환경 서비스 시작 🚀 ===="
    if ! start_target_service $target_env; then
        log_error "${target_env} 환경 시작 실패. 배포를 중단합니다."
        exit 1
    fi

    echo ""
    echo "==== [4/6] 트래픽 전환 🔄 ===="
    if ! switch_nginx_config $target_env; then
        log_error "트래픽 전환 실패. 롤백을 시도합니다."
        rollback $target_env
        exit 1
    fi

    echo ""
    echo "==== [5/6] 안정화 대기 ⏳ ===="
    log_info "서비스 안정화 대기 중... (15초)"
    sleep 15

    # 최종 확인
    if ! final_status_check; then
        log_warning "서비스 확인에 문제가 있습니다. 롤백하시겠습니까? (y/N)"
        read -r response
        if [ "$response" = "y" ] || [ "$response" = "Y" ]; then
            rollback $target_env
            exit 1
        fi
    fi

    echo ""
    echo "==== [6/6] 이전 환경 정리 🧹 ===="
    cleanup_previous_env $current_env

    log_success "🎉 블루-그린 배포 완료! 활성 환경: ${target_env}"
}

# 스크립트 실행
main "$@"