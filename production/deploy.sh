#!/bin/bash

# habiDue Blue-Green Zero-Downtime Deployment Script
# Location: ~/habidue-app/deploy.sh

APP_NAME="habidue-app"
DOCKER_COMPOSE_PATH="/home/ec2-user/habidue-app/docker-compose.yml"
NGINX_CONF_PATH="/home/ec2-user/habidue-app/nginx/conf.d/nginx_default.conf"

echo "🚀 [$(date)] Deployment started..."

# 1. 현재 실행 중인 슬롯 확인 (Blue가 떠 있으면 Green을 띄우고, 반대면 Blue를 띄움)
EXIST_BLUE=$(docker ps | grep ${APP_NAME}-blue)

if [ -z "$EXIST_BLUE" ]; then
    TARGET_PORT=8081
    TARGET_SLOT="blue"
    IDLE_SLOT="green"
    IDLE_PORT=8082
else
    TARGET_PORT=8082
    TARGET_SLOT="green"
    IDLE_SLOT="blue"
    IDLE_PORT=8081
fi

echo "✨ Target slot: $TARGET_SLOT (Port: $TARGET_PORT)"

# 2. 새로운 이미지 Pull (GitHub Actions에서 빌드 후 올린 최신 이미지)
echo "📥 Pulling latest images..."
docker-compose -f $DOCKER_COMPOSE_PATH pull app-$TARGET_SLOT

# 3. 새로운 슬롯 실행
echo "⚙️ Starting app-$TARGET_SLOT..."
docker-compose -f $DOCKER_COMPOSE_PATH up -d app-$TARGET_SLOT

# 4. Health Check (새로운 앱이 정상적으로 떴는지 확인)
echo "🔍 Health checking on port $TARGET_PORT..."
for retry in {1..15}
do
    # 컨테이너 내부의 8080이 아닌 호스트에서 노출된 포트로 체크
    RESPONSE=$(curl -s http://localhost:$TARGET_PORT/api/health | grep "UP")
    if [ -n "$RESPONSE" ]; then
        echo "✅ Health check success!"
        break
    fi

    if [ $retry -eq 15 ]; then
        echo "❌ Health check failed. Deployment aborted."
        docker-compose -f $DOCKER_COMPOSE_PATH stop app-$TARGET_SLOT
        exit 1
    fi

    echo "⏳ Waiting for app to start... (retry $retry/15)"
    sleep 10
done

# 5. Nginx 설정 변경 (Upstream 전환)
echo "🔄 Switching Nginx upstream to $TARGET_SLOT..."
# sed를 사용하여 upstream 부분을 타겟 슬롯으로 교체
sudo sed -i "s/server app-.*:8080;/server app-$TARGET_SLOT:8080;/g" $NGINX_CONF_PATH

# 6. Nginx Reload
echo "Reloading Nginx..."
docker exec habidue-nginx nginx -s reload

# 7. 이전 슬롯 종료
echo "🧹 Stopping idle slot: $IDLE_SLOT..."
docker-compose -f $DOCKER_COMPOSE_PATH stop app-$IDLE_SLOT

echo "🎉 [$(date)] Deployment finished successfully!"
