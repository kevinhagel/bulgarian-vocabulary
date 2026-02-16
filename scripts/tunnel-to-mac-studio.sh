#!/usr/bin/env bash
# SSH tunnel to Mac Studio services for secure remote access
# This allows MacBook M2 to securely access PostgreSQL, pgAdmin, and Valkey

set -euo pipefail

REMOTE_HOST="${MAC_STUDIO_HOST:-macstudio}"

echo "🔒 Creating secure SSH tunnel to ${REMOTE_HOST}"
echo ""
echo "Forwarding ports:"
echo "  Local 5432 → Mac Studio PostgreSQL (5432)"
echo "  Local 5050 → Mac Studio pgAdmin (5050)"
echo "  Local 6379 → Mac Studio Valkey (6379)"
echo ""
echo "Press Ctrl+C to close tunnel"
echo ""

# Create SSH tunnel with port forwards
# -N: Don't execute remote command
# -L: Local port forwarding
ssh -N \
  -L 5432:localhost:5432 \
  -L 5050:localhost:5050 \
  -L 6379:localhost:6379 \
  "${REMOTE_HOST}"
