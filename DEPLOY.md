# Despliegue · OSTEOPERU Resultados

Documentacion operativa para levantar el stack en **desarrollo local** o en **produccion**.

> Este proyecto usa Docker Compose con archivos `.env` separados por entorno. No se commitean secretos.

---

## 1. Matriz de entornos

| Aspecto | DEV (local) | PROD (servidor) |
|---|---|---|
| **Comando recomendado** | `./scripts/dev-up.sh` | `./scripts/prod-up.sh` |
| **Compose files** | `docker-compose.yml` (override auto) | `docker-compose.yml` + `docker-compose.prod.yml` |
| **Env file** | `.env.dev` (commiteado) | `.env.prod` (gitignored) |
| **Perfil Spring** | `local` | `server` |
| **Acceso frontend** | `http://localhost:6060/resultados/` | `https://osteoperu-dev.qallpatic.com/resultados/` |
| **Acceso backend directo** | `http://localhost:7090/osteoperu-api-resultados/` | Solo via `osteoperu-frontend` en la red Docker |
| **Swagger** | `http://localhost:7090/osteoperu-api-resultados/swagger-ui.html` | No expuesto (proxy interno) |
| **RENIEC** | Deshabilitado | Habilitado |
| **SMS** | Deshabilitado | Habilitado |
| **DB** | MySQL en `161.132.53.229`, schema `osteoperu_resultados` | Misma DB |
| **Red Docker** | `osteoperu-net` (+ `proxy-network` para NPM si existe) | `osteoperu-net` + `proxy-network` |
| **Volumen uploads** | `/home/docker/uploads` en el host | `./uploads` en `/home/docker/osteoperu-resultados/uploads` |
| **Nginx Proxy Manager** | No requerido | Requerido para el acceso publico |

---

## 2. Estructura de archivos

```
apps/app-osteoperu-resultados/
├── docker-compose.yml            # Base parametrizada con ${VAR:-default}
├── docker-compose.prod.yml       # Override prod (env_file + sin puertos host)
├── .env.example                  # Plantilla COMMITTED (placeholders)
├── .env.dev                      # Valores DEV COMMITTED (sin secretos reales)
├── .env.prod                     # Valores PROD GITIGNORED (secretos reales) ← CREAR A PARTIR DE .env.example
├── .gitignore                    # Ignora .env.prod y .env.local
├── DEPLOY.md                     # Este archivo
├── scripts/
│   ├── dev-up.sh                 # Levantar dev
│   ├── dev-down.sh               # Bajar dev
│   ├── dev-logs.sh               # Logs dev
│   ├── prod-up.sh                # Levantar prod
│   ├── prod-down.sh              # Bajar prod
│   └── prod-logs.sh              # Logs prod
├── back-envio-resultados-api/    # Backend Spring Boot + Dockerfile multi-stage
└── front-envio-resultados/       # Frontend Angular + Dockerfile multi-stage
```

> **El host NO necesita Java, Maven ni Node instalados.** Los Dockerfiles son
> multi-stage: el backend compila el JAR dentro de Docker con `maven:3.9-eclipse-temurin-21`,
> y el frontend corre `npm install` + `ng build` dentro de `node:20-alpine`. Solo
> necesitás Docker (engine + compose plugin v2) en el host.

---

## 3. Levantar DEV (recomendado: con script)

### 3.1 Una sola vez — preparar `.env.dev`

```bash
# Si NO existe .env.dev en el repo clonado (debería existir, pero por si acaso):
cp .env.example .env.dev
# Editar valores si necesitas cambiarlos (DB, JWT, etc.). Los valores por defecto ya funcionan.
```

### 3.2 Levantar

```bash
./scripts/dev-up.sh
```

Esto ejecuta internamente: `docker compose up -d --build`.

### 3.3 Verificar

- Frontend: abrir `http://localhost:6060/resultados/`
- Swagger: abrir `http://localhost:7090/osteoperu-api-resultados/swagger-ui.html`
- Login admin: `admin / Admin@2026`

### 3.4 Logs / detener

```bash
./scripts/dev-logs.sh            # todos los servicios
./scripts/dev-logs.sh backend    # solo el backend
./scripts/dev-down.sh
```

