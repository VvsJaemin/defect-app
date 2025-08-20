#!/bin/bash

                                       set -e

                                       ENV_FILE="./deploy.env"

                                       if [ ! -f "$ENV_FILE" ]; then
                                         echo "❌ $ENV_FILE 파일이 없습니다."
                                         exit 1
                                       fi

                                       source "$ENV_FILE"

                                       BACKEND_REMOTE_PATH="/var/www/qms/backend"
                                       FRONTEND_REMOTE_PATH="/var/www/qms/frontend/dist"
                                       JAR_NAME="defectapp-0.0.1-SNAPSHOT.jar"

                                       # 병렬 빌드를 위한 함수들
                                       build_backend() {
                                           echo "🔨 [백엔드] 빌드 시작..."
                                           cd backend || { echo "❌ backend 디렉토리가 없습니다."; exit 1; }
                                           ./gradlew build -x test --parallel --build-cache || { echo "❌ 백엔드 빌드 실패"; exit 1; }
                                           cd ..
                                           echo "✅ [백엔드] 빌드 완료"
                                       }

                                       build_frontend() {
                                           echo "🔨 [프론트엔드] 빌드 시작..."
                                           cd frontend || { echo "❌ frontend 디렉토리가 없습니다."; exit 1; }

                                           # package-lock.json 체크섬으로 캐시 확인
                                           if [ -f "node_modules/.cache-timestamp" ] && [ -f "package-lock.json" ]; then
                                               LOCK_HASH=$(md5sum package-lock.json | cut -d' ' -f1)
                                               CACHED_HASH=$(cat node_modules/.cache-timestamp 2>/dev/null || echo "")

                                               if [ "$LOCK_HASH" != "$CACHED_HASH" ]; then
                                                   echo "📦 의존성 변경 감지, npm install 실행..."
                                                   npm ci --prefer-offline --no-audit
                                                   echo "$LOCK_HASH" > node_modules/.cache-timestamp
                                               else
                                                   echo "♻️  캐시된 node_modules 사용"
                                               fi
                                           else
                                               echo "📦 첫 설치, npm install 실행..."
                                               npm ci --prefer-offline --no-audit
                                               md5sum package-lock.json | cut -d' ' -f1 > node_modules/.cache-timestamp
                                           fi

                                           npm run build || { echo "❌ 프론트엔드 빌드 실패"; exit 1; }
                                           cd ..
                                           echo "✅ [프론트엔드] 빌드 완료"
                                       }

                                       # 무중단 배포를 위한 헬스체크 함수
                                       health_check() {
                                           local port=$1
                                           local max_attempts=30
                                           local attempt=1

                                           echo "🏥 포트 $port 헬스체크 시작..."

                                           while [ $attempt -le $max_attempts ]; do
                                               if ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} \
                                                  "curl -f http://localhost:$port/actuator/health > /dev/null 2>&1"; then
                                                   echo "✅ 포트 $port 서비스 준비 완료 (시도: $attempt/$max_attempts)"
                                                   return 0
                                               fi

                                               echo "⏳ 포트 $port 대기 중... ($attempt/$max_attempts)"
                                               sleep 2
                                               attempt=$((attempt + 1))
                                           done

                                           echo "❌ 포트 $port 헬스체크 실패"
                                           return 1
                                       }

                                       echo "==== [1/8] 병렬 빌드 시작 🚀 ===="
                                       build_backend &
                                       BACKEND_PID=$!

                                       build_frontend &
                                       FRONTEND_PID=$!

                                       wait $BACKEND_PID
                                       wait $FRONTEND_PID

                                       echo "==== [2/8] 백엔드 배포 📤 ===="
                                       rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
                                         backend/build/libs/$JAR_NAME ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ || {
                                           echo "❌ 백엔드 전송 실패"; exit 1;
                                       }

                                       echo "==== [2-1/8] 백엔드 JAR 권한 및 소유권 설정 ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
                                         sudo chown ubuntu:ubuntu ${BACKEND_REMOTE_PATH}/$JAR_NAME
                                         sudo chmod 755 ${BACKEND_REMOTE_PATH}/$JAR_NAME
                                       EOF

                                       echo "==== [3/8] 프론트엔드 배포 전 기존 파일 삭제 🗑️ ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${FRONTEND_REMOTE_PATH}/*" || {
                                         echo "❌ 기존 프론트엔드 파일 삭제 실패"; exit 1;
                                       }

                                       echo "==== [4/8] 프론트엔드 배포 📤 ===="
                                       rsync -avz -e "ssh -i $PEM_PATH -o StrictHostKeyChecking=no" \
                                         frontend/dist/ ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ || {
                                           echo "❌ 프론트엔드 전송 실패"; exit 1;
                                       }

                                       echo "==== [5/8] 무중단 배포 시작 - 서버1 (8080) 🔄 ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << 'EOF'
                                         # 서버1 graceful shutdown
                                         PID_8080=$(lsof -t -i:8080 2>/dev/null || echo "")
                                         if [ ! -z "$PID_8080" ]; then
                                           echo "🛑 서버1 (8080) 종료 중..."
                                           kill -TERM $PID_8080

                                           # 30초 대기
                                           for i in {1..30}; do
                                             if ! kill -0 $PID_8080 2>/dev/null; then
                                               break
                                             fi
                                             sleep 1
                                           done

                                           # 강제 종료가 필요한 경우
                                           if kill -0 $PID_8080 2>/dev/null; then
                                             echo "⚠️ 강제 종료 실행"
                                             kill -KILL $PID_8080
                                           fi
                                         fi

                                         # 서버1 시작
                                         echo "🚀 서버1 (8080) 시작..."
                                         cd /var/www/qms/backend
                                         nohup java -jar -Dspring.profiles.active=prod-server1 defectapp-0.0.1-SNAPSHOT.jar > logs/app-8080.log 2>&1 &
                                       EOF

                                       # 서버1 헬스체크
                                       health_check 8080 || exit 1

                                       echo "==== [6/8] 무중단 배포 시작 - 서버2 (8081) 🔄 ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << 'EOF'
                                         # 서버2 graceful shutdown
                                         PID_8081=$(lsof -t -i:8081 2>/dev/null || echo "")
                                         if [ ! -z "$PID_8081" ]; then
                                           echo "🛑 서버2 (8081) 종료 중..."
                                           kill -TERM $PID_8081

                                           # 30초 대기
                                           for i in {1..30}; do
                                             if ! kill -0 $PID_8081 2>/dev/null; then
                                               break
                                             fi
                                             sleep 1
                                           done

                                           # 강제 종료가 필요한 경우
                                           if kill -0 $PID_8081 2>/dev/null; then
                                             echo "⚠️ 강제 종료 실행"
                                             kill -KILL $PID_8081
                                           fi
                                         fi

                                         # 서버2 시작
                                         echo "🚀 서버2 (8081) 시작..."
                                         cd /var/www/qms/backend
                                         nohup java -jar -Dspring.profiles.active=prod-server2 defectapp-0.0.1-SNAPSHOT.jar > logs/app-8081.log 2>&1 &
                                       EOF

                                       # 서버2 헬스체크
                                       health_check 8081 || exit 1

                                       echo "==== [7/8] Nginx 설정 검증 및 무중단 reload 🌐 ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
                                         sudo nginx -t
                                         if [ \$? -ne 0 ]; then
                                           echo "❌ Nginx 설정 문법 오류 발생. reload 중단."
                                           exit 1
                                         fi
                                         sudo nginx -s reload
                                         echo "✅ Nginx 무중단 reload 완료"
                                       EOF

                                       echo "==== [8/8] 최종 상태 확인 🔍 ===="
                                       ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << 'EOF'
                                         echo "📊 서버 상태 확인:"
                                         echo "- 포트 8080: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)"
                                         echo "- 포트 8081: $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health)"
                                         echo "- 외부 접속: $(curl -s -o /dev/null -w "%{http_code}" https://qms.jaemin.app/actuator/health)"

                                         echo ""
                                         echo "📈 프로세스 상태:"
                                         ps aux | grep "defectapp" | grep -v grep || echo "⚠️ Java 프로세스 없음"

                                         echo ""
                                         echo "📝 최근 로그 (8080):"
                                         tail -n 10 /var/www/qms/backend/logs/app-8080.log || echo "로그 파일 없음"

                                         echo ""
                                         echo "📝 최근 로그 (8081):"
                                         tail -n 10 /var/www/qms/backend/logs/app-8081.log || echo "로그 파일 없음"
                                       EOF

                                       echo "🎉 무중단 배포 완료! 두 서버 모두 정상 동작 중입니다."