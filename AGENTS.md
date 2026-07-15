# AGENTS.md — app-osteoperu-resultados

> Instrucciones operativas y de convención para los agentes IA. **Antes de tocar
> código, leé este archivo.** Si algo entra en conflicto con lo que el usuario te
> pide explícitamente en una sesión, gana el usuario.
>
> Si en cualquier momento detectás que una instrucción, cambio o decisión tuya
> entra en conflicto con la estructura/arquitectura actual del proyecto, o si
> tenés dudas sobre qué patrón aplicar → **preguntá antes de actuar**.
> Una pregunta a tiempo evita romper features existentes y mejora la precisión
> del resultado.

---

## 0. Herramientas obligatorias

### 0.1 CodeGraph (inteligencia de código)

Este repo está indexado con CodeGraph (directorio `.codegraph/`). **Usalo primero,
siempre**:

```bash
codegraph explore "<símbolos o pregunta en lenguaje natural>"
# o equivalente MCP: codegraph_explore(query="...")
```

Reglas:
- **NUNCA arranques con `grep`/`Read`/`find`** cuando puedas preguntar a CodeGraph.
  Una sola llamada devuelve código verbatim línea-numerado + blast radius (quién
  llama a qué) + dynamic-dispatch hops.
- Si el resultado tiene banner de staleness, re-leé SOLO los archivos listados.
- Si CodeGraph no está disponible (sin `.codegraph/`), caé a `Read`/`Grep`.

### 0.2 Engram (memoria persistente)

Usalo proactivamente, no solo cuando te lo pidan:

| Cuándo | Herramienta |
|---|---|
| Inicio de sesión | `mem_context` / `mem_current_project` |
| Decisión de arquitectura / patrón nuevo | `mem_save` (tipo `decision`/`architecture`/`pattern`) |
| Bugfix / workaround no obvio | `mem_save` (tipo `bugfix`) con **What/Why/Where/Learned** |
| Convención nueva | `mem_save` (tipo `pattern`) |
| Final de sesión / tarea grande | `mem_session_summary` |
| Leés el prompt del usuario | `mem_save_prompt` |

Si `mem_save` devuelve `judgment_required: true` con confianza < 0.7, o con
relación `supersedes`/`conflicts_with` sobre `architecture`/`policy`/`decision`
→ **preguntale al usuario antes de juzgar**.

---

## 1. El proyecto en breve

Monolito **Spring Boot 3.3 (Java 21) + Angular 19** para envío de resultados de
pacientes OSTEOPERU por SMS con link acortado + visor web.

- **Backend**: web, security, data-jpa, webflux (WebClient para SMS),
  validation, thymeleaf (visor), MapStruct + Lombok, jjwt 0.12.6,
  springdoc-openapi 2.6.0, Apache PDFBox 3 (extrae texto PDF para validar nombre
  vs RENIEC), Flyway 10 + MySQL 8.
- **Frontend**: Angular 19 standalone + signals + RxJS, Karma+Jasmine (sin
  tests aún), entornos separados dev/prod.
- **Despliegue**: Docker Compose multi-stage. **El host NO necesita Java/Maven/Node**.
  Primer build 3-8 min, subsiguientes 10-30 s.

Roles: `ADMIN`, `RESULTADOS`, `ADMISION`, `PACIENTE`, `CALLCENTER` (con prefijo
`ROLE_` en Spring Security).

Estructura: `back-envio-resultados-api/` (Spring Boot) + `front-envio-resultados/`
(Angular). Módulos backend: `admision`, `compartido`, `config`, `integracion.{pdf,reniec}`,
`paciente`, `pacientes`, `resultados`, `seguridad`, `usuarios`.

---

## 2. Comandos clave

| Acción | Comando |
|---|---|
| Levantar DEV | `./scripts/dev-up.sh` (= `docker compose up -d --build`) |
| Bajar DEV | `./scripts/dev-down.sh` |
| Logs | `./scripts/dev-logs.sh [servicio]` |
| Solo back dockerizado + front con HMR | `docker compose up -d backend` + `cd front-envio-resultados && npm start` |
| Backend sin Docker (JDK 21) | `./mvnw spring-boot:run` |
| Front sin Docker (Node 20+) | `npm start` / `npm run build` / `npm test` |

Accesos DEV:
- Frontend: `http://localhost:6060/resultados/`
- Backend: `http://localhost:7090/osteoperu-api-resultados/`
- Swagger: `http://localhost:7090/osteoperu-api-resultados/swagger-ui.html`
- Admin DEV: `admin / Admin@2026`