---

## 4. Levantar DEV sin script (docker compose directo)

Si preferís invocar `docker compose` directamente:

```bash
# Levantar (lee .env.dev automaticamente, hace override si existe docker-compose.override.yml)
docker compose up -d --build

# Logs
docker compose logs -f

# Detener
docker compose down
```

> **Tip**: podés apuntar a otro `.env` con `docker compose --env-file .env.otro up -d`.

---

## 5. Levantar PROD

### 5.1 Una sola vez — crear `.env.prod`

```bash
# En el servidor (SSH), como el usuario que maneja docker:
cd /home/docker/osteoperu-resultados

# Crear .env.prod a partir de la plantilla
cp .env.example .env.prod

# Editar y rellenar secretos reales
nano .env.prod
```

Variables **obligatorias** a rellenar en `.env.prod` (tienen defaults inseguros en `.env.example`):

| Variable | Qué poner |
|---|---|
| `DB_PASSWORD` | Password real del usuario `resultados_user` de MySQL |
| `JWT_SECRET` | Clave aleatoria ≥ 32 bytes (ej: `openssl rand -base64 48`) |
| `ADMIN_PASSWORD` | Password del usuario admin sembrado al arrancar |
| `RENIEC_TOKEN` | Token del proveedor RENIEC (decolecta, apidni, etc.) |
| `SMS_API_KEY` | API key del gateway SMS |
| `PUBLIC_BASE_URL` | `https://osteoperu-dev.qallpatic.com/resultados/osteoperu-api-resultados` |
| `PORTAL_URL` | `https://osteoperu-dev.qallpatic.com/resultados/login` |
| `CORS_ALLOWED_ORIGINS` | `https://osteoperu-dev.qallpatic.com` |

### 5.2 Crear la red `proxy-network` (solo si no existe)

NPM usa esta red para resolver contenedores por nombre:

```bash
docker network create proxy-network
```

### 5.3 Levantar (con script — recomendado)

```bash
./scripts/prod-up.sh
```

El script hace sanity-check de placeholders y luego ejecuta:
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

### 5.4 Levantar sin script (docker compose directo)

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --env-file .env.prod \
  up -d --build
```

### 5.5 Verificar

- Acceso publico: `https://osteoperu-dev.qallpatic.com/resultados/`
- Logs: `./scripts/prod-logs.sh`
- Inspeccionar red: `docker network inspect proxy-network`

### 5.6 Nginx Proxy Manager (UI)

Ya configurado en producción, verificar que la entrada apunte a `http://osteoperu-frontend:80` con la pestaña **Advanced** incluyendo solo este bloque (NPM ya agrega `proxy_pass` y los `set_header` basicos):

```nginx
rewrite ^/resultados$ /resultados/ permanent;
rewrite ^/resultados/(.*)$ /$1 break;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Prefix /resultados;
proxy_request_buffering off;
proxy_read_timeout 300s;
```

---

## 6. Variables por entorno (referencia rapida)

### Compartidas (mismo nombre en ambos)

- `TAG` — Tag de imagen Docker (`dev` o `prod`)
- `BACKEND_PORT` / `FRONTEND_PORT` — Puertos host (en prod se ignoran via override)
- `DB_*` — Conexion MySQL
- `SPRING_PROFILES_ACTIVE` — `local` (dev) o `server` (prod)
- `CONTEXT_PATH` — siempre `/osteoperu-api-resultados`
- `PUBLIC_BASE_URL` / `PORTAL_URL` / `CORS_ALLOWED_ORIGINS`
- `JWT_SECRET` / `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- `UPLOADS_PATH` — path en host que se monta en `/app/uploads`
- `RENIEC_*` / `SMS_*`

### Especificas por entorno (valores sugeridos)

| Variable | DEV | PROD |
|---|---|---|
| `TAG` | `dev` | `prod` |
| `BACKEND_PORT` | `7090` | (ignorado) |
| `FRONTEND_PORT` | `6060` | (ignorado) |
| `SPRING_PROFILES_ACTIVE` | `local` | `server` |
| `PUBLIC_BASE_URL` | `http://localhost:6060/osteoperu-api-resultados` | `https://osteoperu-dev.qallpatic.com/resultados/osteoperu-api-resultados` |
| `PORTAL_URL` | `http://localhost:6060/login` | `https://osteoperu-dev.qallpatic.com/resultados/login` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:6060,http://localhost:4200` | `https://osteoperu-dev.qallpatic.com` |
| `JWT_SECRET` | (placeholder dev) | (random ≥32 bytes, real) |
| `RENIEC_ENABLED` | `false` | `true` |
| `SMS_ENABLED` | `false` | `true` |
| `RENIEC_TOKEN` | (placeholder) | (token real del proveedor) |
| `SMS_API_KEY` | (placeholder) | (api key real) |

