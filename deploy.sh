#!/bin/bash

set -e

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

# 현재 서비스 상태 확인
check_current_status() {
    log_info "현재 서비스 상태 확인 중..."

    # 서비스 상태
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active qms-server1 2>/dev/null || echo 'inactive'")

    # 포트 상태
    local port_status="❌"
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :8080 > /dev/null 2>&1"; then
        port_status="✅"
    fi

    echo "  📊 현재 상태: 서비스 $service_status, 포트 8080 $port_status"
}

# 백업 생성
create_backup() {
    log_info "현재 버전 백업 생성 중..."
    local timestamp=$(date +%Y%m%d_%H%M%S)

    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo mkdir -p $BACKUP_PATH

        # JAR 파일 백업 (존재하는 경우에만)
        if [ -f $BACKEND_REMOTE_PATH/$JAR_NAME ]; then
            sudo cp $BACKEND_REMOTE_PATH/$JAR_NAME $BACKUP_PATH/${JAR_NAME}.backup.$timestamp
            echo 'Backend backup created'
        fi

        # 프론트엔드 백업 (존재하는 경우에만)
        if [ -d $FRONTEND_REMOTE_PATH ]; then
            sudo cp -r $FRONTEND_REMOTE_PATH $BACKUP_PATH/frontend_backup_$timestamp
            echo 'Frontend backup created'
        fi

        # 오래된 백업 정리 (7일 이상)
        find $BACKUP_PATH -name '*.backup.*' -mtime +7 -delete 2>/dev/null || true
        find $BACKUP_PATH -name 'frontend_backup_*' -mtime +7 -exec rm -rf {} + 2>/dev/null || true
    " >/dev/null 2>&1

    log_success "백업 생성 완료 (타임스탬프: $timestamp)"
}

# 헬스체크 (간단한 버전)
health_check() {
    local max_attempts=20  # 30초에서 20초로 단축
    local attempt=0

    log_info "서비스 헬스체크 시작 (최대 ${max_attempts}번 시도)..."

    while [ $attempt -lt $max_attempts ]; do
        # Spring Boot Actuator health endpoint 확인
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 http://localhost:8080/actuator/health | grep -q 'UP' 2>/dev/null"; then
            log_success "헬스체크 성공 (시도: $((attempt + 1)))"
            return 0
        fi

        # Actuator가 없다면 단순 포트 체크
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :8080 > /dev/null 2>&1"; then
            log_success "포트 8080 확인됨 (시도: $((attempt + 1)))"
            return 0
        fi

        attempt=$((attempt + 1))
        echo -n "."
        sleep 3
    done

    echo ""
    log_error "헬스체크 실패 - 서비스가 정상적으로 시작되지 않았습니다"
    return 1
}

# 롤백 기능
rollback() {
    log_warning "롤백을 시작합니다..."

    # 가장 최근 백업 찾기
    local latest_jar_backup
    local latest_frontend_backup

    latest_jar_backup=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "ls -t $BACKUP_PATH/*.backup.* 2>/dev/null | head -1" || echo "")
    latest_frontend_backup=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "ls -td $BACKUP_PATH/frontend_backup_* 2>/dev/null | head -1" || echo "")

    if [ -z "$latest_jar_backup" ] && [ -z "$latest_frontend_backup" ]; then
        log_error "백업 파일을 찾을 수 없습니다. 수동으로 복구해야 합니다."
        return 1
    fi

    # 서비스 중지
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl stop qms-server1" >/dev/null 2>&1 || true

    # JAR 파일 롤백
    if [ -n "$latest_jar_backup" ]; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp $latest_jar_backup $BACKEND_REMOTE_PATH/$JAR_NAME" >/dev/null 2>&1
        log_info "백엔드 롤백 완료"
    fi

    # 프론트엔드 롤백
    if [ -n "$latest_frontend_backup" ]; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            sudo rm -rf $FRONTEND_REMOTE_PATH
            sudo cp -r $latest_frontend_backup $FRONTEND_REMOTE_PATH
            sudo chown -R www-data:www-data $FRONTEND_REMOTE_PATH
        " >/dev/null 2>&1
        log_info "프론트엔드 롤백 완료"
    fi

    # 서비스 재시작
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl start qms-server1" >/dev/null 2>&1

    sleep 5
    if health_check; then
        log_success "롤백 성공!"
        return 0
    else
        log_error "롤백 후에도 서비스가 정상 동작하지 않습니다"
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

