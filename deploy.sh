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

echo "==== [1/7] 백엔드 빌드 시작 ===="
cd backend || { echo "❌ backend 디렉토리가 없습니다."; exit 1; }
./gradlew clean build -x test || { echo "❌ 백엔드 빌드 실패"; exit 1; }
cd ..

echo "==== [2/7] 백엔드 배포 ===="
scp -o StrictHostKeyChecking=no -i "$PEM_PATH" backend/build/libs/defectapp-0.0.1-SNAPSHOT.jar ${EC2_USER}@${EC2_HOST}:${BACKEND_REMOTE_PATH}/ || { echo "❌ 백엔드 전송 실패"; exit 1; }

echo "==== [3/7] 프론트엔드 빌드 시작 ===="
cd frontend || { echo "❌ frontend 디렉토리가 없습니다."; exit 1; }
npm install
npm run build || { echo "❌ 프론트엔드 빌드 실패"; exit 1; }
cd ..

echo "==== [4/7] 프론트엔드 배포 전 기존 파일 삭제 ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} "rm -rf ${FRONTEND_REMOTE_PATH}/*" || { echo "❌ 기존 프론트엔드 파일 삭제 실패"; exit 1; }

echo "==== [5/7] 프론트엔드 배포 ===="
scp -o StrictHostKeyChecking=no -i "$PEM_PATH" -r frontend/dist/* ${EC2_USER}@${EC2_HOST}:${FRONTEND_REMOTE_PATH}/ || { echo "❌ 프론트엔드 전송 실패"; exit 1; }

echo "==== [6/7] 백엔드 무중단 재시작 (systemd) ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
  sudo systemctl restart qms
EOF

echo "==== [7/7] Nginx 설정 문법 검사 및 무중단 reload ===="
ssh -o StrictHostKeyChecking=no -i "$PEM_PATH" ${EC2_USER}@${EC2_HOST} << EOF
  sudo nginx -t
  if [ \$? -ne 0 ]; then
    echo "❌ Nginx 설정 문법 오류 발생. reload 중단."
    exit 1
  fi
  sudo nginx -s reload
  echo "✅ Nginx 무중단 reload 완료"
EOF

echo "✅ 전체 배포 완료!"