---

## 7. Troubleshooting

### "additional properties 'proxy-network' not allowed" al hacer `docker compose down`

Error de indentacion YAML. Verificar con:
```bash
sed -n '/^networks:/,/^$/p' docker-compose.yml
```
La seccion `networks:` debe verse asi (sangria de 2 espacios bajo `networks:`):
```yaml
networks:
  osteoperu-net:
    driver: bridge
  proxy-network:
    external: true
    name: proxy-network
```

### "JWT_SECRET no puede ser vacio" o app arranca con secreto por defecto

`JWT_SECRET` no esta en `.env.prod`. Generar uno real:
```bash
openssl rand -base64 48
```
Pegarlo en `.env.prod` y re-ejecutar `./scripts/prod-up.sh`.

### "Cannot connect to MySQL"

- Verificar `DB_HOST` / `DB_PORT` en el `.env` correspondiente.
- Verificar que el contenedor backend pueda resolver el host (en dev apuntamos a la IP publica del server MySQL — si estas offline, no levanta).
- Probar conectividad desde el contenedor: `docker compose exec backend ping -c 2 $DB_HOST`.

### Frontend no carga assets (404 en `chunk-XXX.js`)

El front esta compilado con `--base-href=/resultados/` y debe servirse bajo ese sub-path. Verificar:
- DEV: acceder a `http://localhost:6060/resultados/` (NO `http://localhost:6060/`).
- PROD: NPM debe hacer `rewrite ^/resultados/(.*)$ /$1 break;` antes de pasar al contenedor.

### "port is already allocated"

Otro proceso o contenedor usa el puerto. Ver `.env` y cambiar `BACKEND_PORT` / `FRONTEND_PORT`, o detener quien lo ocupa:
```bash
sudo lsof -i :7090
sudo lsof -i :6060
```

### Cambiar credenciales despues de un primer arranque

El usuario admin se siembra solo si no existe. Para rotar la password:
- Cambiar `ADMIN_PASSWORD` en `.env.prod`, `docker compose ... up -d --build` para que tome la nueva env var. **OJO**: la entidad admin ya creada en la DB conserva su password vieja (BCrypt hasheada). El sembrado es idempotente (no actualiza si existe). Para forzar el cambio, actualizar directamente en MySQL o exponer un endpoint admin de reseteo.

### Levantar solo el backend (sin front dockerizado)

Para iterar rapido en el front con `ng serve`:
```bash
docker compose up -d backend         # solo backend en docker
cd front-envio-resultados && npm start  # frontend con HMR en :4200
```
El `environment.development.ts` ya apunta a `http://localhost:7090/osteoperu-api-resultados`.

### El primer build tarda mucho (3-8 min)

Es normal la primera vez: Docker descarga `maven:3.9-eclipse-temurin-21` (~600MB) y todas las dependencias de Maven (~200MB) y de npm (~250MB). Builds subsiguientes reutilizan el cache y tardan 10-30 segundos.

Si querés ver el progreso en otra terminal:
```bash
docker compose build backend   # o frontend
```

---

## 8. Comandos utiles adicionales

```bash
# Estado del stack
docker compose ps

# Entrar al contenedor backend
docker compose exec backend sh

# Ver logs de un servicio especifico (sin seguir)
docker compose logs --tail=100 backend

# Inspeccionar la red proxy-network
docker network inspect proxy-network

# Limpiar TODO (contenedores + volumenes + imagenes) — DEV only, NO en prod
docker compose down -v --rmi all
```