Para PROD ver `DEPLOY.md`.

---

## 3. Convenciones backend (Java / Spring)

- **Paquete raíz**: `pe.com.qallpatic.osteoperu.<modulo>`. **Nombres en español**.
- **SQL en MAYÚSCULAS** (respeta nombres vía `PhysicalNamingStrategyStandardImpl`).
- **Lombok**: entities con `@Entity @Table(name="TABLA") @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder`. Services/controllers con
  `@RequiredArgsConstructor @Slf4j`.
- **MapStruct** para Entity↔DTO (viven en `mapper/` del módulo).
- **DTOs** son `record` (o `@Builder` si hace falta), terminación `Request`/`Response`.
- **Manejo de errores**: lanzar `ExcepcionNegocio` (negocio) o
  `SolicitudInvalidaException`/`RecursoNoEncontradoException` (cliente). El
  `ManejadorGlobalExcepciones` (`@ControllerAdvice(annotations = RestController.class)`)
  mapea a `RespuestaError`. **No** se aplica al visor Thymeleaf.
- **Respuestas**: usar `RespuestaApi<T>` (envoltorio) en endpoints REST
  paginados; DTO directo en endpoints simples.
- **Transacciones**: `@Transactional` en services que mutan. `open-in-view: false`
  → fetch joins, no acceder a lazy desde controller.
- **Auditoría**: heredar de `EntidadAuditable` (`compartido.auditoria`).
- **Validación**: Bean Validation (`@Valid`, `@Size`, `@NotBlank`). Para
  `@RequestParam` usar `@Validated` en el controller.
- **Seguridad**: JWT HS256 stateless (`FiltroAutenticacionJwt`). Rutas públicas:
  `/api/auth/**`, `/s/**`, `/r/**`, swagger, actuator health. Resto:
  `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`.
- **Flyway**: migraciones versionadas `V{n}__desc.sql`. **Inmutables** una vez
  aplicadas (checksum mismatch rompe arranque). Nueva columna/cambio → nueva
  `V(n+1)__...sql`. Convención: `ID_TABLA`, `MAYUSCULA_SNAKE`, FKs
  `FK_TABLA_COL_REF`, UKs `UK_TABLA_COL`, charset `utf8mb4 / utf8mb4_unicode_ci`.
- **Configuración**: **nada hardcodeado**. Todo en `application.yml` /
  `application-{perfil}.yml`, sobreescribible por ENV. Props custom via
  `@ConfigurationProperties(prefix="...")` en `config/`.
- **Logging**: `@Slf4j`, logs `INICIO`/`FIN` en operaciones clave. **Nunca**
  loggear passwords, JWTs, Authorization headers completos, tokens RENIEC/SMS.
- **Storage**: `STORAGE_BASE_PATH=/app/uploads` (contenedor) → `UPLOADS_PATH`
  (host). Carpeta paciente: `{telefono}_{dni}` (`StorageService.buildFolderName`).
  Persistir SIEMPRE `nombreOriginal` + `nombreAlmacenado` + `tamanoBytes` +
  `tipoContenido` + `tipoMedio`. Tipos permitidos en `storage.allowed-content-types`.
  Límite 50 MB/archivo, 250 MB/request (`spring.servlet.multipart.*`).
- **Zona horaria**: JVM con `-Duser.timezone=$TZ` (default `America/Lima`).
  `LocalDateTime.now()` devuelve hora Lima si TZ está bien seteado.

---

## 4. Convenciones frontend (Angular 19)

- **Standalone components** (sin `NgModule`). Nombres en **español**.
- Estructura por feature: `src/app/features/<feature>/<feature>.component.ts`.
  Service: `<feature>.service.ts`. Modelos: `<feature>.models.ts`. Compartido
  en `src/app/core/`. Lazy loading con `loadComponent: () => import(...)`.
- **Routing**: `app.routes.ts` con `authGuard`, `guestGuard`, `roleGuard(['ADMIN','RESULTADOS'])`.
- **Auth con signals** (`AuthService`): `_sesion`, `autenticado`, `roles`,
  `rolPrincipal`, tokens en `localStorage`. **Single-flight refresh** con
  `shareReplay(1)` + `refrescando$` cacheado.
- **Environments**:
  - `environment.ts` (prod) usa **paths relativos**: `apiBaseUrl: '/resultados/osteoperu-api-resultados'`.
  - `environment.development.ts` apunta a `http://localhost:7090/osteoperu-api-resultados`.
  - **No commitear** secrets ni otras URLs.
