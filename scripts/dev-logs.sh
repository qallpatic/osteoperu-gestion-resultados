#!/usr/bin/env bash
# Sigue los logs del stack DEV. Pasa opcionalmente el nombre del servicio
# como argumento: ./scripts/dev-logs.sh backend

set -euo pipefail
cd "$(dirname "$0")/.."

docker compose logs -f "$@"