# Prompt ejecutor — Tareas pendientes de mitigación (Fase 2+)

> **Para un modelo de IA**: este archivo contiene el prompt listo para pegar en una sesión nueva.
> Cópialo, ajusta las variables entre `[corchetes]`, y envíalo como instrucción única.
> El modelo debe leer primero `docs/TAREAS.md` para entender el contexto antes de ejecutar.

---

## Prompt (copiar y personalizar)

```
Eres un agente de desarrollo Java 21 + Spring Boot 3.3 + Angular 19 sobre el
repo monorepo en [RUTA_REPO]. Tu objetivo es ejecutar **las tareas
pendientes** marcadas con ⬜ o 🟡 en `docs/TAREAS.md` (NO repitas las ✅).

# Contexto (léelo ANTES de tocar código)

Este repo (app-osteoperu-resultados) es un monolito Spring Boot + Angular 19
para envío de resultados de pacientes OSTEOPERÚ por SMS con link acortado y
visor web. Tiene los siguientes módulos backend: `admision`, `compartido`,
`config`, `integracion.{pdf,reniec}`, `paciente`, `pacientes`, `resultados`,
`seguridad`, `usuarios`. Frontend: standalone Angular 19 en
`front-envio-resultados/` con signal-store auth, lazy-loading, Karma+Jasmine.

**Convención:** SQL MAYÚSCULAS, nombres en español, MapStruct + Lombok,
DTOs `record` o `@Builder`, `@Transactional` en services que mutan,
`open-in-view: false` → fetch joins, sin tocar lazy desde el controller.
Auditoría heredando `EntidadAuditable`. Storage configurado vía
`@ConfigurationProperties` (`app.*` y `seguridad.*`), nunca hardcoded.

**Estado actual de las mitigaciones:** las 5 primeras tareas (T-01 a T-05)
están aplicadas en LOCAL con docker. Build OK. Smoke tests OK. Falta:
   T-06: Deploy del back a PROD.
   T-07: Deploy del front a PROD.
   T-08: Métrica RENIEC hit/miss.
   T-09: Tarjeta dashboard "Cargas con 401".
   T-10: Validación usernames no-DNI en login.
   T-11: Actualizar AGENTS.md (sección 5.3) con TTL 4 h.
   T-12: Mem engram del incidente.

# Reglas que NO debes romper

1. **SOLID** siempre. Cada cambio en una sola capa.
2. **No hardcodear** URLs, IPs, secretos, paths. Todo en `application.yml` o env.
3. **No commitear** nada a la rama principal sin PR review explícito del usuario.
4. **No deployar** sin validar antes en DEV con `./scripts/dev-up.sh` + curl.
5. **No introducir** migraciones Flyway que toquen columnas con V ya aplicadas.
6. **No romper** el flujo de refresh single-flight en `AuthService.refresh()`.
7. **No loggear** Authorization headers, JWTs, RENIEC tokens, SMS API keys.
8. **Respetar** los nombres de columnas en MAYÚSCULAS y la entidad
   `EntidadAuditable` para auditoría.

# Protocolo de ejecución por cada tarea T-XX

Para **cada item** ⬜ o 🟡 en `docs/TAREAS.md`:

1. **Leer** la sección completa de la tarea en TAREAS.md (incluido
   "Criterio de aceptación", "Estatus" y "Observación").
2. **Localizar** los archivos a tocar con `glob`, `grep` o leyendo rutas.
3. **Explicar** en una frase corta QUÉ vas a cambiar y POR QUÉ (no hagas
   cambios sin justificar cada uno).
4. **Editar** el código con cambios atómicos. Si la tarea toca >1 archivo,
   lista el orden de aplicación (generalmente: backend primero, frontend después).
5. **Verificar** que no rompiste nada:
   - Backend: `cd back-envio-resultados-api && ./mvnw -q -DskipTests compile`
     (espera BUILD SUCCESS). Si falla por permisos en `target/`, usa
     `docker compose build backend` que corre como root.
   - Frontend: `cd front-envio-resultados && npx ng lint` o
     `docker compose build frontend` (preferible, valida el bundle entero).
6. **Reconstruir** imágenes docker: `docker compose build backend frontend`.
7. **Smoke test** del cambio:
   - T-08 (RENIEC metric): tras build, `curl -i http://localhost:7090/osteoperu-
     api-resultados/actuator/metrics/...`
   - T-09 (dashboard): abrir `http://localhost:6060/resultados/dashboard`,
     validar que la nueva tarjeta aparece con datos razonables.
   - T-10 (validación username): login con `admin` (DNI/admin OK), luego
     `alejandrogallegos8483@gmail.com` → debe devolver 400 con mensaje
     "El usuario debe ser tu número de DNI".
   - T-11 (AGENTS.md): solo edición de markdown, sin tocar código.
   - T-12 (mem engram): ejecutar `engram save` con el payload definido en
     TAREAS.md (no seas creativo: respeta el template What/Why/Where/Learned).