- **HTTP**: `authInterceptor` añade `Bearer <token>` salvo `/api/auth/`.
  `errorInterceptor` mapea errores a snackbars. Toda respuesta paginada es
  `RespuestaApi<T>` → `.pipe(map(r => r.data))`.
- **FormData (upload)**: `form.append('confirmarNombre', String(req.confirmarNombre))`
  (NO booleano crudo, Spring lo rechaza).
- **Validaciones reactivas**: DNI 8 dígitos, CE 9-12 dígitos, teléfono `^9\d{8}$`.
- **Inyección**: usar siempre `inject(HttpClient)` (no constructor).
- **Pipes en template**: preferir `| async` o signals; evitar `subscribe()`
  sin manejar error.
- **Tablas con scroll**: en las 5 vistas con tablas (`logs-carga`,
  `consultar-resultados`, `pendientes`, `usuarios-admin`, `admision`) el patrón
  es **header sticky + scroll vertical SOLO en `<tbody>`** sin tocar HTML/TS
  básico — mantenerlo si se replica.
- **Sin emojis** (regla global). Sin comentarios innecesarios.

---

## 5. Flujos críticos

### 5.1 Carga de resultados → SMS

```
POST /api/resultados/upload (JWT, multipart)
   └─ ResultadoController.upload()
       └─ ResultadoService.uploadResultados() @Transactional
           ├─ UtilTelefono.normalizar(phone, "51") + validarDocumentoIdentidad(dni)
           ├─ validarNombre(dni, files)   ← RENIEC + PDFBox
           │   └─ Si RENIEC existe Y PDF no coincide → requiereConfirmacion=true
           ├─ storageService.ensurePatientFolder({telefono}_{dni})
           ├─ for each file: storageService.store(...)
           ├─ resultadoRepository.save(resultado)
           ├─ registrarPacienteConResultados(dni, ...)
           ├─ shortLinkService.create(viewerUrl, "RESULTADO", id)
           ├─ if (tieneTelefono) smsService.enqueueResultLink(...)
           └─ registrarLog(...)  ← LOG_CARGA (bitácora)
```

### 5.2 Paciente abre el link

```
GET /s/{codigoCorto}  (302) → /r/{token}
GET /r/{token}  ViewerController (Thymeleaf, HTML, marca first_viewed_at)
GET /r/{token}/archivo/{fileId}    ViewerController.serve()    ← inline
GET /r/{token}/descargar/{fileId}  ViewerController.download()
```

### 5.3 Login + refresh

```
/api/auth/login    → { accessToken (15 min), refreshToken (7 días) }
[access expira]    → 401 → authInterceptor → AuthService.refresh() (single-flight)
                    → /api/auth/refresh { refreshToken } → reintenta request
[refresh expira]   → 401 → limpiaSesion() → redirect a /login
```

### 5.4 Validación RENIEC vs PDF

`ResultadoService.validarNombre(dni, files)`:
1. `clienteReniec.consultar(dni)` → `DatosReniec` (con cache BD, TTL
   `reniec.cache-dias`).
2. Si no hay PDF → "no verificado" (no bloquea).
3. Si hay PDF → `validadorPdf.extraerTexto(pdfBytes)`, normaliza y compara
   token-por-token (`UMBRAL_COINCIDENCIA = 0.6`, tokens ≥ 3 chars via
   `NormalizadorTexto`).
4. Si NO coincide y `confirmarNombre=false` → responde `requiereConfirmacion=true`.

---

## 6. Secretos y configuración

| Regla | Por qué |
|---|---|
| **NUNCA** commitear `.env.prod`, `.env.local`, ni `JWT_SECRET`/`DB_PASSWORD`/`RENIEC_TOKEN`/`SMS_API_KEY` reales | Gitignored pero un descuido es irreparable |
| `.env.dev` puede tener placeholders de RENIEC, **jamás** `JWT_SECRET` real | Sirve para dev pero no es usable en prod |
| `JWT_SECRET` ≥ 32 bytes (`openssl rand -base64 48`) | HS256 lo exige |
| Sembrado de admin es **idempotente** (no actualiza si existe) | Para rotar: UPDATE directo en BD |
| `UPLOADS_PATH` debe ser directorio preexistente escribible | Permisos rotos = no arrancan uploads |
| Si tocás `application.yml`, dejá comentario de qué ENV lo sobreescribe en prod | Sin contexto, futuras manos rompen prod |

---

## 7. Testing

**Estado actual**: NO hay tests. `pom.xml` incluye `spring-boot-starter-test`
+ `spring-security-test` y `package.json` tiene Karma+Jasmine, pero no hay
archivos `*Test.java`/`*.spec.ts`.

