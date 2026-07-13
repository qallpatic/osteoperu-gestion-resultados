#!/usr/bin/env bash
# Sigue los logs del stack PROD. Acepta el nombre del servicio como argumento.

set -euo pipefail
cd "$(dirname "$0")/.."

docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  logs -f "$@"