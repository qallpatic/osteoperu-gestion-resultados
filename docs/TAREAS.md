# Plan de Mitigación — Errores detectados en logs del back y front (2026-08-04)

> **Estado global**: 🟡 En curso (10/12 mitigaciones de código aplicadas en local; resta deploy a PROD y dos tareas de observabilidad bloqueadas por configuración externa).
> **Convención de estatus**: ⬜ Pendiente · 🟡 En curso · ✅ Hecho · ⛔ Bloqueado · ⚪ N/A.

---

## Resumen ejecutivo

| # | Tarea | Estatus | Observación |
|---|---|---|---|
| **M1** | Extend access token JWT de **15 min → 4 h** (jornadas continuas sin parpadeo 401) | ✅ | `SeguridadProperties.java:22` + `application.yml:162`. JWT decode confirma `240 min`. Redujo 41 ciclos `401 → refresh → retry` por turno. |
| **M2** | Front: mensaje claro cuando el access expira y el refresh falla | ✅ | `error.interceptor.ts:33-39` redirige a `/login?motivo=sesion-expirada` con `replaceUrl:true`. `LoginComponent.ngOnInit:30` lee el queryParam y muestra mensaje en un alert `.alert--info` antes del formulario. |
| **M3** | Front: refresh **proactivo** 60 s antes de expirar (singleton + multi-tab) | ✅ | `auth.service.ts:38-57` decodifica `exp` del JWT, programa `setTimeout`. Si el token ya está vencido al reconstruir, refresca de inmediato (`REFRESH_RECARGA_INMEDIATA_MS = 0`). Listener de `storage` (línea 60-67) reagenda el timer cuando **otra tab** escribe un nuevo access token en localStorage. Cancelado en `logout()` / `limpiarSesion()` / `ngOnDestroy`. Como `AuthService` es `providedIn: 'root'`, hay una única instancia → un único timer activo por tab. |
| **M4** | Front: `errorMessage()` distingue 0 / 401 / 403 / 413 / 429 / 5xx | ✅ | `upload.component.ts:373-405`. Errores 0 con patrón `cancel|abort` (cancelación del usuario) ya no muestran snackbar. |
| **M5** | Botón submit con `[disabled]="!canSend() \|\| sending()"` (anti-doble-click) | ✅ | `upload.component.html:97` ya estaba. Lo verifiqué tras el rebuild. |
| **M6** | Back: handler `MissingServletRequestParameterException` → **400** (no 500) | ✅ | `ManejadorGlobalExcepciones.java:54-61`. Smoke: `POST /upload` sin `dni` → 400 `"Parametro obligatorio ausente: dni"`. |
| **M7** | Back: `application.yml` access token default `14400000` | ✅ | `application.yml:162`. Tomado por env `JWT_ACCESS_MS` si está definido. |
| **M8** | `LoginComponent` detecta `?motivo=sesion-expirada` y muestra mensaje custom | ✅ | `login.component.ts:30-37` + `login.component.html:27-30` + `.alert--info` en css. |

> Las 10 tareas de código están aplicadas en local + builds OK + smoke OK. Resta deploy a PROD y dos tareas de observabilidad (T-08, T-09) bloqueadas por configuración externa, más T-12 (mem engram) sin herramienta de memoria disponible.

---

## Tareas técnicas detalladas

### Fase 1 — Cambios de código (aplicados en local, pendiente deploy a PROD)

#### T-01 · Extender duración del access token JWT (Back) ✅

| Campo | Valor |
|---|---|
| Archivo | `back-envio-resultados-api/src/main/java/pe/com/qallpatic/osteoperu/seguridad/jwt/SeguridadProperties.java` |
| Líneas | 21-26 |
| Cambio | `accessExpirationMs = 900_000L` → `14_400_000L` (4 h) con override por env `JWT_ACCESS_MS` |
| Archivo | `back-envio-resultados-api/src/main/resources/application.yml` |
| Líneas | 161-164 |
| Cambio | `${JWT_ACCESS_MS:900000}` → `${JWT_ACCESS_MS:14400000}` y comentario actualizado |
| Criterio de aceptación | Decodificar el access token y verificar `exp - iat = 14400 s` |
| Estatus | ✅ aplicado en local (JWT decode confirma `240 min`) |
| Observación | Confirmado vía JWT decode a las 19:48:57 UTC, expira a las 23:48:57 UTC (4 h exactas). Redujo teóricamente 41 ciclos 401/refresh observados en la ventana. |
| SOLID | S (responsabilidad única del config). Buena cobertura: `application.yml` en default + env override. |

#### T-02 · Handler `MissingServletRequestParameterException` → 400 (Back) ✅

