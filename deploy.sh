#!/bin/bash

set -e

export TZ=Asia/Seoul


# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

log_step() {
    echo -e "${PURPLE}🚀 $1${NC}"
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
BACKUP_PATH="/var/www/qms/backups"

# 현재 nginx가 가리키는 포트 확인
get_current_active_port() {
    local nginx_config
    nginx_config=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "grep -E 'proxy_pass.*:80[0-9]+' /etc/nginx/sites-available/qms 2>/dev/null | head -1" || echo "")

    if echo "$nginx_config" | grep -q ":8080"; then
        echo "8080"
    elif echo "$nginx_config" | grep -q ":8081"; then
        echo "8081"
    else
        echo "8080"  # 기본값
    fi
}

# 타겟 포트 결정 (현재 포트의 반대)
get_target_port() {
    local current_port=$1
    if [ "$current_port" = "8080" ]; then
        echo "8081"
    else
        echo "8080"
    fi
}

# 포트별 서비스명 매핑
get_service_name() {
    local port=$1
    if [ "$port" = "8080" ]; then
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

# 현재 상태 확인
check_current_status() {
    local current_port=$(get_current_active_port)
    local current_service=$(get_service_name $current_port)
    local target_port=$(get_target_port $current_port)
    local target_service=$(get_service_name $target_port)

    log_info "현재 배포 상태 확인 중..."

    echo "  🔄 현재 nginx → 포트 $current_port ($current_service)"
    echo "  🎯 배포 대상 → 포트 $target_port ($target_service)"

    # 현재 활성 서비스 상태
    local current_status
    current_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $current_service 2>/dev/null || echo 'inactive'")
    echo "  📊 현재 서비스: $current_status"

    # 타겟 서비스 상태
    local target_status
    target_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $target_service 2>/dev/null || echo 'inactive'")
    echo "  📊 대상 서비스: $target_status"
}

# 헬스체크
health_check() {
    local port=$1
    local max_attempts=20
    local attempt=0

    log_info "포트 ${port}에서 헬스체크 시작..."

    while [ $attempt -lt $max_attempts ]; do
        # Spring Boot Actuator health endpoint 확인
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 http://localhost:${port}/actuator/health | grep -q 'UP' 2>/dev/null"; then
            log_success "헬스체크 성공 (포트: ${port}, 시도: $((attempt + 1)))"
            return 0
        fi

        # Actuator가 없다면 단순 포트 체크
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :${port} > /dev/null 2>&1"; then
            log_success "포트 ${port} 응답 확인 (시도: $((attempt + 1)))"
            return 0
        fi

        attempt=$((attempt + 1))
        echo -n "."
        sleep 3
    done

    echo ""
    log_error "포트 ${port} 헬스체크 실패"
    return 1
}

# nginx 포트 스위칭
switch_nginx_port() {
    local target_port=$1
    local target_service=$(get_service_name $target_port)

    log_info "nginx 포트를 ${target_port}로 전환 중... (서비스: ${target_service})"

    # nginx 설정 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp /etc/nginx/sites-available/qms /etc/nginx/sites-available/qms.backup.\$(TZ=Asia/Seoul date +%Y%m%d_%H%M%S)" >/dev/null 2>&1

    # nginx 설정에서 proxy_pass 포트 변경
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo sed -i 's/proxy_pass http:\/\/localhost:[0-9]\+/proxy_pass http:\/\/localhost:${target_port}/g' /etc/nginx/sites-available/qms
    " >/dev/null 2>&1

    # nginx 설정 테스트
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -t" >/dev/null 2>&1; then
        # nginx 리로드
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -s reload" >/dev/null 2>&1; then
            log_success "nginx 포트 전환 완료: → ${target_port}"
            return 0
        else
            log_error "nginx reload 실패"
            return 1
        fi
    else
        log_error "nginx 설정 테스트 실패. 설정을 롤백합니다."
        # 가장 최근 백업으로 롤백
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            sudo cp \$(ls -t /etc/nginx/sites-available/qms.backup.* 2>/dev/null | head -1) /etc/nginx/sites-available/qms 2>/dev/null || true
            sudo nginx -s reload
        " >/dev/null 2>&1
        return 1
    fi
}

# 롤백 기능 (이전 포트로 되돌리기)
rollback() {
    local current_port=$(get_current_active_port)
    local previous_port=$(get_target_port $current_port)  # 현재의 반대가 이전 포트

    log_warning "포트 ${previous_port}로 롤백을 시도합니다..."

    if switch_nginx_port $previous_port; then
        log_success "롤백 완료: 포트 ${previous_port}로 전환됨"
        return 0
    else
        log_error "롤백 실패"
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

    # 의존성 관리 (캐시 최적화)
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
        log_info "의존성 설치 중..."
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

    # 파일 존재 확인
    if [ ! -f "backend/build/libs/$JAR_NAME" ]; then
        log_error "JAR 파일을 찾을 수 없습니다: backend/build/libs/$JAR_NAME"
        exit 1
    fi

    if [ ! -d "frontend/dist" ]; then
        log_error "프론트엔드 빌드 디렉토리를 찾을 수 없습니다: frontend/dist"
        exit 1
    fi

    # 백엔드 파일 배포
    log_info "백엔드 파일 업로드 중..."
    if rsync -az --timeout=30 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/; then
        log_success "백엔드 파일 업로드 완료"
    else
        log_error "백엔드 파일 업로드 실패"
        exit 1
    fi

    # 프론트엔드 무중단 배포 (경로 문제 해결)
    log_info "프론트엔드 파일 배포 중..."

    # 부모 디렉토리 레벨에서 임시 디렉토리 생성
    TEMP_FRONTEND_PATH="/var/www/qms/frontend/dist_temp"
    BACKUP_FRONTEND_PATH="/var/www/qms/frontend/dist_old"

    # 서버에서 기존 임시/백업 디렉토리 정리 및 새로 생성
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        echo '기존 임시/백업 디렉토리 정리...'
        rm -rf ${TEMP_FRONTEND_PATH} ${BACKUP_FRONTEND_PATH}

        # dist 내부의 잘못된 임시 디렉토리도 정리
        if [ -d ${FRONTEND_REMOTE_PATH}/dist_temp ]; then
            echo '기존 dist 내부 임시 디렉토리 정리...'
            rm -rf ${FRONTEND_REMOTE_PATH}/dist_temp
        fi

        mkdir -p ${TEMP_FRONTEND_PATH}
        echo '임시 디렉토리 준비 완료: ${TEMP_FRONTEND_PATH}'
        ls -la /var/www/qms/frontend/
    " >/dev/null 2>&1

    if rsync -az --timeout=30 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${TEMP_FRONTEND_PATH}/; then
        # 원자적 교체 (기존 → 백업, 임시 → 활성)
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            echo '파일 교체 시작...'

            if [ -d ${FRONTEND_REMOTE_PATH} ]; then
                echo '기존 dist를 dist_old로 백업'
                mv ${FRONTEND_REMOTE_PATH} ${BACKUP_FRONTEND_PATH}
            fi

            echo '새 파일을 dist로 이동'
            mv ${TEMP_FRONTEND_PATH} ${FRONTEND_REMOTE_PATH}

            echo '이전 백업 정리'
            rm -rf ${BACKUP_FRONTEND_PATH}

            echo '파일 교체 완료, 최종 상태:'
            ls -la /var/www/qms/frontend/

            # dist 내부 구조 확인
            echo 'dist 내부 구조:'
            ls -la ${FRONTEND_REMOTE_PATH}/
        " >/dev/null 2>&1

        log_success "프론트엔드 파일 배포 완료"
    else
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 권한 설정 및 nginx 캐시 클리어
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs
        sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH}
        sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME
        sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH}
        sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/*
        sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \\;

        # nginx 캐시 클리어 (정적 파일 캐싱 문제 해결)
        if [ -d /var/cache/nginx ]; then
            echo 'nginx 캐시 클리어...'
            sudo rm -rf /var/cache/nginx/*
        fi

        # nginx 설정 리로드
        sudo nginx -s reload

        echo '권한 설정 및 캐시 클리어 완료'
    " >/dev/null 2>&1

    log_success "파일 배포 및 권한 설정 완료"
}

# 타겟 서비스 시작
start_target_service() {
    local target_port=$1
    local target_service=$(get_service_name $target_port)

    log_info "${target_service} (포트: ${target_port}) 시작 중..."

    # 타겟 포트의 기존 프로세스 정리
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo pkill -f 'defectapp.*--server.port=${target_port}' || true" >/dev/null 2>&1

    sleep 3

    # 서비스 시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl start $target_service" >/dev/null 2>&1; then
        log_success "${target_service} 시작 완료"

        # 헬스체크 수행
        if health_check $target_port; then
            return 0
        else
            log_error "${target_service} 헬스체크 실패"
            return 1
        fi
    else
        log_error "${target_service} 시작 실패"
        return 1
    fi
}

# 이전 서비스 정리 (선택사항)
cleanup_previous_service() {
    local previous_port=$1
    local previous_service=$(get_service_name $previous_port)

    log_info "${previous_service} (포트: ${previous_port}) 정리 중..."

    # 이전 서비스 중지 (리소스 절약을 위해)
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl stop $previous_service" >/dev/null 2>&1 || true

    log_success "${previous_service} 정리 완료"
}

# 최종 상태 확인
final_status_check() {
    local current_port=$(get_current_active_port)

    log_info "배포 결과 확인 중..."

    # 현재 활성 서비스 상태 확인
    local current_service=$(get_service_name $current_port)
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $current_service 2>/dev/null || echo 'inactive'")

    local port_status="❌"
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :$current_port > /dev/null 2>&1"; then
        port_status="✅"
    fi

    echo "  📊 활성 서비스: $current_service (포트: $current_port)"
    echo "  📊 서비스 상태: $service_status, 포트 상태: $port_status"

    # 외부 접속 테스트
    log_info "외부 접속 테스트 중..."
    local external_status
    external_status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '연결실패')

    echo "  🌐 외부 접속: $external_status"

    if [ "$external_status" = "200" ] || [ "$external_status" = "401" ] || [ "$external_status" = "403" ]; then
        log_success "✨ 포트 스위칭 배포 성공!"
        return 0
    else
        log_warning "⚠️  배포는 완료되었지만 외부 접속 확인 필요 (응답: $external_status)"
        return 1
    fi
}

# 배포 시간 측정 시작
start_time=$(date +%s)

# 메인 배포 로직
main() {
    echo "================================================"
    echo "🔄 QMS 배포 시작"
    echo "📅 시작 시간: $(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S')"
    echo "================================================"

    # 현재 상태 확인
    test_ssh_connection

    local current_port=$(get_current_active_port)
    local target_port=$(get_target_port $current_port)

    log_step "STEP 0: 배포 계획 확인"
    check_current_status

    echo ""
    log_step "STEP 1: 소스코드 빌드 (병렬 처리)"

    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    log_step "STEP 2: 서버에 파일 배포"
    deploy_files

    echo ""
    log_step "STEP 3: 새로운 서비스 시작 (포트: ${target_port})"
    if ! start_target_service $target_port; then
        log_error "새 서비스 시작 실패! 배포를 중단합니다."
        exit 1
    fi

    echo ""
    log_step "STEP 4: nginx 트래픽 전환"
    if ! switch_nginx_port $target_port; then
        log_error "트래픽 전환 실패! 롤백을 시도합니다."
        rollback
        exit 1
    fi

    echo ""
    log_step "STEP 5: 서비스 안정화 대기 (10초)"
    sleep 10

    echo ""
    log_step "STEP 6: 배포 결과 확인"
    if ! final_status_check; then
        log_warning "서비스 확인에 문제가 있습니다. 롤백하시겠습니까? (y/N)"
        read -r -t 30 response || response="n"  # 30초 타임아웃
        if [ "$response" = "y" ] || [ "$response" = "Y" ]; then
            rollback
            exit 1
        fi
    fi

    echo ""
    log_step "STEP 7: 이전 서비스 정리"
    cleanup_previous_service $current_port

    # 배포 시간 계산
    end_time=$(date +%s)
    duration=$((end_time - start_time))

    echo ""
    echo "================================================"
    log_success "🎉 배포 완료!"
    echo "🔄 활성 포트: ${current_port} → ${target_port}"
    echo "⏱️  소요 시간: ${duration}초"
    echo "📅 완료 시간: $(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S')"
    echo "🔗 서비스 URL: https://qms.jaemin.app"
    echo "================================================"
}

# 인터럽트 처리
trap 'log_error "배포가 중단되었습니다. 서비스 상태를 확인해주세요."; exit 1' INT TERM

# 스크립트 실행
main "$@"