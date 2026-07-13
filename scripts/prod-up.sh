#!/usr/bin/env bash
# Levanta el stack PROD (perfil server de Spring, .env.prod, sin puertos al host).
# No requiere Java/Maven/Node en el host: Docker hace todo via multi-stage builds.
# Equivale a: docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build

set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo "ERROR: .env.prod no existe."
  echo "  cp .env.example .env.prod"
  echo "  nano .env.prod   # rellenar JWT_SECRET, DB_PASSWORD, RENIEC_TOKEN, ADMIN_PASSWORD, etc."
  exit 1
fi

# Sanity-check: que no se haya quedado con placeholders del .env.example
if grep -qE '^(JWT_SECRET|DB_PASSWORD|RENIEC_TOKEN|ADMIN_PASSWORD)= *$' .env.prod \
   || grep -qE '^JWT_SECRET=cambia-esta-clave' .env.prod; then
  echo "ADVERTENCIA: .env.prod parece tener placeholders sin rellenar."
  echo "Revisar: JWT_SECRET, DB_PASSWORD, RENIEC_TOKEN, ADMIN_PASSWORD"
  read -r -p "Continuar de todos modos? [s/N] " ans
  [[ "$ans" =~ ^[sSyY]$ ]] || exit 1
fi

echo "==> Construyendo imagenes (la primera vez tarda 5-10 min: descarga Maven, npm y deps)"
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  up -d --build

cat <<EOF

OK: stack PROD arriba.

  Backend:  solo accesible via osteoperu-frontend en la red Docker.
  Publico:  https://osteoperu-dev.qallpatic.com/resultados/  (vía Nginx Proxy Manager)

  Logs:     ./scripts/prod-logs.sh
  Detener:  ./scripts/prod-down.sh

EOF