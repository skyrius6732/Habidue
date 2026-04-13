#!/bin/bash
# =============================================================
# HabiDue DB 자동 백업 스크립트
# - mysqldump → gzip 압축 → S3 업로드 → 로컬 7일 이상 파일 삭제
# 실행 위치: EC2 서버의 docker-compose.yml이 있는 디렉토리
# crontab 등록 예시: 0 3 * * * /home/ec2-user/habidue-app/db_backup.sh >> /home/ec2-user/habidue-app/backups/logs/backup.log 2>&1
# =============================================================

set -e

# --- 경로 설정 (서버 실제 경로로 맞춰줄 것) ---
COMPOSE_DIR="/home/ec2-user/habidue-app"
BACKUP_DIR="$COMPOSE_DIR/backups"
BACKUP_LOG_DIR="$COMPOSE_DIR/backups/logs"
LOG_PREFIX="[$(date '+%Y-%m-%d %H:%M:%S')]"

# --- .env에서 환경변수 로드 ---
if [ -f "$COMPOSE_DIR/.env" ]; then
    export $(grep -v '^#' "$COMPOSE_DIR/.env" | xargs)
else
    echo "$LOG_PREFIX [ERROR] .env 파일을 찾을 수 없습니다: $COMPOSE_DIR/.env"
    exit 1
fi

# --- 필수 변수 확인 ---
: "${DB_NAME:?DB_NAME이 설정되지 않았습니다}"
: "${DB_USERNAME:?DB_USERNAME이 설정되지 않았습니다}"
: "${DB_PASSWORD:?DB_PASSWORD가 설정되지 않았습니다}"
: "${S3_BUCKET_NAME:?S3_BUCKET_NAME이 설정되지 않았습니다}"
: "${AWS_REGION:?AWS_REGION이 설정되지 않았습니다}"

# --- 백업 디렉토리 생성 ---
mkdir -p "$BACKUP_DIR"
mkdir -p "$BACKUP_LOG_DIR"

# --- 파일명 설정 ---
DATE=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="$BACKUP_DIR/habidue_backup_${DATE}.sql.gz"

echo "$LOG_PREFIX 백업 시작: $DB_NAME → $BACKUP_FILE"

# --- mysqldump 실행 (Docker 컨테이너 내부) ---
docker exec habidue-db mysqldump \
    -u"$DB_USERNAME" \
    -p"$DB_PASSWORD" \
    --single-transaction \
    --no-tablespaces \
    --routines \
    --triggers \
    "$DB_NAME" | gzip > "$BACKUP_FILE"

echo "$LOG_PREFIX mysqldump 완료 ($(du -sh "$BACKUP_FILE" | cut -f1))"

# --- S3 업로드 ---
S3_PATH="s3://${S3_BUCKET_NAME}/db-backups/$(date '+%Y/%m')/$(basename "$BACKUP_FILE")"
aws s3 cp "$BACKUP_FILE" "$S3_PATH" --region "$AWS_REGION"

echo "$LOG_PREFIX S3 업로드 완료: $S3_PATH"

# --- 로컬 7일 이상 된 백업 파일 삭제 ---
find "$BACKUP_DIR" -name "habidue_backup_*.sql.gz" -mtime +7 -delete
echo "$LOG_PREFIX 로컬 오래된 백업 정리 완료"

echo "$LOG_PREFIX ✅ 백업 성공"
