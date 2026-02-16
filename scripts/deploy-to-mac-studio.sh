#!/usr/bin/env bash
# Deploy Bulgarian Vocabulary infrastructure to Mac Studio
# Syncs docker-compose files and configuration to ~/bulgarian-vocabulary on Mac Studio

set -euo pipefail

REMOTE_HOST="${MAC_STUDIO_HOST:-macstudio}"
REMOTE_USER="${MAC_STUDIO_USER:-kevin}"
REMOTE_DIR="~/bulgarian-vocabulary"
LOCAL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "🚀 Deploying Bulgarian Vocabulary to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}"
echo "   From: ${LOCAL_DIR}"
echo ""

# Ensure remote directory exists
ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_DIR}"

# Sync docker-compose files and config
rsync -avz --progress \
  --include='docker-compose.yml' \
  --include='docker-compose.prod.yml' \
  --include='.env.production' \
  --include='config/' \
  --include='config/**' \
  --exclude='*' \
  "${LOCAL_DIR}/" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/"

echo ""
echo "✅ Files synced to Mac Studio"
echo ""
echo "Next steps on Mac Studio:"
echo "  1. cd ${REMOTE_DIR}"
echo "  2. Stop old containers: docker-compose -f ~/projects/archive/bulgarian-tutor-web/backend/docker-compose.yml down"
echo "  3. Start new stack: docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d"
echo "  4. Check health: docker-compose ps"
echo ""