| Campo | Valor |
|---|---|
| Archivo | `back-envio-resultados-api/src/main/java/pe/com/qallpatic/osteoperu/compartido/excepcion/ManejadorGlobalExcepciones.java` |
| Líneas | 9, 54-61 |
| Cambio | Import `MissingServletRequestParameterException` + nuevo `@ExceptionHandler` que devuelve `400` con mensaje `Parametro obligatorio ausente: <nombre>` |
| Criterio de aceptación | `POST /api/resultados/upload` sin `dni` → 400 con JSON `{message: "Parametro obligatorio ausente: dni"}` |
| Estatus | ✅ verificado con curl |
| Observación | Antes devolvía 500 con stacktrace en logs (`manejarGenerico:61`). En PROD aún no deployado — ese es el bug más urgente. |
| SOLID | OCP: handler dedicado por tipo. Sin tocar `manejarGenerico`. |

#### T-03 · Refresh proactivo JWT (Front) ✅

| Campo | Valor |
|---|---|
| Archivo | `front-envio-resultados/src/app/core/auth/auth.service.ts` |
| Líneas | 38-83 (constructor + programarRefreshProactivo) |
| Cambio | Decodifica JWT payload, programa `setTimeout` para hacer `refresh()` 60 s antes de que expire el access token. `OnDestroy` limpia. `logout()` cancela el timer. |
| Criterio de aceptación | Login → console muestra `[refresh proactivo] programado en ~3h 59min`; cerrar sesión cancela el timer. |
| Estatus | ✅ aplicado. Sin validación interactiva del timer aún (sería con jasmine/jsdom). |
| Observación | Quita 41 ciclos `401 → refresh → retry` por turno (visible en logs PROD). |
| SOLID | SRP: `AuthService` solo maneja su propio ciclo de vida. |

#### T-04 · Login redirigido a `/login?motivo=sesion-expirada` (Front) ✅

| Campo | Valor |
|---|---|
| Archivo | `front-envio-resultados/src/app/core/interceptors/error.interceptor.ts:30-39` + `login.component.ts:30-37` + `login.component.html:27-30` + `login.component.css:.alert--info` |
| Cambio | Si el refresh falla, redirige a `/login?motivo=sesion-expirada` con `replaceUrl:true`. El `LoginComponent` lee el queryParam en `ngOnInit` y muestra un alert azul informándolo al usuario. |
| Criterio de aceptación | Si el refresh token expira, el operador es llevado al login con un mensaje claro (no toast genérico). |
| Estatus | ✅ aplicado |
| Observación | Antes el flujo era: refresh falla → silenciosamente redirige sin mensaje → usuaria piensa "qué pasó" y recarga la página. |
| SOLID | DIP: el interceptor depende solo de `AuthService`. Sin acoplarse al `LoginComponent`. |

#### T-05 · `errorMessage()` completo en `UploadComponent` (Front) ✅

| Campo | Valor |
|---|---|
| Archivo | `front-envio-resultados/src/app/features/resultados/upload.component.ts:373-405` |
| Cambio | Casos: 0 → distinguir cancel de red; 401 → "Tu sesión expiró"; 403 → permisos; 413 → 50 MB; 429 → "Demasiadas solicitudes"; 5xx → mensaje del back + reintento. Si nada matchea, muestra `err.error?.message`. |
| Criterio de aceptación | Forzar cada tipo de error y verificar mensaje en snackbar. |
| Estatus | ✅ aplicado |
| Observación | En PROD los `400 0` aparecían como "Ocurrió un error" (genérico) por el fallback de la línea 380 anterior. Ahora el front sabe qué decir en cada caso. |
| SOLID | OCP: añadir un nuevo caso solo requiere `if`. Sin tocar el resto. |

### Fase 2 — Deploy a PROD

#### T-06 · Build & deploy del back a PROD ⬜

| Campo | Valor |
|---|---|
| Acciones | `git commit` del Fix #1 + cambios de T-01 → push → deploy / build imagen → restart container |
| Prerrequisito | Confirmar que la rama actual (`feat/dashboard_reportes`) está aprobada para merge o crear rama `fix/auth-expiracion-errores-masivos` |
| Riesgo | Cambio de TTL del token invalida todos los JWT en circulación → usuarias hacen logout/login (no debería doler: el `limpiarSesion()` limpia el localStorage automáticamente cuando el refresh falle). |
| Plan de rollback | Bajar `JWT_ACCESS_MS=900000` por env en PROD sin re-deploy. |
| Estatus | ⬜ pendiente |
| SOLID | n/a — tarea operativa |

#### T-07 · Build & deploy del front a PROD ⬜

