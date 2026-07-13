#!/usr/bin/env bash
# Detiene el stack DEV (no elimina volumenes ni imagenes).

set -euo pipefail
cd "$(dirname "$0")/.."

docker compose down

echo "OK: stack DEV detenido. Datos en ./uploads preservados."
echo "  Para limpiar TODO (incluido volumen uploads): rm -rf ./uploads"