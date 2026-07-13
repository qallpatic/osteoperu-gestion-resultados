#!/usr/bin/env bash
# Levanta el stack DEV (perfil local de Spring, .env.dev).
# No requiere Java/Maven/Node en el host: Docker hace todo via multi-stage builds.
# Equivale a: docker compose up -d --build

set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env.dev ]; then
  echo "ERROR: .env.dev no existe."
  echo "  cp .env.example .env.dev && editar valores"
  exit 1
fi

echo "==> Construyendo imagenes (la primera vez tarda 3-8 min: descarga Maven y deps)"
docker compose up -d --build

# Carga puertos del .env para mostrarlos en el resumen (best-effort).
set +u
# shellcheck disable=SC1091
source <(grep -E '^(FRONTEND_PORT|BACKEND_PORT)=' .env.dev)
set -u

cat <<EOF

OK: stack DEV arriba.

  Frontend (con sub-path):  http://localhost:${FRONTEND_PORT:-6060}/resultados/
  Backend  (directo):       http://localhost:${BACKEND_PORT:-7090}/osteoperu-api-resultados/
  Swagger:                  http://localhost:${BACKEND_PORT:-7090}/osteoperu-api-resultados/swagger-ui.html

  Login admin:              admin / Admin@2026

  Logs:                     ./scripts/dev-logs.sh
  Detener:                  ./scripts/dev-down.sh

EOF