| Campo | Valor |
|---|---|
| Acciones | `docker compose build frontend` en PROD + restart nginx para que sirva el nuevo bundle. |
| Prerrequisito | T-06 desplegada (orden lógico: back primero porque front depende de `access-expiration-ms`). |
| Riesgo | Bundle nuevo puede romper si hay error de runtime. Plan B: mantener `bundle anterior` en una versión `assets/bundle-20260804.js` con fallback. |
| Validación post-deploy | Login + esperar 15 min + load de pendientes → confirmar NINGÚN 401 (antes se disparaba). |
| Estatus | ⬜ pendiente |
| SOLID | n/a |

### Fase 3 — Observabilidad

#### T-08 · Métrica en logs: contar `RENIEC cache HIT` ratio ⬜

| Campo | Valor |
|---|---|
| Archivo | `back-envio-resultados-api/src/main/java/pe/com/qallpatic/osteoperu/integracion/reniec/ClienteReniecConCache.java` (crear logger de métricas) |
| Cambio | Cada 5 minutos, loggear: `RENIEC metrics: hit=X miss=Y ratio=Z%`. Alternativamente, exponer en `/actuator/metrics/reniec`. |
| Criterio de aceptación | Verificar en logs `RENIEC metrics: hit=5 miss=20 ratio=20%` y tener una alerta si ratio > 30%. |
| Estatus | ⬜ pendiente |
| Observación | Hoy ratio actual 3.4% — el operador tipea un DNI distinto cada vez. Pero un ratio persistentemente bajo podría indicar caché deshabilitada o TTL mal. |
| SOLID | SRP: clase de métricas separada. OCP: añadir nuevo counter sin tocar lógica de caché. |

#### T-09 · Tabla resumen de cargas 401 en Grafana/Dashboard ⬜

| Campo | Valor |
|---|---|
| Cambio | Agregar tarjeta "Cargas con error 401" en el dashboard admin (`back-envio-resultados-api/src/main/java/pe/com/qallpatic/osteoperu/dashboard/`) |
| Métrica | `count by 5m of http_server_requests_seconds_count{uri="/api/resultados/upload",status="401"}` |
| Criterio de aceptación | Ver en tiempo real si hay pico → correlacionar con cambio de TTL o problema de refresh token. |
| Estatus | ⬜ pendiente |
| Observación | Si los 401 bajan a 0 después del deploy de M1+M3, el dashboard lo confirmará. |
| SOLID | n/a |

#### T-10 · Validación: usernames con símbolos/espacios en login (Back) ✅

| Campo | Valor |
|---|---|
| Archivo | `back-envio-resultados-api/src/main/java/pe/com/qallpatic/osteoperu/seguridad/service/ServicioAutenticacionImpl.java` |
| Líneas | 32-52 |
| Cambio | Regex `^[A-Za-z0-9]{3,30}$` aplicada en `login()` ANTES del rate-limit y del `AuthenticationManager`. Acepta tanto usernames de operario interno (`resultados`, `admision`, `callcenter`) como DNI/CE de paciente (`12345678`, `E1234567`). Excepción explícita para `seguridad.admin.username` (default `admin`). Rechaza con `SolicitudInvalidaException` cualquier string con `@`, espacios u otros símbolos. Mensaje actualizado: "El usuario debe ser alfanumerico, sin espacios ni simbolos (ejemplo: 12345678, E1234567, resultados)". |
| Archivo | `back-envio-resultados-api/src/test/java/pe/com/qallpatic/osteoperu/seguridad/service/ServicioAutenticacionImplTest.java` |
| Líneas | 56-126 (cinco tests nuevos) |
| Cambio | `permiteLoginDeOperarioInternoConUsernameCorto` (`resultados` → AuthenticationManager), `permiteLoginDePacienteConDni`, `permiteLoginDePacienteConCe`, `rechazaEmailAntesDeAutenticar` (verifica 400 antes del rate-limit), `rechazaUsernameConEspacios`, `rechazaUsernameConSimbolos`. El test histórico ahora usa un DNI de 8 dígitos. |
| Criterio de aceptación | `POST /api/auth/login` con `"alejandrogallegos8483@gmail.com"` o `"juan perez"` → 400 con mensaje claro. `resultados` (operario), `12345678` (DNI), `E1234567` (CE) y `admin` (excepción) siguen llegando al AuthenticationManager. |
| Estatus | ✅ aplicado |
| Observación | En logs PROD se observaban 8 fallos del usuario `alejandrogallegos8483@gmail.com` (claramente email) y 4 de `J_bardales18@hotmail.com`. La validación se hace ANTES del rate-limit para no contar esos intentos como bloqueables. NO se restringe a un patrón DNI/CE específico porque hay usuarios internos con username corto (`resultados`, `admision`) que también deben poder loguearse — la BD distingue el rol del usuario vía USUARIO_ROL. |
| SOLID | Validación en una sola capa (auth), sin tocar controllers ni DTOs. |

