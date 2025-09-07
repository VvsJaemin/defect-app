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

# 강화된 헬스체크 (애플리케이션 준비 상태 확인)
enhanced_health_check() {
    local port=$1
    local max_attempts=30  # 최대 90초 대기
    local attempt=0

    log_info "포트 ${port}에서 강화된 헬스체크 시작..."

    while [ $attempt -lt $max_attempts ]; do
        # 1. 포트 리스닝 확인
        if ! ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :${port} > /dev/null 2>&1"; then
            echo -n "🔄"
            attempt=$((attempt + 1))
            sleep 3
            continue
        fi

        # 2. HTTP 응답 확인 (200, 401, 403 모두 정상)
        local http_status
        http_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 5 -o /dev/null -w '%{http_code}' http://localhost:${port}/ 2>/dev/null || echo '000'")

        if [ "$http_status" = "200" ] || [ "$http_status" = "401" ] || [ "$http_status" = "403" ]; then
            # 3. Actuator health endpoint 확인 (있다면)
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 http://localhost:${port}/actuator/health | grep -q 'UP' 2>/dev/null"; then
                log_success "완전한 헬스체크 통과 (포트: ${port}, HTTP: ${http_status}, Actuator: UP, 시도: $((attempt + 1)))"
            else
                log_success "기본 헬스체크 통과 (포트: ${port}, HTTP: ${http_status}, 시도: $((attempt + 1)))"
            fi

            # 추가 안정성 확인을 위해 3번 연속 성공 확인
            local stability_check=0
            for i in {1..3}; do
                sleep 2
                local check_status
                check_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 -o /dev/null -w '%{http_code}' http://localhost:${port}/ 2>/dev/null || echo '000'")
                if [ "$check_status" = "200" ] || [ "$check_status" = "401" ] || [ "$check_status" = "403" ]; then
                    stability_check=$((stability_check + 1))
                    echo -n "✓"
                else
                    echo -n "✗"
                    break
                fi
            done

            if [ $stability_check -eq 3 ]; then
                log_success "서비스 안정성 확인 완료 (포트: ${port})"
                return 0
            else
                log_warning "서비스가 불안정합니다. 추가 대기 중..."
            fi
        else
            echo -n "⏳"
        fi

        attempt=$((attempt + 1))
        sleep 3
    done

    echo ""
    log_error "포트 ${port} 헬스체크 실패 (최대 시도 횟수 초과)"
    return 1
}

# nginx 포트 스위칭 (원자적 전환)
switch_nginx_port() {
    local target_port=$1
    local target_service=$(get_service_name $target_port)

    log_info "nginx 포트를 ${target_port}로 전환 중... (서비스: ${target_service})"

    # nginx 설정 백업
    local backup_name="qms.backup.$(TZ=Asia/Seoul date +%Y%m%d_%H%M%S)"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo cp /etc/nginx/sites-available/qms /etc/nginx/sites-available/${backup_name}" >/dev/null 2>&1

    # 새로운 설정 파일을 임시로 생성하고 테스트
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo cp /etc/nginx/sites-available/qms /tmp/qms.new
        sudo sed -i 's/proxy_pass http:\/\/localhost:[0-9]\+/proxy_pass http:\/\/localhost:${target_port}/g' /tmp/qms.new

        # 설정 테스트용 임시 복사
        sudo cp /tmp/qms.new /etc/nginx/sites-available/qms.test
    " >/dev/null 2>&1

    # nginx 설정 테스트
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -t -c /etc/nginx/nginx.conf" >/dev/null 2>&1; then
        # 원자적으로 설정 교체
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            sudo mv /tmp/qms.new /etc/nginx/sites-available/qms
            sudo rm -f /etc/nginx/sites-available/qms.test
        " >/dev/null 2>&1

        # nginx 그레이스풀 리로드
        if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo nginx -s reload" >/dev/null 2>&1; then
            log_success "nginx 포트 전환 완료: → ${target_port}"

            # 전환 후 즉시 확인
            sleep 2
            local new_port=$(get_current_active_port)
            if [ "$new_port" = "$target_port" ]; then
                log_success "nginx 전환 검증 성공"
                return 0
            else
                log_error "nginx 전환 검증 실패 (예상: ${target_port}, 실제: ${new_port})"
                return 1
            fi
        else
            log_error "nginx reload 실패"
            return 1
        fi
    else
        log_error "nginx 설정 테스트 실패. 설정을 롤백합니다."
        # 백업으로 롤백
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
            sudo cp /etc/nginx/sites-available/${backup_name} /etc/nginx/sites-available/qms 2>/dev/null || true
            sudo rm -f /etc/nginx/sites-available/qms.test /tmp/qms.new
            sudo nginx -s reload
        " >/dev/null 2>&1
        return 1
    fi
}