Si vas a agregar: JUnit 5 + Spring Boot Test + MockMvc en controllers;
`@DataJpaTest` para repos. Front: apuntar a `ResultadosService`,
`AuthService.refresh()` (single-flight). Si el método era testeable antes,
agregale test; si no, no introduzcas tests con mala calidad solo por sumar.

**Verificación manual mínima**: `./scripts/dev-up.sh` + `curl` a backend/swagger
+ `curl http://localhost:6060/resultados/` (debe devolver `<app-root>`).

---

## 8. Checklist de cierre

Antes de decir "listo":

1. Estilo consistente con el módulo (Lombok + 4 spaces en Java; estilo del
   archivo en TS).
2. Si tocás Flyway: NO edites V ya aplicadas; nueva `V(n+1)__*.sql` si hace falta.
3. Endpoint nuevo: `@Operation` + `@Tag`, autorizar en `SecurityConfig` o
   `roleGuard` en front.
4. `application.yml` modificado: default sensato + entrada en `.env.example`.
5. Modelo/entity nuevo: migración `V(n+1)`, Lombok completo, DTO + Mapper
   actualizados.
6. Front: ruta en `app.routes.ts` + `*.models.ts` + `*.service.ts`.
7. Memoria: `mem_save` con `**What/Why/Where/Learned**` si fue decisión,
   patrón o bugfix no obvio. `mem_session_summary` antes de cerrar si fue
   sesión grande.
8. **No commitear** salvo que el usuario lo pida explícitamente.

---

## 9. Anti-patrones

- ❌ Hardcodear URLs, IPs, secretos, paths absolutos en código.
- ❌ Editar migraciones Flyway ya aplicadas.
- ❌ `ddl-auto: update`/`create` — **siempre `none`** (lo controla Flyway).
- ❌ Persistir archivos fuera de `STORAGE_BASE_PATH`.
- ❌ Loggear Authorization headers, JWTs, passwords, tokens de proveedor.
- ❌ Devolver `Map<String,Object>` plano — usar DTOs tipados.
- ❌ `open-in-view: true` — ya está `false`, no cambiarlo.
- ❌ `HttpClient` por constructor — usar `inject(HttpClient)`.
- ❌ `subscribe()` sin manejar error/desuscribir — preferir `firstValueFrom`
  o `| async`.
- ❌ Archivos FE en inglés — el repo es **español**.
- ❌ Crear componentes/pipes/directives sin chequear si ya existen (usar
  `codegraph_explore`).
- ❌ Cambiar `CONTEXT_PATH` sin tocar `compose`, `nginx.conf` y `apiBaseUrl`
  a la vez.
- ❌ Tocar `docker-compose.prod.yml` a mano para "depurar": hacer override nuevo.
- ❌ Asumir que un valor de `application.yml` es el real: verificar `.env.dev`
  / `.env.prod` / `docker compose exec backend env`.

---

## 10. Glosario rápido

| Término | Significado |
|---|---|
| Lote / batch | Cada `Resultado` (`telefono+dni`, N archivos) |
| Token público | 24 chars en `RESULTADO.TOKEN_PUBLICO` (visor `/r/...`) |
| Código corto | 7 chars en `ENLACE_CORTO.CODIGO_CORTO` (SMS `/s/...`) |
| Visor | Thymeleaf `/r/{token}` que lista archivos y marca `first_viewed_at` |
| Acortador | 302 `/s/{code}` → `/r/{token}` que cuenta `click_count` |
| Pendiente | `PACIENTE.PENDIENTE_RESULTADOS=true` (con admisión, sin resultados aún) |
| Nombre canónico | `"APELLIDO_P APELLIDO_M, NOMBRES"` (ver engram) |
| Credencial | DNI=usuario + password aleatorio + QR al portal paciente |

---

## 11. Pedir contexto a CodeGraph — ejemplos

```bash
codegraph explore "ResultadoService uploadResultados flow completo"
codegraph explore "ValidadorPdf ResultadoService validarNombre UMBRAL_COINCIDENCIA"
codegraph explore "SmsService enqueueResultLink Textbee frequency-ms cola"
codegraph explore "ShortLinkService callers blast radius"
codegraph explore "FiltroAutenticacionJwt ProveedorJwt UsuarioAutenticado SecurityConfig"
codegraph explore "ViewerController serve inline StorageService FileSystemResource"
codegraph explore "roleGuard authGuard auth.service signals JWT refresh"
```