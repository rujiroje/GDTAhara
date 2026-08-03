#!/bin/bash
# =============================================================================
# GDTahara Deploy Script — run from project root on your LOCAL machine
# Usage:
#   chmod +x deploy/deploy.sh
#   ./deploy/deploy.sh
# =============================================================================

set -e  # exit on any error

# ──── CONFIG (edit these) ────────────────────────────────────────────────────
SERVER_USER="gdtahara"                # Linux user on the server
SERVER_HOST="10.1.x.x"               # << เปลี่ยนเป็น IP ของ App Server
SERVER_APP_DIR="/opt/gdtahara"        # directory บน server
SERVER_FE_DIR="/var/www/gdtahara"     # Nginx web root
SERVICE_NAME="gdtahara"              # systemd service name
# ─────────────────────────────────────────────────────────────────────────────

echo "=== [1/4] Building Backend JAR ==="
mvn clean package -DskipTests -q
JAR_FILE=$(ls target/gdtahara-backend-*.jar | head -1)
echo "    Built: $JAR_FILE"

echo "=== [2/4] Building Frontend ==="
cd gdtahara-frontend
npm install --silent
npm run build --silent
cd ..
echo "    Built: gdtahara-frontend/dist/"

echo "=== [3/4] Uploading to server ($SERVER_HOST) ==="
# Upload JAR
scp "$JAR_FILE" "$SERVER_USER@$SERVER_HOST:$SERVER_APP_DIR/app-new.jar"

# Upload FE
rsync -az --delete gdtahara-frontend/dist/ "$SERVER_USER@$SERVER_HOST:$SERVER_FE_DIR/"

echo "=== [4/4] Restarting Backend ==="
ssh "$SERVER_USER@$SERVER_HOST" "
  mv $SERVER_APP_DIR/app-new.jar $SERVER_APP_DIR/app.jar
  sudo systemctl restart $SERVICE_NAME
  sleep 3
  sudo systemctl status $SERVICE_NAME --no-pager -l
"

echo ""
echo "✓ Deploy complete!"
echo "  App: http://$SERVER_HOST/"
echo "  API: http://$SERVER_HOST/api/actuator/health"
