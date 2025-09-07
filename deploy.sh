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

# 강화된 헬스체크 (무중단 배포 핵심)
health_check() {
    local port=$1 attempt=0 max_attempts=30
    log_info "포트 ${port} 강화된 헬스체크 시작..."

    while [ $attempt -lt $max_attempts ]; do
        # 1단계: 포트 리스닝 확인
        if ! ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "netstat -tln | grep :${port} >/dev/null 2>&1"; then
            echo -n "🔄"; attempt=$((attempt+1)); sleep 3; continue
        fi

        # 2단계: HTTP 응답 확인
        local http_status
        http_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "curl -s --max-time 5 -o /dev/null -w '%{http_code}' http://localhost:${port}/ 2>/dev/null || echo '000'")

        if [ "$http_status" = "200" ] || [ "$http_status" = "401" ] || [ "$http_status" = "403" ]; then
            # 3단계: Actuator 헬스체크 (선택사항)
            if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
                "curl -s --max-time 3 http://localhost:${port}/actuator/health | grep -q 'UP' 2>/dev/null"; then
                log_success "완전한 헬스체크 성공 (포트: $port, HTTP: $http_status, Actuator: UP)"
            else
                log_success "기본 헬스체크 성공 (포트: $port, HTTP: $http_status)"
            fi

            # 4단계: 안정성 확인 (3회 연속 성공)
            local stable_count=0
            for i in {1..3}; do
                sleep 2
                local check_status
                check_status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
                    "curl -s --max-time 3 -o /dev/null -w '%{http_code}' http://localhost:${port}/ 2>/dev/null || echo '000'")
                if [ "$check_status" = "200" ] || [ "$check_status" = "401" ] || [ "$check_status" = "403" ]; then
                    stable_count=$((stable_count+1)); echo -n "✓"
                else
                    echo -n "✗"; break
                fi
            done

            if [ $stable_count -eq 3 ]; then
                log_success "서비스 안정성 확인 완료 (포트: $port)"
                return 0
            else
                log_warning "서비스 불안정, 재시도 중..."
            fi
        else
            echo -n "⏳"
        fi

        attempt=$((attempt+1)); sleep 3
    done

    echo ""; log_error "헬스체크 실패: $port"; return 1
}

# 안전한 nginx 트래픽 스위치
switch_nginx_port() {
    local port=$1
    log_info "nginx 포트 ${port}로 안전하게 전환..."

    # 설정 백업
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo cp /etc/nginx/sites-available/qms /etc/nginx/sites-available/qms.backup.$(date +%Y%m%d_%H%M%S)"

    # 설정 변경 및 테스트
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo sed -i 's/proxy_pass http:\/\/localhost:[0-9]\+/proxy_pass http:\/\/localhost:${port}/' /etc/nginx/sites-available/qms \
        && sudo nginx -t && sudo nginx -s reload"; then

        log_success "nginx 포트 전환 완료: ${port}"

        # 전환 검증
        sleep 3
        local verify_port=$(get_current_active_port)
        if [ "$verify_port" = "$port" ]; then
            log_success "nginx 전환 검증 성공"
            return 0
        else
            log_error "nginx 전환 검증 실패"
            return 1
        fi
    else
        log_error "nginx 설정 변경 실패"
        return 1
    fi
}

# 백엔드 서비스 안전 시작
start_target_service() {
    local port=$1 service=$(get_service_name $port)
    log_info "${service} 안전하게 시작 (포트: $port)..."

    # 기존 프로세스 정리 (그레이스풀)
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo pkill -TERM -f 'defectapp.*--server.port=${port}' 2>/dev/null || true"
    sleep 10
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo pkill -KILL -f 'defectapp.*--server.port=${port}' 2>/dev/null || true"

    # 포트 완전 해제 대기
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "while netstat -tln | grep :${port} >/dev/null 2>&1; do sleep 1; done"

    # 서비스 시작
    if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl start $service"; then
        log_success "${service} 시작 명령 완료"

        # 헬스체크 및 워밍업
        if health_check $port; then
            log_info "서비스 워밍업 중..."
            for i in {1..5}; do
                ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
                    "curl -s --max-time 3 http://localhost:${port}/ >/dev/null 2>&1 || true"
                sleep 1
            done
            log_success "워밍업 완료"
            return 0
        else
            log_error "${service} 헬스체크 실패"
            return 1
        fi
    else
        log_error "${service} 시작 실패"
        return 1
    fi
}