# 파일 배포 (무중단 배포)
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

    # 프론트엔드 무중단 배포 (원자적 교체)
    log_info "프론트엔드 파일 배포 중..."

    TEMP_FRONTEND_PATH="${FRONTEND_REMOTE_PATH}_temp"

    # 임시 디렉토리에 업로드
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH} && mkdir -p ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1

    if rsync -az --timeout=30 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${TEMP_FRONTEND_PATH}/; then
        # 원자적 교체
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            mv ${FRONTEND_REMOTE_PATH} ${FRONTEND_REMOTE_PATH}_old 2>/dev/null || true
            mv ${TEMP_FRONTEND_PATH} ${FRONTEND_REMOTE_PATH}
            rm -rf ${FRONTEND_REMOTE_PATH}_old
        " >/dev/null 2>&1

        log_success "프론트엔드 파일 배포 완료"
    else
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${TEMP_FRONTEND_PATH}" >/dev/null 2>&1
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 권한 설정
    log_info "파일 권한 설정 중..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs
        sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH}
        sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME
        sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH}
        sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/*
        sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \\;
    " >/dev/null 2>&1

    log_success "모든 파일 배포 및 권한 설정 완료"
}

# 서비스 재시작 (안전한 방식)
restart_service() {
    log_info "서비스 재시작 중..."

    # 기존 프로세스 정리
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo pkill -f 'defectapp' || true" >/dev/null 2>&1

    # 포트 해제 대기
    sleep 3

    # 서비스 재시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl restart qms-server1" >/dev/null 2>&1; then
        log_success "서비스 재시작 완료"
        return 0
    else
        log_error "서비스 재시작 실패"
        return 1
    fi
}

# nginx 캐시 클리어
clear_nginx_cache() {
    log_info "nginx 캐시 클리어 및 리로드..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo nginx -s reload
        # 필요시 캐시 디렉토리도 정리
        sudo rm -rf /var/cache/nginx/* 2>/dev/null || true
    " >/dev/null 2>&1 || log_warning "nginx reload 실패"
}

# 최종 상태 확인
final_status_check() {
    log_info "배포 결과 확인 중..."

    # 로컬 서비스 상태
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active qms-server1 2>/dev/null || echo 'inactive'")

    local port_status="❌"
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :8080 > /dev/null 2>&1"; then
        port_status="✅"
    fi

    echo "  📊 서비스 상태: $service_status, 포트 8080: $port_status"

    # 외부 접속 테스트
    log_info "외부 접속 테스트 중..."
    local external_status
    external_status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '연결실패')

    echo "  🌐 외부 접속: $external_status"

    if [ "$external_status" = "200" ]; then
        log_success "✨ 배포 성공! 서비스가 정상적으로 동작하고 있습니다."
        return 0
    elif [ "$external_status" = "401" ] || [ "$external_status" = "403" ]; then
        log_success "✨ 배포 성공! 서비스 동작 중 (인증 페이지 확인됨)"
        return 0
    else
        log_warning "⚠️  배포는 완료되었지만 외부 접속에 문제가 있을 수 있습니다 (응답: $external_status)"
        return 1
    fi
}

# 배포 시간 측정 시작
start_time=$(date +%s)

# 메인 배포 로직
main() {
    echo "================================================"
    echo "🚀 QMS 프로젝트 배포 시작"
    echo "📅 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "================================================"

    # 0. 사전 확인
    log_step "STEP 0: 사전 점검"
    test_ssh_connection
    check_current_status

    echo ""
    # 1. 백업 생성
    log_step "STEP 1: 현재 버전 백업"
    create_backup

    echo ""
    # 2. 병렬 빌드
    log_step "STEP 2: 소스코드 빌드 (병렬 처리)"

    build_backend &
    BACKEND_PID=$!

    build_frontend &
    FRONTEND_PID=$!

    # 빌드 완료 대기
    wait $BACKEND_PID
    wait $FRONTEND_PID

    echo ""
    # 3. 파일 배포
    log_step "STEP 3: 서버에 파일 배포"
    deploy_files

    echo ""
    # 4. 서비스 재시작
    log_step "STEP 4: 백엔드 서비스 재시작"
    if ! restart_service; then
        log_error "서비스 재시작 실패! 롤백을 시도합니다."
        rollback
        exit 1
    fi

    echo ""
    # 5. 헬스체크
    log_step "STEP 5: 서비스 상태 확인"
    if ! health_check; then
        log_error "헬스체크 실패! 롤백을 시도합니다."
        rollback
        exit 1
    fi

    echo ""
    # 6. 캐시 클리어
    log_step "STEP 6: 웹서버 캐시 클리어"
    clear_nginx_cache

    echo ""
    # 7. 최종 확인
    log_step "STEP 7: 배포 결과 확인"
    final_status_check

    # 배포 시간 계산
    end_time=$(date +%s)
    duration=$((end_time - start_time))

    echo ""
    echo "================================================"
    log_success "🎉 배포 완료!"
    echo "⏱️  소요 시간: ${duration}초"
    echo "📅 완료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "🔗 서비스 URL: https://qms.jaemin.app"
    echo "================================================"
}

# 인터럽트 처리 (Ctrl+C 등)
trap 'log_error "배포가 중단되었습니다. 서비스 상태를 확인해주세요."; exit 1' INT TERM

# 스크립트 실행
main "$@"