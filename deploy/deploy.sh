#!/bin/bash
# =============================================================================
# GDTahara Deploy Script
# รันจาก project root บนเครื่อง local (Git Bash บน Windows หรือ Terminal บน Mac/Linux)
# Usage: bash deploy/deploy.sh
# =============================================================================

set -e  # หยุดทันทีถ้า command ไหน fail

# ──── CONFIG ──────────────────────────────────────────────────────────────────
SERVER_USER="ubuntu"          # << เปลี่ยนเป็น username ที่ใช้ SSH เข้า server
SERVER_HOST="10.1.53.30"
SERVER_APP_DIR="/opt/gdtahara"
SERVER_FE_DIR="/var/www/gdtahara"
SERVICE_NAME="gdtahara"
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "========================================="
echo " GDTahara Deploy → $SERVER_HOST"
echo "========================================="

# ── Step 1: Build Backend ─────────────────────────────────────────────────────
echo ""
echo "[1/4] Building Backend JAR..."
mvn clean package -DskipTests -q
JAR_FILE=$(ls target/gdtahara-backend-*.jar | head -1)
echo "      OK: $JAR_FILE"

# ── Step 2: Build Frontend ────────────────────────────────────────────────────
echo ""
echo "[2/4] Building Frontend (base=/MES/)..."
cd gdtahara-frontend
npm install --silent
VITE_BASE_PATH=/MES/ npm run build
cd ..
echo "      OK: gdtahara-frontend/dist/"

# ── Step 3: Upload to Server ──────────────────────────────────────────────────
echo ""
echo "[3/4] Uploading to $SERVER_HOST..."

# Upload JAR (ชื่อ app-new.jar ก่อน → rename เป็น app.jar หลัง restart)
echo "      → Uploading JAR..."
scp "$JAR_FILE" "$SERVER_USER@$SERVER_HOST:/tmp/gdtahara-app-new.jar"

# Upload FE: zip ส่ง แล้ว unzip บน server (ทำงานได้บน Windows Git Bash ไม่ต้อง rsync)
echo "      → Uploading Frontend..."
cd gdtahara-frontend/dist
zip -qr /tmp/gdtahara-fe.zip .
cd ../..
scp /tmp/gdtahara-fe.zip "$SERVER_USER@$SERVER_HOST:/tmp/gdtahara-fe.zip"
rm /tmp/gdtahara-fe.zip

# ── Step 4: Apply on Server ───────────────────────────────────────────────────
echo ""
echo "[4/4] Applying on server and restarting..."
ssh "$SERVER_USER@$SERVER_HOST" bash <<ENDSSH
  set -e

  # Deploy JAR
  sudo mv /tmp/gdtahara-app-new.jar $SERVER_APP_DIR/app.jar
  sudo chown gdtahara:gdtahara $SERVER_APP_DIR/app.jar

  # Deploy FE (clear old files แล้ว unzip ใหม่)
  sudo rm -rf $SERVER_FE_DIR/*
  sudo unzip -qo /tmp/gdtahara-fe.zip -d $SERVER_FE_DIR/
  sudo chown -R www-data:www-data $SERVER_FE_DIR/
  rm /tmp/gdtahara-fe.zip

  # Restart service
  sudo systemctl restart $SERVICE_NAME
  sleep 4
  sudo systemctl is-active --quiet $SERVICE_NAME \
    && echo "      Service: RUNNING" \
    || (echo "      Service: FAILED — checking logs..." && sudo journalctl -u $SERVICE_NAME -n 20 --no-pager)
ENDSSH

echo ""
echo "========================================="
echo " Deploy complete!"
echo " App: http://$SERVER_HOST/MES/"
echo " Health: http://$SERVER_HOST/MES/api/actuator/health"
echo "========================================="