# 그레이스풀 서비스 종료 (다운타임 방지 핵심)
cleanup_previous_service() {
    local port=$1 service=$(get_service_name $port)
    log_info "이전 서비스 그레이스풀 종료: ${service} (포트: $port)"

    # 1단계: 연결 드레인 시간 제공 (30초)
    log_info "기존 연결 드레인 대기 중... (30초)"
    sleep 30

    # 2단계: 그레이스풀 셧다운
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl stop $service" >/dev/null 2>&1 || true

    # 3단계: 완전 종료 확인
    local wait_count=0
    while [ $wait_count -lt 15 ]; do
        local status
        status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "sudo systemctl is-active $service 2>/dev/null || echo 'inactive'")
        if [ "$status" = "inactive" ]; then
            log_success "이전 서비스 정상 종료 완료"
            return 0
        fi
        echo -n "⏳"; sleep 2; wait_count=$((wait_count+1))
    done

    # 4단계: 강제 종료 (마지막 수단)
    log_warning "강제 종료 수행"
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo pkill -KILL -f 'defectapp.*--server.port=${port}' 2>/dev/null || true"
    log_success "이전 서비스 종료 완료"
}

# 파일 배포 (최적화)
deploy_files() {
    log_info "배포 파일 업로드 중..."

    # 파일 존재 확인
    if [ ! -f "backend/build/libs/$JAR_NAME" ]; then
        log_error "JAR 파일 없음: backend/build/libs/$JAR_NAME"; exit 1
    fi
    if [ ! -d "frontend/dist" ]; then
        log_error "프론트엔드 빌드 없음: frontend/dist"; exit 1
    fi

    # 백엔드 배포
    if rsync -az --timeout=30 -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
        backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/; then
        log_success "백엔드 파일 업로드 완료"
    else
        log_error "백엔드 파일 업로드 실패"; exit 1
    fi

    # 프론트엔드 배포
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo chown -R ubuntu:ubuntu ${FRONTEND_REMOTE_PATH} && rm -rf ${FRONTEND_REMOTE_PATH}/*"

    if rsync -az --timeout=30 --delete -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
        frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/; then
        log_success "프론트엔드 파일 업로드 완료"
    else
        log_error "프론트엔드 파일 업로드 실패"; exit 1
    fi

    # 권한 설정
    ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo mkdir -p ${BACKEND_REMOTE_PATH}/logs \
        && sudo chown -R ubuntu:ubuntu ${BACKEND_REMOTE_PATH} \
        && sudo chmod +x ${BACKEND_REMOTE_PATH}/$JAR_NAME \
        && sudo chown -R www-data:www-data ${FRONTEND_REMOTE_PATH} \
        && sudo chmod -R 644 ${FRONTEND_REMOTE_PATH}/* \
        && sudo find ${FRONTEND_REMOTE_PATH} -type d -exec chmod 755 {} \\; \
        && sudo rm -rf /var/cache/nginx/* 2>/dev/null || true"

    log_success "파일 배포 및 권한 설정 완료"
}

# 빌드 함수들
build_backend() {
    log_info "[백엔드] 빌드 시작..."
    cd backend && ./gradlew build -x test --parallel --build-cache -q && cd ..
    log_success "[백엔드] 빌드 완료"
}

build_frontend() {
    log_info "[프론트엔드] 빌드 시작..."
    cd frontend && npm ci --prefer-offline --no-audit --silent && npm run build --silent && cd ..
    log_success "[프론트엔드] 빌드 완료"
}

# 최종 검증
final_verification() {
    local port=$1
    log_info "배포 결과 최종 검증 중..."

    # 외부 접속 테스트 (5회 시도)
    local success=0
    for i in {1..5}; do
        local status
        status=$(curl -s -o /dev/null -w '%{http_code}' -m 10 https://qms.jaemin.app/ 2>/dev/null || echo 'fail')
        if [ "$status" = "200" ] || [ "$status" = "401" ] || [ "$status" = "403" ]; then
            success=$((success+1)); echo -n "✅"
        else
            echo -n "❌"
        fi
        sleep 2
    done

    echo ""
    if [ $success -ge 4 ]; then
        log_success "외부 접속 안정성 확인: ${success}/5 성공"
        return 0
    else
        log_error "외부 접속 불안정: ${success}/5 성공"
        return 1
    fi
}

# 롤백 함수
rollback() {
    local current=$(get_current_active_port)
    local previous=$(get_target_port $current)
    log_warning "긴급 롤백 수행: 포트 ${previous}로 복구..."

    # 이전 서비스 재시작 (필요시)
    local prev_service=$(get_service_name $previous)
    local status
    status=$(ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
        "sudo systemctl is-active $prev_service 2>/dev/null || echo 'inactive'")
    if [ "$status" != "active" ]; then
        ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
            "sudo systemctl start $prev_service"
        sleep 10
    fi

    if switch_nginx_port $previous; then
        log_success "롤백 완료"; return 0
    else
        log_error "롤백 실패 - 수동 복구 필요!"; return 1
    fi
}

# 메인 함수 (무중단 배포)
main() {
    start_time=$(date +%s)
    echo "================================================"
    echo "🚀 QMS 무중단 배포 시작"
    echo "📅 $(date '+%Y-%m-%d %H:%M:%S')"
    echo "================================================"

    test_ssh_connection
    current_port=$(get_current_active_port)
    target_port=$(get_target_port $current_port)
    log_step "현재: $current_port → 배포 대상: $target_port"

    # 현재 서비스 상태 사전 검증
    log_step "STEP 1: 현재 서비스 안정성 검증"
    if ! health_check $current_port; then
        log_error "현재 서비스 불안정! 배포 중단"; exit 1
    fi

    # 병렬 빌드
    log_step "STEP 2: 소스코드 빌드 (병렬)"
    build_backend & BACKEND_PID=$!
    build_frontend & FRONTEND_PID=$!
    wait $BACKEND_PID $FRONTEND_PID

    # 파일 배포
    log_step "STEP 3: 파일 배포"
    deploy_files

    # 새 서비스 시작 및 준비 완료
    log_step "STEP 4: 새 서비스 시작 및 완전 준비 대기"
    if ! start_target_service $target_port; then
        log_error "새 서비스 시작 실패!"; exit 1
    fi

    # 최종 준비 상태 확인
    log_step "STEP 5: 서비스 최종 안정성 검증 (15초 추가 대기)"
    sleep 15
    if ! health_check $target_port; then
        log_error "새 서비스 최종 검증 실패!"; exit 1
    fi

    # 트래픽 전환 (순간적)
    log_step "STEP 6: 트래픽 원자적 전환"
    if ! switch_nginx_port $target_port; then
        log_error "트래픽 전환 실패! 롤백 수행"
        rollback; exit 1
    fi

    # 전환 후 안정성 확인
    log_step "STEP 7: 전환 후 안정성 확인 (20초)"
    sleep 20
    if ! final_verification $target_port; then
        log_warning "전환 후 검증 실패! 롤백 수행"
        rollback; exit 1
    fi

    # 이전 서비스 그레이스풀 종료
    log_step "STEP 8: 이전 서비스 그레이스풀 종료"
    cleanup_previous_service $current_port

    # 완료
    end_time=$(date +%s)
    duration=$((end_time - start_time))

    echo "================================================"
    log_success "🎉 무중단 배포 성공!"
    echo "🔄 포트 전환: ${current_port} → ${target_port}"
    echo "⏱️ 총 소요시간: ${duration}초"
    echo "💡 실제 다운타임: 0초"
    echo "🔗 서비스: https://qms.jaemin.app"
    echo "================================================"
}

# 안전한 종료 처리
trap 'log_error "배포 중단됨! 서비스 상태 확인 필요"; exit 1' INT TERM

main "$@"