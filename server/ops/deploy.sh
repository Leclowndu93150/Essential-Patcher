#!/usr/bin/env bash
# Deploy cosmetics-sync to the VPS. Run from the project root (one above ops/).
set -euo pipefail

CERTBOT_EMAIL="${CERTBOT_EMAIL:?set CERTBOT_EMAIL for the Lets Encrypt registration}"
CERTBOT_DOMAIN="${CERTBOT_DOMAIN:-cosmetics.leclowndu93150.dev}"
VPS_USER="${COSMETICS_VPS_USER:-debian}"
VPS_HOST="${COSMETICS_VPS_HOST:?set COSMETICS_VPS_HOST to the target host}"

if [ -z "${COSMETICS_VPS_PASS:-}" ]; then
  echo "set COSMETICS_VPS_PASS to the debian user password" >&2
  exit 1
fi

SSH="sshpass -p $COSMETICS_VPS_PASS ssh -o StrictHostKeyChecking=no $VPS_USER@$VPS_HOST"
SCP="sshpass -p $COSMETICS_VPS_PASS scp -o StrictHostKeyChecking=no -r"

if [ ! -f conf/cosmeticsync.env ]; then
  echo "conf/cosmeticsync.env not found; copy conf/cosmeticsync.example.env, set JWT_SECRET + PANEL_HMAC_KEY, and rerun." >&2
  echo "generate JWT_SECRET with: openssl rand -base64 48" >&2
  exit 1
fi
if ! grep -q '^JWT_SECRET=..' conf/cosmeticsync.env; then
  echo "JWT_SECRET in conf/cosmeticsync.env looks empty. set it before deploying." >&2
  exit 1
fi

echo "[1/8] uploading source"
$SCP cosmeticsync/ ops/ requirements.txt $VPS_USER@$VPS_HOST:/tmp/cosmetics-stage/

echo "[2/8] creating cosmeticsync user + /opt/cosmetics-sync + venv"
$SSH 'bash -s' <<'REMOTE'
set -euo pipefail
sudo id -u cosmeticsync >/dev/null 2>&1 || sudo useradd --system --home /opt/cosmetics-sync --shell /usr/sbin/nologin cosmeticsync
sudo mkdir -p /opt/cosmetics-sync
sudo rm -rf /opt/cosmetics-sync/cosmeticsync /opt/cosmetics-sync/ops
sudo mv /tmp/cosmetics-stage/cosmeticsync /opt/cosmetics-sync/cosmeticsync
sudo mv /tmp/cosmetics-stage/ops /opt/cosmetics-sync/ops
sudo mv /tmp/cosmetics-stage/requirements.txt /opt/cosmetics-sync/requirements.txt
rm -rf /tmp/cosmetics-stage
sudo pip3 install --break-system-packages --ignore-installed --quiet -r /opt/cosmetics-sync/requirements.txt
sudo chown -R cosmeticsync:cosmeticsync /opt/cosmetics-sync
REMOTE

echo "[3/8] installing .env"
$SCP conf/cosmeticsync.env $VPS_USER@$VPS_HOST:/tmp/cosmeticsync.env
$SSH 'sudo mv /tmp/cosmeticsync.env /opt/cosmetics-sync/.env && sudo chown cosmeticsync:cosmeticsync /opt/cosmetics-sync/.env && sudo chmod 600 /opt/cosmetics-sync/.env'

echo "[4/8] installing systemd unit"
$SSH 'sudo install -m 0644 /opt/cosmetics-sync/ops/cosmetics-sync.service /etc/systemd/system/cosmetics-sync.service'
$SSH 'sudo systemctl daemon-reload'

echo "[5/8] enabling + starting service"
$SSH 'sudo systemctl enable --now cosmetics-sync.service'

echo "[6/8] nginx site"
$SSH 'sudo install -m 0644 /opt/cosmetics-sync/ops/nginx-cosmetics.conf /etc/nginx/sites-available/cosmetics && sudo ln -sf /etc/nginx/sites-available/cosmetics /etc/nginx/sites-enabled/cosmetics && sudo nginx -t && sudo systemctl reload nginx'

echo "[7/8] certbot"
$SSH "sudo certbot --nginx -d $CERTBOT_DOMAIN --non-interactive --agree-tos -m $CERTBOT_EMAIL --redirect || echo '(certbot non-interactive failed; run interactively on the VPS the first time)'"

echo "[8/8] adding cosmetics-sync.service to the panel's sudoers + service list"
echo "  -> remember to: (a) add 'cosmetics-sync.service' to PANEL_SERVICES in /opt/panel/.env"
echo "  -> remember to: (b) add restart/start/stop entries for cosmetics-sync.service to /etc/sudoers.d/panel"
echo "  -> remember to: (c) sudo systemctl restart panel"
echo
echo "done. test: curl https://$CERTBOT_DOMAIN/"