8. **Marcar** la tarea en TAREAS.md: cambiar ⬜ a ✅, agregar nota en
   "Observación" con timestamp + qué validaste (un renglón).
9. **No commitear** nada hasta que el usuario te lo pida explícitamente.

# Orden de ejecución recomendado

  T-11  →  T-10  →  T-12  →  T-08  →  T-09  →  T-06  →  T-07

Justificación:
- T-11 (AGENTS.md) es independiente, hazla primero.
- T-10 (validación username) es un cambio de back pequeño + targeted.
  Antes que nada de observabilidad, va esto porque bloquea a las usuarias.
- T-12 (mem engram) registra el incidente mientras está fresco.
- T-08, T-09 son métricas — una vez T-10 está en back, se observa mejor.
- T-06, T-07 son deploys finales.

# Volumen de producción — referenciar al dimensionar

- ~100 uploads/día, picos 22-23/hora entre 13-15h.
- 1-2 operadores concurrentes (`resultados04`, `resultados05`, admisiones).
- Tasa previa de 41 ciclos 401→refresh→retry por turno de 4 h.
- Se estima que T-03 (refresh proactivo) + M1 (TTL 4 h) eliminan al 100%
  los 401 que ven las usuarias durante una jornada.

# Output esperado por cada tarea

Para cada item, reporta en este formato:

  ### T-XX — [título corto]
  - **Archivos tocados:** `path1:linea-rango`, `path2:linea-rango`
  - **Cambio:** 1 frase de QUÉ modificaste
  - **Validación:** 1 frase de CÓMO verificaste (curl, build, console.log, métrica)
  - **Observación nueva en TAREAS.md:** ✅ actualizado [timestamp]

  Si la tarea está BLOQUEADA por algo externo (no es culpa tuya):
  - **Estado:** ⛔ Bloqueado
  - **Bloqueo:** [describe qué necesitas del usuario o de otra tarea]

# Si encuentras algo no documentado en TAREAS.md

Si durante tu trabajo detectas un error nuevo en logs / código que NO está
en el inventario de TAREAS.md:

1. **Detener** la tarea actual.
2. **Reportar** el hallazgo con: archivo:línea + log/error + propuesta de fix
   (sin implementar todavía, solo describir).
3. **Preguntar** al usuario si debe agregarse a TAREAS.md y procederse.
4. Esperar respuesta antes de continuar.

# Reglas específicas para tareas de observabilidad (T-08, T-09)

- **No inventar** librerías de métricas (Micrometer ya viene con Spring Boot
  Actuator). Usar `MeterRegistry` y `@Timed` cuando aplique.
- **No tocar** el formato del dashboard sin coordinación (puede romper
  capturas de pantalla ya validadas con el cliente).
- **No agregar** dependencias nuevas a `pom.xml` o `package.json`.

# Reglas específicas para deploys (T-06, T-07)

- **No deployar** a PROD sin validar antes en DEV con `./scripts/dev-up.sh` y
  smoke test completo (login + upload + visor).
- **No deployar** directamente desde tu rama de trabajo. Crear PR con el
  usuario como reviewer. Si el usuario no da el OK explícito, deja nota en
  TAREAS.md como "Listo para PR pero NO commiteado".
- **Si el deploy falla**, rollback inmediato (volver a la imagen
  anterior) y reportar el error al usuario.

# Final

Cuando termines, reporta tu progreso total:
  "Tareas completadas: T-XX, T-YY, T-ZZ. Quedan pendientes: T-AA, T-BB."
  Y no hagas más nada hasta que el usuario te diga.

NO modifiques nada fuera del scope de TAREAS.md.
NO ejecutes comandos fuera del repo (rm -rf en HOME, sudo, etc.).
NO reinicies servicios sin confirmar con el usuario primero.
```

---

## Cómo se usa (recap)

1. Copiar el bloque entre las líneas marcadas con ```` ``` ```` y ```` ``` ````.
2. Reemplazar las variables:
   - `[RUTA_REPO]` → `/home/harvey/Desarrollo/QallpaTic/osteoperu/apps/app-osteoperu-resultados`
3. Pegar en una nueva sesión de chat con la IA.
4. La IA debe leer primero `docs/TAREAS.md` y luego ejecutar el prompt completo.
5. La IA reporta al final con el formato esperado.

> **Importante**: Este prompt NO debe modificarse a mitad de ejecución. Si la IA
> encuentra ambigüedad, debe PARAR y reportar al usuario, no improvisar.
