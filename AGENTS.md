# AGENTS.md — app-osteoperu-resultados

> Instrucciones operativas y de convención para los agentes IA que trabajan en este
> repositorio. **Antes de tocar código, leé este archivo entero.** Si algo entra en
> conflicto con lo que el usuario te pide explícitamente en una sesión, gana el usuario.

---

## 0. Herramientas obligatorias del agente

Este repositorio está **indexado con CodeGraph** (directorio `.codegraph/`) y los
desarrolladores lo mantienen así a propósito para reducir tokens y tiempo de
búsqueda. Su uso es **obligatorio** y debe ser lo primero que hagas:

### 0.1 CodeGraph (inteligencia de código)

```bash
# ¿Cómo funciona X? ¿Cómo fluye Y? ¿Dónde está Z? → una sola llamada:
codegraph explore "<simbolos o pregunta en lenguaje natural>"

# Equivalente MCP (preferir si está disponible):
codegraph_explore(query="ResultadoService upload flow ShortLinkService")
```

Reglas:
- **NUNCA arranques con `grep`/`Read`/`find` cuando puedas preguntar a CodeGraph.**
  Una sola llamada a `codegraph_explore` devuelve el código verbatim línea-numerado
  (seguro de editar) + blast radius (quién llama a qué) + dynamic-dispatch hops.
