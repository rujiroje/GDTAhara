#!/bin/bash
# =============================================================================
# GDTahara Deploy Script
# รันจาก project root บน Git Bash (Windows)
# Usage: bash deploy/deploy.sh
#
# ต้องมีไฟล์ deploy/.env.deploy (ไม่ commit ขึ้น git):
#   SERVER_PASS=your_ssh_password
# =============================================================================

set -e

# ── Load server credentials ───────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_DEPLOY="$SCRIPT_DIR/.env.deploy"
if [ ! -f "$ENV_DEPLOY" ]; then
  echo "ERROR: ไม่พบ deploy/.env.deploy"
  echo "สร้างไฟล์นั้นก่อน:"
  echo "  echo 'SERVER_PASS=รหัสผ่าน' > deploy/.env.deploy"
  exit 1
fi
source "$ENV_DEPLOY"

PLINK="/c/Program Files/PuTTY/plink"
PSCP="/c/Program Files/PuTTY/pscp"
SERVER_USER="tst"
SERVER_HOST="10.1.53.30"
SERVER_APP_DIR="/opt/gdtahara"
SERVER_FE_DIR="/var/www/gdtahara"
SERVICE_NAME="gdtahara"

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

echo "      → JAR..."
"$PSCP" -pw "$SERVER_PASS" -batch \
  "$JAR_FILE" \
  "$SERVER_USER@$SERVER_HOST:$SERVER_APP_DIR/app-new.jar"

echo "      → Frontend..."
ZIPFILE="${TEMP:-/tmp}/gdtahara_fe_$(date +%s).zip"
cd gdtahara-frontend/dist
zip -qr "$ZIPFILE" .
cd ../..
"$PSCP" -pw "$SERVER_PASS" -batch "$ZIPFILE" "$SERVER_USER@$SERVER_HOST:/tmp/gdtahara-fe.zip"
rm -f "$ZIPFILE"

# ── Step 4: Apply on Server ───────────────────────────────────────────────────
echo ""
echo "[4/4] Applying on server..."
"$PLINK" -ssh -pw "$SERVER_PASS" -batch "$SERVER_USER@$SERVER_HOST" "
  set -e
  mv $SERVER_APP_DIR/app-new.jar $SERVER_APP_DIR/app.jar
  chown gdtahara:gdtahara $SERVER_APP_DIR/app.jar
  rm -rf $SERVER_FE_DIR/*
  unzip -qo /tmp/gdtahara-fe.zip -d $SERVER_FE_DIR/
  rm /tmp/gdtahara-fe.zip
  sudo systemctl restart $SERVICE_NAME
  sleep 5
  sudo systemctl is-active --quiet $SERVICE_NAME \
    && echo '      Service: RUNNING' \
    || (echo '      Service: FAILED — logs:' && sudo journalctl -u $SERVICE_NAME -n 30 --no-pager && exit 1)
"

echo ""
echo "========================================="
echo " Deploy complete!"
echo " App   : https://$SERVER_HOST/MES/"
echo " Health: https://$SERVER_HOST/MES/api/actuator/health"
echo "========================================="
