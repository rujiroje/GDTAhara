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
SERVER_HOST="10.1.53.31"
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
./mvnw clean package -DskipTests -q -Dmaven.clean.failOnError=false
JAR_FILE=$(ls target/gdtahara-backend-*.jar | head -1)
echo "      OK: $JAR_FILE"

# ── Step 2: Build Frontend ────────────────────────────────────────────────────
echo ""
echo "[2/4] Building Frontend (base=/MES/)..."
cd gdtahara-frontend
npm install --silent
VITE_PROD=1 npm run build
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
TARFILE="/tmp/gdtahara_fe.tar.gz"
cd gdtahara-frontend/dist
tar -czf "$TARFILE" .
cd ../..
"$PSCP" -pw "$SERVER_PASS" -batch "$TARFILE" "$SERVER_USER@$SERVER_HOST:/tmp/gdtahara-fe.tar.gz"
rm -f "$TARFILE"

# ── Step 4: Apply on Server ───────────────────────────────────────────────────
echo ""
echo "[4/4] Applying on server..."

# 4a. File operations
"$PLINK" -ssh -pw "$SERVER_PASS" -batch "$SERVER_USER@$SERVER_HOST" "
  set -e
  mv $SERVER_APP_DIR/app-new.jar $SERVER_APP_DIR/app.jar
  chmod 644 $SERVER_APP_DIR/app.jar
  echo '$SERVER_PASS' | sudo -S find $SERVER_FE_DIR -mindepth 1 -delete
  echo '$SERVER_PASS' | sudo -S tar -xzf /tmp/gdtahara-fe.tar.gz -C $SERVER_FE_DIR/
  rm /tmp/gdtahara-fe.tar.gz
  echo '      Files applied OK'
"

# 4b. Restart service — use echo pipe to feed sudo password (plink -batch has no TTY)
"$PLINK" -ssh -pw "$SERVER_PASS" -batch "$SERVER_USER@$SERVER_HOST" "echo '$SERVER_PASS' | sudo -S systemctl restart $SERVICE_NAME"
echo "      Service restarted"
sleep 8

# 4c. Check status
STATUS=$("$PLINK" -ssh -pw "$SERVER_PASS" -batch "$SERVER_USER@$SERVER_HOST" "echo '$SERVER_PASS' | sudo -S systemctl is-active $SERVICE_NAME 2>/dev/null")
if [ "$STATUS" = "active" ]; then
  echo "      Service: RUNNING"
else
  echo "      Service: FAILED (status=$STATUS) — last 30 log lines:"
  "$PLINK" -ssh -pw "$SERVER_PASS" -batch "$SERVER_USER@$SERVER_HOST" \
    "sudo journalctl -u $SERVICE_NAME -n 30 --no-pager"
  exit 1
fi

echo ""
echo "========================================="
echo " Deploy complete!"
echo " App   : https://$SERVER_HOST/MES/"
echo " Health: https://$SERVER_HOST/MES/api/actuator/health"
echo "========================================="