- Si la respuesta tiene banners de staleness ("archivos editados desde el último
  sync"), re-leé SOLO esos archivos específicos. Los demás se confían.
- Si CodeGraph no está disponible (sin `.codegraph/`), caé a `Read`/`Grep`
  normalmente — nunca lo inicialices vos mismo, es decisión del usuario.

### 0.2 Engram (memoria persistente entre sesiones)

Engram guarda contexto que sobrevive a compactaciones y sesiones. **Usalo
proactivamente**, no solo cuando te lo pidan.

Herramientas que **debes** usar sin que te lo pidan:

| Cuándo | Herramienta | Por qué |
|---|---|---|
| Inicio de sesión | `mem_context` / `mem_current_project` | Ver qué se hizo antes |
| Decisión de arquitectura / patrón nuevo | `mem_save` (tipo `decision`/`architecture`/`pattern`) | No repetir el debate en sesiones futuras |
| Bugfix / workaround no obvio | `mem_save` (tipo `bugfix`) con **What/Why/Where/Learned** | Futuro agente evita el mismo pozo |
| Convención nueva del usuario | `mem_save` (tipo `pattern`) | Mantener consistencia |
| Final de sesión / tarea grande | `mem_session_summary` | Resumen estructurado Goal/Discoveries/Accomplished |
| Leés el prompt del usuario | `mem_save_prompt` | Trazabilidad |

Si `mem_save` devuelve `judgment_required: true` con candidatos:
- Confianza ≥ 0.7 y relación `related`/`compatible`/`scoped`/`not_conflict` → resolvés vos con `mem_judge`.
- Relación `supersedes`/`conflicts_with` en tipos `architecture`/`policy`/`decision`,
  o confianza < 0.7 → **preguntale al usuario antes de juzgar**.

---

## 1. Qué es este proyecto

Aplicación monolítica **Spring Boot 3.3 (Java 21) + Angular 19** para el envío
de resultados de pacientes de OSTEOPERU por SMS con link acortado y visor web.

### 1.1 Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Backend | Spring Boot (web, security, data-jpa, webflux, validation, thymeleaf, actuator) | 3.3.5 |
| Lenguaje backend | Java | 21 (eclipse-temurin) |
| Persistencia | MySQL + Flyway (migraciones en `src/main/resources/db/migration/`) | 8.x / Flyway 10.x |
| Mapeo Entity↔DTO | MapStruct + Lombok | 1.6.3 / 1.18.x |
| JWT | jjwt | 0.12.6 |
| Documentación API | springdoc-openapi (Swagger) | 2.6.0 |
| PDF | Apache PDFBox 3 (extracción de texto para validación de nombre) | 3.0.3 |
| Frontend | Angular standalone components + signals | 19.1 |
| Lenguaje frontend | TypeScript | 5.7 |
| Despliegue | Docker Compose multi-stage (Maven + Node dentro de Docker) | compose v2 |

### 1.2 Servicios en runtime

```
┌──────────────────────────────────────────────────────────────┐
│ osteoperu-net (bridge) + proxy-network (external, NPM)        │
│                                                               │
│  ┌──────────────────┐    ┌──────────────────────────────┐    │
│  │ osteoperu-backend│◀──▶│    MySQL (externo: server)    │    │
│  │ (Spring Boot)    │    │    DB: osteoperu_resultados    │    │
│  │ :7090  /osteoperu-api-resultados │ Flyway controla esquema│    │
│  └──┬───────────────┘    └──────────────────────────────┘    │
│     │ HTTP                                                  │
│     ▼                                                       │
│  ┌──────────────────┐                                       │
│  │osteoperu-frontend│──static──▶ Nginx ── /resultados/     │
│  │ (Angular) :80    │                                       │
│  └──────────────────┘                                       │
└──────────────────────────────────────────────────────────────┘
```

### 1.3 Roles (visibles en `ROL.NOMBRE` de la BD)

`ADMIN`, `RESULTADOS`, `ADMISION`, `PACIENTE`, `CALLCENTER` — los mapea
`UsuarioAutenticado.autoridadesDesdeRoles(...)` con prefijo `ROLE_`.

---

## 2. Estructura del repo

```
apps/app-osteoperu-resultados/
├── AGENTS.md                       ← este archivo
├── DEPLOY.md                       ← guía operativa de despliegue
├── docker-compose.yml              ← base parametrizada (DEV por defecto)
├── docker-compose.prod.yml         ← override producción (sin puertos host)
├── .env.example                    ← plantilla COMMITTED (placeholders)
├── .env.dev                        ← DEV COMMITTED (sin secretos reales)
├── .env.prod                       ← PROD GITIGNORED (secretos reales)
├── scripts/                        ← dev-up.sh, dev-down.sh, prod-up.sh, ...
├── uploads/                        ← volumen dev (no commitear contenido)
│
├── back-envio-resultados-api/       ← Spring Boot 3.3 / Java 21
│   ├── Dockerfile                  ← multi-stage: Maven build + JRE runtime
│   ├── pom.xml
│   └── src/main/
│       ├── java/pe/com/qallpatic/osteoperu/
│       │   ├── OsteoperuApplication.java
│       │   ├── admision/           ← admisión de pacientes (genera usuario+QR)
│       │   ├── compartido/         ← excepciones, respuestas, utilitarios, auditoría
│       │   ├── config/             ← AppProperties, Security, OpenAPI, WebClient, Sms, Storage
│       │   ├── integracion/
│       │   │   ├── pdf/            ← extracción de texto PDF (PDFBox)
│       │   │   └── reniec/         ← lookup DNI→nombre con cache BD
│       │   ├── paciente/           ← portal paciente (/r + /api/paciente)
│       │   ├── pacientes/          ← entity + repository del PACIENTE
│       │   ├── resultados/         ← módulo principal (carga, visor, short-link)
│       │   ├── seguridad/          ← JWT, login/refresh, usuarios, roles
│       │   └── usuarios/           ← CRUD admin de USUARIO
│       └── resources/
│           ├── application.yml              ← perfil base (común)
│           ├── application-local.yml        ← perfil `local` (dev)
│           ├── application-server.yml       ← perfil `server` (prod)
│           ├── logback-spring.xml
│           ├── templates/                   ← Thymeleaf (visor)
│           └── db/migration/V{1..N}__*.sql  ← Flyway (JAMÁS editar una V ya aplicada)
│
└── front-envio-resultados/          ← Angular 19 standalone
    ├── Dockerfile                  ← multi-stage: node:20-alpine build + nginx:1.27
    ├── nginx.conf                  ← SPA fallback a index.html
    ├── angular.json
    ├── package.json
    └── src/
        ├── environments/
        │   ├── environment.ts             ← PRODUCCIÓN (paths relativos)
        │   └── environment.development.ts ← DEV (http://localhost:7090/...)
        └── app/
            ├── app.component.ts
            ├── app.config.ts              ← providers globales (router, http, interceptors)
            ├── app.routes.ts              ← rutas + roleGuard
            ├── core/
            │   ├── auth/auth.service.ts   ← signals + JWT (login/refresh/logout)
            │   ├── guards/auth.guard.ts   ← authGuard, guestGuard, roleGuard
            │   ├── interceptors/          ← auth (Bearer) + error
            │   ├── layout/                ← MainLayoutComponent (shell)
            │   ├── models/                ← Sesion, TokenResponse
            │   └── services/              ← ReniecService, etc.
            └── features/
                ├── auth/login/
                ├── dashboard/
                ├── admin/                 ← gestión de usuarios (solo ADMIN)
                ├── admision/              ← admisión de pacientes
                ├── paciente/              ← portal paciente
                └── resultados/            ← carga/consulta/pendientes/logs
```

---

## 3. Comandos operativos

### 3.1 Levantar / bajar el stack

| Acción | DEV | PROD |
|---|---|---|
| Levantar | `./scripts/dev-up.sh` | `./scripts/prod-up.sh` |
| Bajar | `./scripts/dev-down.sh` | `./scripts/prod-down.sh` |
| Logs | `./scripts/dev-logs.sh` `[servicio]` | `./scripts/prod-logs.sh` `[servicio]` |
| Equivalente manual | `docker compose up -d --build` | `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build` |

Detalle clave: **el host NO necesita Java/Maven/Node**. Todo se compila dentro
de Docker. El primer build tarda 3-8 min (descarga Maven + deps), los
subsiguientes 10-30 s.

### 3.2 Iteración rápida (back dockerizado + front con HMR)

```bash
docker compose up -d backend         # solo el back
cd front-envio-resultados && npm start   # ng serve en :4200
# environment.development.ts ya apunta a http://localhost:7090/osteoperu-api-resultados
```

### 3.3 Backend (sin Docker, si el host tiene JDK 21)

```bash
./mvnw spring-boot:run                                   # perfil default
./mvnw spring-boot:run -Dspring-boot.run.profiles=local  # explícito
./mvnw test                                              # tests (suspendidos — ver §8)
./mvnw clean package                                     # genera target/*.jar
```

### 3.4 Frontend (sin Docker, si el host tiene Node 20+)

```bash
npm install
npm start      # ng serve en :4200
npm run build  # build de prod (con --base-href=/resultados/ via Dockerfile)
npm test       # ng test (Karma + Jasmine — sin tests aún, ver §8)
```

### 3.5 Accesos DEV

| Recurso | URL |
|---|---|
| Frontend (sub-path) | http://localhost:6060/resultados/ |
| Backend directo | http://localhost:7090/osteoperu-api-resultados/ |
| Swagger | http://localhost:7090/osteoperu-api-resultados/swagger-ui.html |
| Admin DEV | usuario `admin` / pass `Admin@2026` |

### 3.6 Snippets DB (problemas comunes)

```bash
docker compose exec backend ping -c 2 $DB_HOST         # conectividad
docker compose exec backend sh                          # shell en backend
docker compose logs --tail=100 backend
sudo lsof -i :7090                                      # "port already allocated"
```

---

## 4. Convenciones de código — Backend (Java / Spring)

### 4.1 Naming

- **Paquete raíz**: `pe.com.qallpatic.osteoperu.<modulo>`. Módulos actuales:
  `admision`, `compartido`, `config`, `integracion.{pdf,reniec}`, `paciente`,
  `pacientes`, `resultados`, `seguridad`, `usuarios`.
- **Idioma**: nombres de clases, métodos, comentarios en **español**.
  Tables/columns SQL en **MAYÚSCULAS** (respetado por Hibernate via
  `PhysicalNamingStrategyStandardImpl`).
- **DTOs**: records Java (`record`/`@Builder` si hace falta). En `dto/` de cada
  módulo, terminación `Response` (salida) o `Request` (entrada).
- **Entities**: terminación libre (`Resultado`, `EnlaceCorto`, `LogCarga`,
  `EventoVista`). Van en `entity/` del módulo, todos los nombres de tabla en
  MAYÚSCULAS con `@Table(name=...)`.
- **Servicios**: interface + `Impl`. Sufijo `Service`. Ej:
  `ResultadoService` (interface, métodos de negocio) — **NO** hay `Impl` aún;
  es solo interface y Spring usa proxy con los repos faltantes.
- **Controllers** REST: terminación `Controller`. Si expone Swagger, anotar
  con `@Tag(name=..., description=...)` y cada método con `@Operation`.

### 4.2 Lombok + MapStruct (pom.xml ya los configura)

Todas las entities y DTOs usan Lombok:

```java
@Entity
@Table(name = "RESULTADO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resultado extends EntidadAuditable { ... }
```

- **`@Slf4j`** para logging (`log.info(...)`).
- **`@RequiredArgsConstructor`** para inyección por constructor en services/controllers.
- **MapStruct** para mapeo Entity↔DTO. Los mappers viven en `mapper/` del módulo.

### 4.3 Manejo de errores

- Lanzar `ExcepcionNegocio` (negocio) o `SolicitudInvalidaException`/`RecursoNoEncontradoException` (cliente).
- `ManejadorGlobalExcepciones` (`@ControllerAdvice(annotations = RestController.class)`)
  mapea a `RespuestaError` con timestamp, status, path, mensaje. **NO** se aplica
  al visor Thymeleaf (`ViewerController` es `@Controller`, no `@RestController`).
- Responder SIEMPRE con `RespuestaApi<T>` (envoltorio) en endpoints REST paginados,
  o DTO directo en endpoints simples (ej. `POST /upload`).

### 4.4 Transacciones

- `@Transactional` en métodos de servicio que mutan estado.
- `spring.jpa.open-in-view: false` — cerrar el EntityManager manualmente o usar
  fetch joins; **no** acceder a lazy associations desde el controller.

### 4.5 Auditoría

- Heredar de `EntidadAuditable` (en `compartido.auditoria`) para tener
  `setUsuarioCreacion(...)`, `setUsuarioModificacion(...)`, fechas.
- `EntidadAuditable` usa JPA `@PrePersist`/`@PreUpdate` (o similar).

### 4.6 Validación

- Bean Validation (`@Valid`, `@Size`, `@NotBlank`) en DTOs y `@RequestParam` validados.
- `ResultadoController` ya usa `@Validated` para `multipart` con `@Size(max = 500)`.

### 4.7 Migraciones Flyway

- Versionadas: `V{n}__{descripcion}.sql`. **Inmutables** una vez commiteadas
  (Flyway rechaza edits sobre versiones ya aplicadas → checksum mismatch).
- Si la migración aún no se aplicó y TODAVÍA no se commiteó, se puede ajustar
  (mejor: crear `V{n+1}__...` y dejar la otra indicada como "do not apply").
- Convención: `ID_TABLA`, columnas `MAYUSCULA_SNAKE`, FKs `FK_TABLA_COL_REF`,
  índices únicos `UK_TABLA_COL`. Charset `utf8mb4 / utf8mb4_unicode_ci`.
- Migraciones existentes: `V1__init_schema`, `V2__add_mensaje_resultado`,
  `V3__drop_comprobante_pago`, `V4__paciente_pendiente_resultados`,
  `V5__resultado_telefono_nullable`, `V6__reniec_cache`,
  `V7__usuario_paciente_segmentado`.

### 4.8 Configuración

- **Nada hardcodeado**: cualquier valor de negocio sale de `application.yml` /
  `application-{perfil}.yml` y se sobreescribe por variable de entorno.
- Las props custom se bindean con `@ConfigurationProperties(prefix = "...")`
  (ej: `AppProperties`, `SmsProperties`, `StorageProperties`, `SeguridadProperties`,
  `ReniecProperties`). Tipos en `config/`.

### 4.9 Logging

- `@Slf4j` (Lombok).
- Patrón en `logback-spring.xml` y `application.yml`.
- Logs clave: `INICIO` / `FIN` de operaciones (`ResultadoService.uploadResultados`
  ya lo hace). Loggear `id`/`dni`/`user`, **nunca** datos sensibles completos
  (passwords, JWT, tokens de proveedor).

### 4.10 Seguridad

- Stateless con JWT (HS256). Filtro `FiltroAutenticacionJwt`, login/refresh en
  `/api/auth/*`.
- Roles via `ROLE_<NOMBRE>` en `UserDetails` (ver `UsuarioAutenticado`).
- Rutas públicas: `/api/auth/**`, `/s/**` (acortador), `/r/**` (visor), swagger, actuator health.
- Resto: `@EnableMethodSecurity` y `@PreAuthorize("hasRole('ADMIN')")` o
  `hasAnyRole('ADMIN', 'RESULTADOS')`.
- **NUNCA** loggear ni retornar el password en claro, ni el JWT secret, ni
  tokens de RENIEC/SMS. Los logs a `DEBUG` no deben incluir Authorization headers.

### 4.11 Integración con proveedores externos

- `RENIEC` (`integracion/reniec/`): interface `ClienteReniec` con dos
  implementaciones (`ClienteReniecHttp` real, `ClienteReniecMock`).
  `ClienteReniecConCache` cachea en BD (`reniec_cache`) con TTL.
- `SMS` (`integracion/.../SmsService`): usa WebClient (`spring-webflux`) contra
  gateway Textbee. Encolar y enviar con `frequency-ms` configurable. Si
  `sms.enabled=false`, solo loggear.
- Variables en `.yml`:
  ```yaml
  sms:
    enabled: ${SMS_ENABLED:false}
    textbee: { url, api-key, frequency-ms }
    message: { template, credencial-template }
  reniec:
    enabled: ${RENIEC_ENABLED:true}
    url: ${RENIEC_URL:...}
    token: ${RENIEC_TOKEN:...}
    timeout-ms: 6000
    cache-dias: 180
  ```

### 4.12 Almacenamiento (FS local — fase actual)

- `STORAGE_BASE_PATH=/app/uploads` (contenedor) → `UPLOADS_PATH` (host).
- Convenciones de nombres:
  - Carpeta del paciente: `{telefono}_{dni}` (sin caracteres especiales).
    Construirla con `StorageService.buildFolderName(phone, dni)`.
  - Nombre del archivo almacenado: lo decide `StorageService.store(...)` (suele
    añadir un slug aleatorio para evitar colisiones — no asumir patrón humano).
  - Persistir SIEMPRE `nombreOriginal` + `nombreAlmacenado` + `tamanoBytes` +
    `tipoContenido` + `tipoMedio` (`TipoMedio.desdeTipoContenido(...)`).
- Permitir solo `application/pdf`, `image/{jpeg,png,webp,gif}`,
  `video/{mp4,quicktime,webm}`. Editar `storage.allowed-content-types`.
- Límite: `50 MB` por archivo, `250 MB` por request (`spring.servlet.multipart.*`).

### 4.13 Zona horaria

- JVM del contenedor corre con `-Duser.timezone=$TZ`. **Importante**:
  `LocalDateTime.now()` devuelve hora Lima si `TZ=America/Lima`. No usar
  `Instant` sin conversión cuando el dato es para mostrar al usuario peruano.

---

## 5. Convenciones de código — Frontend (Angular 19)

### 5.1 Naming y estructura

- **Standalone components** (sin `NgModule`). `RouterModule` ya no se usa.
- Idiomas: **español** para nombres de clases, métodos, archivos
  (`cargar-resultados.component.ts`, no `load-results.component.ts`).
- Estructura por feature: `src/app/features/<feature>/<feature>.component.ts`.
  Service de la feature: `<feature>.service.ts`. Modelos: `<feature>.models.ts`.
- Compartido (auth, guards, interceptors, layout) en `src/app/core/`.
- Lazy loading: cada ruta standalone usa `loadComponent: () => import(...)`.

### 5.2 Routing y guards

- Definir rutas en `app.routes.ts` con `authGuard`, `guestGuard` y
  `roleGuard(['ADMIN', 'RESULTADOS'])` (los roles van como strings exactos del
  enum en backend).
- Rutas no encontradas → `redirectTo: ''`.

### 5.3 Signals + RxJS

- `AuthService` usa **signals** (`_sesion`, `autenticado`, `roles`,
  `rolPrincipal`) + `localStorage` para persistir tokens.
- HTTP sigue siendo RxJS `Observable`, pero al consumir services en templates
  conviene usar signals; sino `| async`.
- **Single-flight refresh** ya implementado en `AuthService.refresh()` con
  `shareReplay(1)` + `refrescando$` cacheado.

### 5.4 Environments

- `environment.ts` (default, production) usa **paths relativos**:
  `apiBaseUrl: '/resultados/osteoperu-api-resultados'` (servido bajo Nginx que
  proxyifica `/resultados/osteoperu-api-resultados → backend`).
- `environment.development.ts` (usado con `ng serve`) usa `localhost:7090`.
- **No commitear** secrets ni URLs hardcodeadas distintas de las dos environments.

### 5.5 HTTP y errores

- `authInterceptor`: añade `Authorization: Bearer <token>` salvo `/api/auth/`.
- `errorInterceptor`: mapea `RespuestaApi.mensaje` / `RespuestaError.mensaje`
  a notificaciones al usuario (snackbar / toast).
- Modelar TODA respuesta como `RespuestaApi<T>` en services y `.pipe(map(r => r.data))`.
- Para FormData (upload): `form.append('confirmarNombre', String(req.confirmarNombre))`
  (NO booleano crudo, Spring lo rechaza).

### 5.6 Forms y UX

- Validaciones: DNI/CE con regex; teléfono peruano `^9\d{8}$` (sin prefijo);
  usar `@angular/forms` reactivamente.
- Mensaje del agente es **obligatorio** cuando se envían resultados (recordado
  en commits previos del proyecto).
- En las 5 vistas con tablas (`logs-carga`, `consultar-resultados`,
  `pendientes`, `usuarios-admin`, `admision`) el patrón aplicado es
  **header sticky + scroll vertical SOLO en `<tbody>`** sin tocar HTML/TS
  básico — mantenerlo si se replica.
- Dropzone de archivos: scrollable, contador de archivos visible.

### 5.7 Standalone

```typescript
@Component({
  selector: 'app-cargar-resultados',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, /* ... */],
  templateUrl: './cargar-resultados.component.html',
  styleUrl: './cargar-resultados.component.css', // o styles: [...]
})
export default class CargarResultadosComponent {}
```

### 5.8 Estilo

- Sin emojis (regla global).
- Sin comentarios innecesarios.
- **Indentación**: ver `.editorconfig`. Sin tabs en `.ts`/`.html`.
- No inventes componentes sin chequear primero `app.routes.ts` y la lista en
  `features/` — usar `codegraph_explore` para encontrarlos rápido.

---

## 6. Flujos críticos (patrones a respetar)

### 6.1 Carga de resultados → SMS al paciente

```
POST /api/resultados/upload (JWT, multipart)
   └─ ResultadoController.upload()
       └─ ResultadoService.uploadResultados()            ← @Transactional
           ├─ UtilTelefono.normalizar(phone, "51")
           ├─ UtilTelefono.validarDocumentoIdentidad(dni)
           ├─ validarNombre(dni, files)   ← RENIEC + PDFBox (opcional)
           │   └─ Si RENIEC existe Y PDF no coincide → requiereConfirmacion=true
           ├─ storageService.ensurePatientFolder({telefono}_{dni})
           ├─ for each file: storageService.store(...)
           ├─ resultadoRepository.save(resultado)
           ├─ registrarPacienteConResultados(dni, ...)
           ├─ shortLinkService.create(viewerUrl, "RESULTADO", id)
           ├─ if (tieneTelefono) smsService.enqueueResultLink(...)
           └─ registrarLog(...)  ← LOG_CARGA (auditoría bitácora)
```

### 6.2 Paciente abre el link del SMS

```
GET /s/{codigoCorto}     (302)  →  /r/{token}
                              ↓
GET /r/{token}           ViewerController.viewResults()  ← Thymeleaf (HTML)
                          ├─ Incrementa view_count, marca first_viewed_at
                          └─ Lista archivos con links a /r/{token}/archivo/{fileId}
GET /r/{token}/archivo/{fileId}   ViewerController.serve()  ← Streaming inline
GET /r/{token}/descargar/{fileId} ViewerController.download()
```

### 6.3 Login + refresh (frontend)

```
/api/auth/login    → { accessToken (15 min), refreshToken (7 días) }
                    se guardan en localStorage y signal<_sesion>

[access expira]
   → 401 → authInterceptor detecta → llama AuthService.refresh()
   → /api/auth/refresh { refreshToken } → { accessToken, refreshToken }
   → reintenta la request original (single-flight, shareReplay)
[refresh expira]
   → 401 → limpiaSesion() → redirect a /login
```

### 6.4 Validación de nombre contra RENIEC

`ResultadoService.validarNombre(dni, files)`:
1. `clienteReniec.consultar(dni)` → DatosReniec (con cache de BD, TTL
   `reniec.cache-dias`).
2. Si no hay PDF → devuelve "no verificado" (no bloquea).
3. Si hay PDF → extrae texto con `validadorPdf.extraerTexto(pdfBytes)`, lo
   normaliza y compara token-por-token (`UMBRAL_COINCIDENCIA = 0.6`).
4. Si NO coincide y `confirmarNombre=false` → responde `requiereConfirmacion=true`
   para que el front reintente con `confirmarNombre=true`.

> **Si toca modificar esta lógica**: el patrón exige normalización via
> `NormalizadorTexto` y comparación por tokens (≥ 3 chars), no por substring.

---

## 7. Secretos y configuración — REGLAS DURAS

| Regla | Por qué |
|---|---|
| **NUNCA** commitear `.env.prod`, `.env.local`, ni valores reales de `JWT_SECRET`, `DB_PASSWORD`, `RENIEC_TOKEN`, `SMS_API_KEY` | Están gitignored pero un descuido es irreparable |
| En `.env.dev` podés tener valores placeholder/ilimitados de RENIEC, pero **jamás** un `JWT_SECRET` real | Sirve para que dev arranque pero no es usable en prod |
| `JWT_SECRET` debe tener **≥ 32 bytes**. Generar con `openssl rand -base64 48` | HS256 lo exige; valores cortos rompen el arranque |
| Defaults inseguros en `application.yml` solo si la doc dice "se sobreescribe por ENV en prod" | Mismas vars deben existir en `.env.example` |
| El sembrado del usuario admin es **idempotente** (no actualiza si existe). Para rotar password, UPDATE directo en BD | Conocido bug-by-design documentado en `DEPLOY.md §7` |
| `STORAGE_BASE_PATH` se monta del host → `UPLOADS_PATH` debe ser **un directorio preexistente** que el host pueda escribir | Permisos rotos = no arrancan los uploads |
| Compilar commit seguro: si tocás `application.yml`, dejá comentario claro de qué ENV lo sobreescribe en prod | Sin contexto, futuras manos rompen prod |

---

## 8. Testing

**Estado actual**: el proyecto NO tiene tests (gradle: `⚠️ no covering tests found`
que reporta CodeGraph para la mayoría de los services). El `pom.xml` incluye
`spring-boot-starter-test` y `spring-security-test` listos para usarse, pero no
hay archivos `*Test.java`/`*.spec.ts`.

### 8.1 Si vas a agregar tests (recomendado):

- Backend: JUnit 5 + Spring Boot Test + MockMvc para controllers; `@DataJpaTest`
  para repos; `@WebMvcTest` para filtros de seguridad.
- Frontend: Karma + Jasmine (ya en `package.json`) — apuntar a services de
  features (`ResultadosService`, `AuthService.refresh()` con single-flight).
- Si tocás un método: si era testeable antes, agregale el test; si no, no
  introduzcas tests con mala calidad solo por sumar.

### 8.2 Verificación manual mínima

- `./scripts/dev-up.sh` → esperar a que ambos containers arranquen
  (`docker compose ps` → `healthy` o `running`).
- `curl -u admin:Admin@2026 http://localhost:7090/osteoperu-api-resultados/api/resultados/1`
  (con token real cambiando `:7090/...` por `/login` primero).
- `curl http://localhost:6060/resultados/` → HTML con `<app-root>`.

---

## 9. Cuando termines una tarea: checklist del agente

Antes de decir "listo", verificá:

1. [ ] **Lint / format**: si tocan `.java`, respetá el estilo Lombok + 4 spaces
       (no hay checkstyle.xml configurado; usar estilo existente en el módulo).
       Si tocan `.ts`, mantené el estilo del archivo (puntos y comas, comillas
       simples en strings TS).
2. [ ] **Si hay cambio en migraciones Flyway**: revisá que las V ya aplicadas
       NO se editen; si hace falta, nueva `V(n+1)__*.sql`. Nunca borrar V antiguas.
3. [ ] **Si agregaste endpoint nuevo**:
       - Anotar `@Operation(...)` + `@Tag(...)` (swagger).
       - Si es público, autorizarlo en `SecurityConfig`.
       - Si es solo para ciertos roles, agregar `@PreAuthorize(...)` o
         `roleGuard([...])` en el front.
4. [ ] **Si tocaste `application.yml`**: verificá que la nueva variable tenga
       default sensato y entrada en `.env.example`.
5. [ ] **Si tocaste el modelo (entity nueva / columna nueva)**:
       - Migración V{n+1} que la cree + backfill si hace falta.
       - Lombok con `@Builder/@Getter/@Setter/@NoArgsConstructor` (consistencia).
       - Actualizar DTOs + Mapper MapStruct.
6. [ ] **Si tocaste el front**: verificá que la ruta exista en `app.routes.ts`
       y el feature tenga su `*.models.ts` + `*.service.ts`.
7. [ ] **Memoria**: si fue una decisión, patrón nuevo o bugfix no obvio,
       `mem_save` con `**What/Why/Where/Learned**`. Si fue sesión grande,
       `mem_session_summary` antes de cerrar.
8. [ ] **No commitear** salvo que el usuario lo pida explícitamente.

---

## 10. Anti-patrones — lo que NO hacer

- ❌ Hardcodear URLs, IPs, contraseñas, secretos, paths absolutos en código.
- ❌ Editar migraciones Flyway ya aplicadas (cambia el checksum y rompe el próximo arranque).
- ❌ Usar `ddl-auto: update` o `create` — **siempre `none`** (lo controla Flyway).
- ❌ Persistir archivos fuera del directorio configurado en `STORAGE_BASE_PATH`.
- ❌ Loggear `Authorization` headers completos, JWTs, passwords, tokens de proveedor.
- ❌ Devolver `Map<String,Object>` plano: usar DTOs (records) tipados.
- ❌ Usar `open-in-view: true` (ya está `false`) — no cambiarlos.
- ❌ Inyectar `HttpClient` fuera de services; usar siempre `inject(HttpClient)`.
- ❌ Usar `subscribe()` sin manejar error o sin desuscribir (preferir `firstValueFrom`
       o pipe en template con `| async`).
- ❌ Crear archivos con camelCase o inglés en el FE — el repo es **español**.
- ❌ Crear componentes/pipes/directives desde cero sin chequear si ya existen.
- ❌ Cambiar el contexto del path (`CONTEXT_PATH`) sin tocar `compose`, `nginx.conf`
       y `apiBaseUrl` de environments a la vez.
- ❌ Tocar `docker-compose.prod.yml` a mano para "depurar": hacer un override nuevo.
- ❌ Asumir que un valor de `application.yml` es el real: siempre verificar
       `.env.dev` / `.env.prod` y si hace falta el ENV vars real del contenedor
       (`docker compose exec backend env`).

---

## 11. Cómo pedir contexto a CodeGraph — ejemplos prácticos

```bash
# ¿Cómo funciona la carga de resultados end-to-end?
codegraph explore "ResultadoService uploadResultados flow completo"

# ¿Dónde se valida la coincidencia de nombre RENIEC vs PDF?
codegraph explore "ValidadorPdf ResultadoService validarNombre UMBRAL_COINCIDENCIA"

# ¿Cómo se envían los SMS y dónde se encolan?
codegraph explore "SmsService enqueueResultLink Textbee frequency-ms cola"

# ¿Quién llama a ShortLinkService y qué pasa si lo cambio?
codegraph explore "ShortLinkService callers blast radius"

# ¿Cómo está armado el JWT filter y por qué roles pasa mi endpoint?
codegraph explore "FiltroAutenticacionJwt ProveedorJwt UsuarioAutenticado SecurityConfig"

# ¿Cómo se conecta el visor de resultados con el storage físico?
codegraph explore "ViewerController serve inline StorageService FileSystemResource"

# ¿Dónde está la lógica de roles en el frontend?
codegraph explore "roleGuard authGuard auth.service signals JWT refresh"

# Si una query devuelve NULL: revisar el banner de staleness arriba de la respuesta
# y re-leer SOLO los archivos listados como desactualizados.
```

---

## 12. Glosario de términos del dominio

| Término | Significado |
|---|---|
| Lote / batch | Cada `Resultado` (uno por `telefono+dni`, con N archivos adentro) |
| Token público | UUID-like de 24 chars en `RESULTADO.TOKEN_PUBLICO` (el del visor `/r/...`) |
| Código corto | 7 chars en `ENLACE_CORTO.CODIGO_CORTO` (el del SMS `/s/...`) |
| Visor | Página Thymeleaf `/r/{token}` que lista los archivos y marca `first_viewed_at` |
| Acortador | Redirección 302 `/s/{code}` → `/r/{token}` que cuenta `click_count` |
| Pendiente | `PACIENTE.PENDIENTE_RESULTADOS=true` cuando tiene resultados pero no se subió ninguno todavía |
| Nombre canónico | Formato único `"APELLIDO_P APELLIDO_M, NOMBRES"` (ver engram) |
| Credencial | Usuario+clave emitidos por admisión al paciente (DNI=user, password aleatorio) + QR al portal |

---

> **Recordatorio final**: si modificás cualquier archivo listado en este AGENTS.md,
> dejá el resto del repo consistente (rutas, environments, scripts de compose, etc.).
> Y si tu cambio altera cómo se despliega o se configura algo: actualizá también
> `DEPLOY.md`. La documentación operativa es tan importante como el código.
