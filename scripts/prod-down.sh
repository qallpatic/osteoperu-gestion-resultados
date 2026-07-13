#!/usr/bin/env bash
# Detiene el stack PROD (no elimina volumenes ni imagenes).

set -euo pipefail
cd "$(dirname "$0")/.."

docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  down

echo "OK: stack PROD detenido."