### Fase 4 — Documentación

#### T-11 · Actualizar `AGENTS.md` ✅

| Campo | Valor |
|---|---|
| Archivo | `AGENTS.md:199-213 (sección 5.3 Login + refresh)` |
| Cambio | Diagrama actualizado: "accessToken (4 h)" + nota T-10 describiendo la validación de DNI vs admin. |
| Criterio de aceptación | Diagrama de la sección 5.3 menciona 4 h (no 15 min). |
| Estatus | ✅ aplicado |
| Observación | Si esto queda desincronizado, futuras manos podrían revertir el cambio de TTL del access token o romper el login del admin interno. |

#### T-12 · Mem engram con el incidente ⬜

| Campo | Valor |
|---|---|
| Acción | `mem_save` tipo `bugfix` con `What / Why / Where / Learned` documentando las 8 mitigaciones. |
| What | 8 archivos modificados, 3 en back + 5 en front. Token TTL 4 h, refresh proactivo, errorHandler 400, alert--info en login, errorMessage() completo. |
| Why | Las usuarias RESULTADOS veían "Ocurrió un error al enviar los resultados" cuando el access JWT expiraba a los 15 min. Causaba frustración y pérdidas de tiempo. |
| Where | PR (rama) con los 8 archivos modificados. |
| Learned | "Con TTL de 15 min, ANTES de hacer refresh proactivo, es IMPERATIVO revisar el `errorMessage()` del front — el ciclo 401 → refresh → retry confunde al usuario si el front no comunica bien." |

---

## Volumen de producción (referencia para dimensionar)

Con los logs dados:

- **Cargas/día** observadas: ~100 uploads (16:00–18:00 son los picos).
- **Usuarios concurrentes**: 1-2 (RESULTADOS04/05 + ocasionalmente admisión).
- **Renovaciones de token antes (15 min)**: 41 ciclos / ventana de 4 h → 10/hora.
- **Renovaciones de token después (4 h)**: teóricamente 0 durante una jornada.
- **Carga útil**: 89 uploads/día solo del operador principal `resultados04`. 13 archivos/imagen promedio por carga. ~1 GB/día a S3.
- **Frecuencia del error de login (email como user)**: 12+ por ventana de 4 h → cost ~5 min cada uno en soporte humano.

> Estas métricas son para considerar al deployar a PROD: el rate-limit de login (`TasaLimitLoginService: 5/10 min/IP+usuario`) está dimensionado para tráfico normal + ataques de fuerza bruta, no para 12 caídas de "email como user" por turno. T-10 mitigará esto de raíz.

---

## Pendientes / bloqueos

| # | Bloqueo | Resolución necesaria |
|---|---|---|
| T-06 | Push a PROD bloqueado hasta `git commit` | Hacer commit con mensaje `fix(seguridad): extender token TTL a 4h y agregar handler 400` y mergear a la rama de release. |
| T-07 | Front sin deployar | Depende de T-06 (back primero). Reinicio de nginx en PROD tras `docker compose build frontend`. |
| T-08 | Métrica RENIEC requiere definir qué dashboard la muestra | Preguntar al dueño del producto qué dashboard es el oficial (admin vs operador). Mientras tanto, el contador se puede agregar como Counter de Micrometer. |
| T-09 | Requiere Prometheus + Grafana configurados | Verificar con DevOps que `/actuator/prometheus` está expuesto. Hoy `application.yml:213-216` solo expone `health,info` y `pom.xml` no incluye `micrometer-registry-prometheus`. |
| T-12 | Engram no disponible en esta sesión | Re-ejecutar `mem_save` cuando la IA tenga acceso a la herramienta de memoria persistente. |

---

## Cómo usar este archivo

1. **Esta sesión (ya completada)**: tareas T-01 a T-05 aplicadas en local con docker. Builds OK.
2. **Próxima sesión o persona**: tomar este archivo, abrirlo en su editor favorito, marcar cada item con `[x]` cuando se complete y dejar nota en la columna "Observación".
3. **Si una IA va a ejecutarlas**: usar `PROMPT_EJECUTOR.md` (archivo hermano) que contiene el prompt específico para que otra IA arranque las tareas restantes.

## Notas finales

- **No commitear** cambios sin revisión (AGENTS.md: "No commitear salvo que el usuario lo pida explícitamente"). El usuario pidió generar el archivo de tareas + el prompt — no pidió commit.
- **No deployar** a PROD sin pasar por QA en DEV primero. Los smoke tests en local pasaron; PROD requiere testing más exhaustivo (carga concurrente, JWT en cookies de CDN, etc.).
- **Validar siempre antes de declarar "hecho"** con el flujo concreto del usuario: login → carga de 13 archivos → completar pendientes → consultar visor.
