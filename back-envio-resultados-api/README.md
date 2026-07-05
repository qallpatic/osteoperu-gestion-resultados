# back-envio-resultados-api

API (Java 21 / Spring Boot 3.3) para el **envío de resultados de pacientes** de OSTEOPERU.

El personal sube los resultados de un paciente (número destino + DNI + archivos PDF/imagen/video).
El backend:

1. Crea una carpeta física por paciente: `{telefono}_{dni}`.
2. Guarda los archivos dentro de esa carpeta.
3. Genera un **link corto** (acortador propio) que apunta a un **visor web**.
4. Envía un **SMS** al paciente con ese link.
5. Registra en la BD el **conteo de clicks** y marca **cuándo el paciente vio** sus resultados.

## ¿Página web o descarga directa?

Se implementó una **página web de resultados** (`/r/{token}`) en lugar de descarga directa porque:

- Un paciente puede tener varios archivos (PDF + video + imágenes): la página los lista todos.
- Se ve inline en el celular sin obligar a descargar.
- La carga de la página **es** el evento de "visto" → así se marca `first_viewed_at` y se cuenta la vista.
- Igual permite descargar cada archivo (`/r/{token}/descargar/{fileId}`).

## Arquitectura

```
Panel (front) ──POST /api/resultados/upload──▶ ResultadoService
                                                 ├─ StorageService  (carpeta {telefono}_{dni} + archivos)
                                                 ├─ ShortLinkService (acortador → tabla short_link)
                                                 └─ SmsService       (cola → gateway Textbee)

Paciente ──SMS──▶ /s/{code} (cuenta CLICK) ──302──▶ /r/{token} (cuenta VIEW, marca visto) ──▶ archivos
```

### Tablas (Flyway `V1__init_schema.sql`)

- `patient_result_batch` — lote por paciente, token del visor, `view_count`, `first_viewed_at`, estado SMS.
- `result_file` — archivos del lote (tipo, tamaño, nombres).
- `short_link` — acortador (código, destino, `click_count`).
- `view_event` — traza de cada CLICK / VIEW / DOWNLOAD (IP, user-agent).

## Ambientes

El perfil se elige con `SPRING_PROFILES_ACTIVE` (`local` | `server`). Por defecto: `local`.

| Parámetro          | local (`application-local.yml`)        | server (`application-server.yml`)          |
|--------------------|----------------------------------------|--------------------------------------------|
| Carpeta resultados | `D:/resultados-osteoperu`              | `/data/resultados-osteoperu`               |
| Link público       | `http://localhost:7090/...`            | `https://resultados.osteoperu.qallpatic...`|
| Envío SMS real     | `false` (solo log)                     | `true`                                     |
| Base de datos      | `161.132.53.229 / osteoperu_resultados`| igual (por variables de entorno)           |

> **Nada está hardcodeado**: todo sale del `.yml` y puede sobreescribirse por variables de entorno.

## Requisitos previos en la BD

El usuario `resultados_user` necesita permisos sobre la BD:

```sql
GRANT ALL PRIVILEGES ON osteoperu_resultados.* TO 'resultados_user'@'%';
FLUSH PRIVILEGES;
```

Flyway crea las tablas automáticamente al arrancar.

## Ejecutar

### Local (Windows)
```bash
./mvnw spring-boot:run
# o con perfil explícito:
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Server
```bash
export SPRING_PROFILES_ACTIVE=server
export DB_HOST=127.0.0.1
export DB_PASSWORD='R3sultados@2026!'
export PUBLIC_BASE_URL=https://resultados.osteoperu.qallpatic.com.pe
export STORAGE_BASE_PATH=/data/resultados-osteoperu
java -jar target/back-envio-resultados-api-0.0.1-SNAPSHOT.jar
```

### Docker
```bash
docker build -t back-envio-resultados-api .
docker run -d -p 7090:7090 \
  -e DB_HOST=161.132.53.229 \
  -e PUBLIC_BASE_URL=https://resultados.osteoperu.qallpatic.com.pe \
  -v /data/resultados-osteoperu:/data/resultados-osteoperu \
  back-envio-resultados-api
```

## Endpoints

| Método | Ruta                                | Auth   | Descripción                              |
|--------|-------------------------------------|--------|------------------------------------------|
| POST   | `/api/resultados/upload`            | Basic  | Sube resultados (multipart)              |
| GET    | `/s/{code}`                         | Público| Redirige el link corto (cuenta click)    |
| GET    | `/r/{token}`                        | Público| Visor de resultados (marca visto)        |
| GET    | `/r/{token}/archivo/{fileId}`       | Público| Ver archivo inline                       |
| GET    | `/r/{token}/descargar/{fileId}`     | Público| Descargar archivo                        |
| GET    | `/swagger-ui.html`                  | Público| Documentación OpenAPI                    |

Todas las rutas cuelgan del context-path `/back-envio-resultados-api`.

### Ejemplo de carga (curl)
```bash
curl -u osteoperu:Osteoperu@2026 \
  -F "phone=987654321" \
  -F "dni=12345678" \
  -F "patientName=Juan Perez" \
  -F "files=@resultado.pdf" \
  -F "files=@ecografia.jpg" \
  http://localhost:7090/back-envio-resultados-api/api/resultados/upload
```

## Nginx (producción, recomendado)

Servir el archivo grande vía la app o, para videos grandes, delegar a nginx (`X-Accel-Redirect`) como
en el proyecto base. La app ya envía `Accept-Ranges: bytes` para permitir seeking de video.