# 롤백 기능 (이전 포트로 되돌리기)
rollback() {
    local current_port=$(get_current_active_port)
    local previous_port=$(get_target_port $current_port)

    log_warning "긴급 롤백: 포트 ${previous_port}로 되돌립니다..."

    # 이전 서비스가 살아있는지 확인
    local previous_service=$(get_service_name $previous_port)
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $previous_service 2>/dev/null || echo 'inactive'")

    if [ "$service_status" != "active" ]; then
        log_warning "이전 서비스가 비활성 상태입니다. 재시작을 시도합니다..."
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl start $previous_service" >/dev/null 2>&1
        sleep 5
    fi

    if switch_nginx_port $previous_port; then
        log_success "롤백 완료: 포트 ${previous_port}로 전환됨"
        return 0
    else
        log_error "롤백 실패 - 수동 복구가 필요합니다!"
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

# 파일 배포 (권한 문제 해결)
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

    # 프론트엔드 파일 직접 배포 (권한 문제 해결)
    log_info "프론트엔드 파일 배포 중..."

    # 먼저 권한을 ubuntu로 변경하고 기존 파일들 삭제
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        echo '프론트엔드 디렉토리 권한 변경 및 정리...'
        sudo chown -R ubuntu:ubuntu ${FRONTEND_REMOTE_PATH}
        sudo chmod -R 755 ${FRONTEND_REMOTE_PATH}
        rm -rf ${FRONTEND_REMOTE_PATH}/*
        echo '파일 정리 완료'
    " >/dev/null 2>&1

    # 새 파일들을 직접 업로드
    if rsync -az --timeout=30 --delete -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/; then
        log_success "프론트엔드 파일 배포 완료"
    else
        log_error "프론트엔드 파일 배포 실패"
        exit 1
    fi

    # 업로드 후 최종 권한 설정
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        # 백엔드 권한 설정
        sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs
        sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH}
        sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME

        # 프론트엔드 최종 권한 설정 (nginx용)
        sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH}
        sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/*
        sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \\;

        # nginx 캐시 클리어 (그레이스풀하게)
        if [ -d /var/cache/nginx ]; then
            sudo rm -rf /var/cache/nginx/* 2>/dev/null || true
        fi

        echo '권한 설정 및 캐시 클리어 완료'
    " >/dev/null 2>&1

    log_success "파일 배포 및 설정 완료"
}

# 타겟 서비스 시작 (완전한 준비 상태까지 대기)
start_target_service() {
    local target_port=$1
    local target_service=$(get_service_name $target_port)

    log_info "${target_service} (포트: ${target_port}) 준비 중..."

    # 타겟 포트의 기존 프로세스 정리 (그레이스풀)
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        # 기존 프로세스에 SIGTERM 전송
        sudo pkill -TERM -f 'defectapp.*--server.port=${target_port}' 2>/dev/null || true

        # 10초 대기 후 강제 종료
        sleep 10
        sudo pkill -KILL -f 'defectapp.*--server.port=${target_port}' 2>/dev/null || true

        # 포트 완전히 해제될 때까지 대기
        while netstat -tln | grep :${target_port} >/dev/null 2>&1; do
            echo 'Waiting for port ${target_port} to be free...'
            sleep 2
        done
    " >/dev/null 2>&1

    log_info "${target_service} 시작 중..."

    # 서비스 시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl start $target_service" >/dev/null 2>&1; then
        log_success "${target_service} 시작 명령 완료"

        # 강화된 헬스체크 수행
        if enhanced_health_check $target_port; then
            log_success "${target_service} 완전 준비 완료"

            # 워밍업을 위한 추가 요청
            log_info "서비스 워밍업 중..."
            for i in {1..5}; do
                ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 3 http://localhost:${target_port}/ >/dev/null 2>&1 || true"
                sleep 1
            done
            log_success "워밍업 완료"

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

# 그레이스풀 서비스 종료 (연결 드레인)
graceful_shutdown_service() {
    local port=$1
    local service=$(get_service_name $port)

    log_info "${service} (포트: ${port}) 그레이스풀 종료 시작..."

    # 1단계: 새로운 연결 차단을 위해 헬스체크 실패하도록 설정 (가능하다면)
    # Spring Boot Actuator shutdown endpoint가 있다면 활용
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        curl -s -X POST --max-time 5 http://localhost:${port}/actuator/shutdown 2>/dev/null || true
    " >/dev/null 2>&1

    # 2단계: 기존 연결이 정리될 시간 제공
    log_info "기존 연결 드레인 중... (30초 대기)"
    sleep 30

    # 3단계: SIGTERM으로 그레이스풀 종료 시도
    log_info "그레이스풀 종료 신호 전송..."
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo systemctl stop $service
    " >/dev/null 2>&1

    # 4단계: 완전히 종료될 때까지 대기 (최대 30초)
    local wait_count=0
    while [ $wait_count -lt 15 ]; do
        local service_status
        service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $service 2>/dev/null || echo 'inactive'")

        if [ "$service_status" = "inactive" ]; then
            log_success "${service} 정상 종료 완료"
            return 0
        fi

        echo -n "⏳"
        sleep 2
        wait_count=$((wait_count + 1))
    done

    echo ""
    log_warning "${service} 정상 종료 시간 초과, 강제 종료 수행"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "
        sudo pkill -KILL -f 'defectapp.*--server.port=${port}' 2>/dev/null || true
    " >/dev/null 2>&1

    log_success "${service} 종료 완료"
}

# 최종 상태 확인 (더 포괄적인 검증)
comprehensive_status_check() {
    local current_port=$(get_current_active_port)

    log_info "종합적인 배포 결과 검증 중..."

    # 1. nginx 설정 확인
    local nginx_port_check
    nginx_port_check=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "grep -E 'proxy_pass.*:${current_port}' /etc/nginx/sites-available/qms | wc -l")

    if [ "$nginx_port_check" -gt 0 ]; then
        log_success "✅ nginx 설정 확인: 포트 ${current_port}"
    else
        log_error "❌ nginx 설정 불일치"
        return 1
    fi

    # 2. 서비스 상태 확인
    local current_service=$(get_service_name $current_port)
    local service_status
    service_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "sudo systemctl is-active $current_service 2>/dev/null || echo 'inactive'")

    if [ "$service_status" = "active" ]; then
        log_success "✅ 서비스 상태: $current_service ($service_status)"
    else
        log_error "❌ 서비스 상태 이상: $service_status"
        return 1
    fi

    # 3. 포트 리스닝 확인
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "netstat -tln | grep :$current_port > /dev/null 2>&1"; then
        log_success "✅ 포트 리스닝: ${current_port}"
    else
        log_error "❌ 포트 리스닝 실패: ${current_port}"
        return 1
    fi

    # 4. 애플리케이션 응답 확인
    local app_status
    app_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "curl -s --max-time 5 -o /dev/null -w '%{http_code}' http://localhost:${current_port}/ 2>/dev/null || echo '000'")

    if [ "$app_status" = "200" ] || [ "$app_status" = "401" ] || [ "$app_status" = "403" ]; then
        log_success "✅ 애플리케이션 응답: HTTP ${app_status}"
    else
        log_error "❌ 애플리케이션 응답 실패: HTTP ${app_status}"
        return 1
    fi

    # 5. 외부 접속 테스트 (여러 번 시도)
    log_info "외부 접속 안정성 테스트 중..."
    local success_count=0

    for i in {1..5}; do
        local external_status
        external_status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo '연결실패')

        if [ "$external_status" = "200" ] || [ "$external_status" = "401" ] || [ "$external_status" = "403" ]; then
            success_count=$((success_count + 1))
            echo -n "✅"
        else
            echo -n "❌"
        fi
        sleep 2
    done

    echo ""
    if [ $success_count -ge 4 ]; then
        log_success "✅ 외부 접속 안정성: ${success_count}/5 성공"
        log_success "🎉 배포 검증 완료!"
        return 0
    else
        log_error "❌ 외부 접속 불안정: ${success_count}/5 성공"
        return 1
    fi
}

# 배포 시간 측정 시작
start_time=$(date +%s)

# 메인 배포 로직 (무중단 배포)
main() {
    echo "================================================"
    echo "🚀 QMS 무중단 배포 시작"
    echo "📅 시작 시간: $(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S')"
    echo "================================================"

    # 현재 상태 확인
    test_ssh_connection

    local current_port=$(get_current_active_port)
    local target_port=$(get_target_port $current_port)

    log_step "STEP 0: 배포 계획 및 사전 검증"
    check_current_status

    # 현재 서비스 상태 확인 (배포 전 안정성 검증)
    log_info "현재 서비스 안정성 사전 검증..."
    if ! enhanced_health_check $current_port; then
        log_error "현재 서비스가 불안정합니다. 배포를 중단합니다."
        exit 1
    fi

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
    log_step "STEP 3: 새로운 서비스 시작 및 준비 완료 대기"
    if ! start_target_service $target_port; then
        log_error "새 서비스 시작 실패! 배포를 중단합니다."
        exit 1
    fi

    echo ""
    log_step "STEP 4: 서비스 안정성 최종 검증"
    log_info "최종 안정성 검증을 위해 추가 대기 중... (15초)"
    sleep 15

    if ! enhanced_health_check $target_port; then
        log_error "새 서비스 최종 검증 실패! 배포를 중단합니다."
        exit 1
    fi

    echo ""
    log_step "STEP 5: 트래픽 전환 (원자적 스위칭)"
    if ! switch_nginx_port $target_port; then
        log_error "트래픽 전환 실패! 롤백을 시도합니다."
        rollback
        exit 1
    fi

    echo ""
    log_step "STEP 6: 전환 후 안정성 확인 (20초)"
    sleep 20

    if ! comprehensive_status_check; then
        log_error "전환 후 검증 실패! 롤백을 수행합니다."
        rollback
        exit 1
    fi

    echo ""
    log_step "STEP 7: 이전 서비스 그레이스풀 종료"
    graceful_shutdown_service $current_port

    # 배포 시간 계산
    end_time=$(date +%s)
    duration=$((end_time - start_time))

    echo ""
    echo "================================================"
    log_success "🎉 무중단 배포 성공!"
    echo "🔄 활성 포트: ${current_port} → ${target_port}"
    echo "⏱️  총 소요시간: ${duration}초"
    echo "📅 완료 시간: $(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S')"
    echo "🔗 서비스 URL: https://qms.jaemin.app"
    echo "💡 다운타임: 0초 (무중단 배포 완료)"
    echo "================================================"
}

# 인터럽트 처리 (안전한 종료)
cleanup_on_exit() {
    log_error "배포가 중단되었습니다. 안전한 정리를 수행합니다..."

    # 현재 상태 확인 후 필요시 롤백
    local current_port=$(get_current_active_port)
    log_info "현재 nginx 포트: ${current_port}"

    log_warning "서비스 상태를 확인하고 필요시 수동으로 복구해주세요."
    exit 1
}

trap 'cleanup_on_exit' INT TERM

# 스크립트 실행
main "$@"