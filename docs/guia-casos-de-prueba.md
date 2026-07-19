# Guía de Casos de Prueba — OSTEOPERU Resultados

> Documento de soporte para QA y pruebas manuales. Recorrido breve por las funcionalidades clave, agrupadas por área y por rol. Para el detalle paso a paso de cada pantalla, consulte el **Manual de Usuario**.

---

## Cómo usar esta guía

- Cada escenario indica el **rol** necesario, los **pre-requisitos**, los **pasos** y el **resultado esperado**.
- Marque cada caso con un check (✓) al verificarlo. Si falla, anote el detalle en la columna de observaciones.
- Antes de comenzar, asegúrese de tener la aplicación levantada y un usuario de cada rol disponible.

### Datos de prueba sugeridos (entorno DEV)

| Concepto | Valor |
|---|---|
| URL del portal | `http://localhost:6060/resultados/` |
| URL del backend | `http://localhost:7090/osteoperu-api-resultados/` |
| Usuario administrador DEV | `admin` |
| Contraseña administrador DEV | `Admin@2026` |
| DNI peruano de prueba | 8 dígitos cualquiera (los reales disparan RENIEC) |
| Carnet de Extranjería de prueba | 9 a 12 dígitos |
| Teléfono de prueba | 9 dígitos, empieza con 9 |

---

## 1. Autenticación

### 1.1 Login exitoso (cualquier rol)

- **Rol:** cualquiera.
- **Pasos:** ingrese usuario y contraseña válidos y haga clic en **Ingresar**.
- **Esperado:** redirige a la página de inicio (Dashboard) con las tarjetas de los módulos según el rol.

### 1.2 Login con credenciales inválidas

- **Rol:** cualquiera.
- **Pasos:** ingrese un usuario o contraseña incorrectos.
- **Esperado:** muestra el mensaje "Usuario o contraseña incorrectos" y no avanza.

### 1.3 Login con sesión expirada

- **Rol:** cualquiera.
- **Pasos:** espere a que el access token expire y use la aplicación normalmente.
- **Esperado:** el sistema renueva el token silenciosamente; si no puede, lo redirige al login.

### 1.4 Recuperar contraseña (paciente, teléfono registrado)

- **Rol:** PACIENTE.
- **Pasos:** en login haga clic en **¿Olvidaste tu contraseña?**, ingrese un teléfono registrado y haga clic en **Enviar SMS**.
- **Esperado:** muestra mensaje genérico de éxito y envía SMS con clave nueva al teléfono registrado.

### 1.5 Recuperar contraseña (teléfono no registrado)

- **Rol:** PACIENTE.
- **Pasos:** repita el flujo con un teléfono que no esté en el sistema.
- **Esperado:** muestra el mismo mensaje genérico (no revela que el teléfono no existe).

### 1.6 Cerrar sesión

- **Rol:** cualquiera.
- **Pasos:** haga clic en **Salir** arriba a la derecha o en el menú lateral.
- **Esperado:** vuelve a la pantalla de login; intentar entrar a una URL interna redirige a login.

---

## 2. Admisión — Gestión de usuarios

### 2.1 Buscar paciente por DNI

- **Rol:** ADMIN, ADMISION, RESULTADOS.
- **Pasos:** abra **Gestión de usuarios**, escriba el DNI y haga clic en **Buscar**.
- **Esperado:** la tabla muestra el paciente con su cantidad de resultados y estado de cuenta.

### 2.2 Crear cuenta nueva (paciente existente)

- **Rol:** ADMIN, ADMISION.
- **Pasos:** ubique un paciente con estado **Sin cuenta**, haga clic en **Generar**, complete nombre y teléfono (opcional), marque **Enviar credencial por SMS** si tiene teléfono, y haga clic en **Generar**.
- **Esperado:** aparece modal con usuario, clave y QR. Si marcó SMS, también confirma el envío.

### 2.3 Crear cuenta para paciente nuevo (sin resultados previos)

- **Rol:** ADMIN, ADMISION.
- **Pasos:** haga clic en **Nuevo usuario**, ingrese DNI (8 dígitos autocompleta nombre desde RENIEC), nombre, teléfono y haga clic en **Generar**.
- **Esperado:** aparece la credencial con usuario y clave nuevos.

### 2.4 Regenerar credencial de cuenta anulada

- **Rol:** ADMIN, ADMISION.
- **Pasos:** ubique un paciente con estado **Anulado** y haga clic en **Regenerar**.
- **Esperado:** se crea una nueva clave. La anterior deja de funcionar.

### 2.5 Anular cuenta activa

- **Rol:** ADMIN, ADMISION.
- **Pasos:** ubique un paciente con estado **Activo**, haga clic en **Anular** y confirme en el diálogo.
- **Esperado:** el estado pasa a **Anulado**. El paciente no puede iniciar sesión.

### 2.6 Imprimir credencial (ticket 80 mm)

- **Rol:** ADMIN, ADMISION.
- **Pasos:** después de generar/regenerar, haga clic en **Imprimir**.
- **Esperado:** se abre una ventana lista para imprimir en formato ticket térmico.

---

## 3. Resultados — Carga y consulta

### 3.1 Cargar resultado con DNI peruano (autocompletado RENIEC)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** en **Cargar** ingrese un DNI de 8 dígitos válido. Al completar, el sistema consulta RENIEC.
- **Esperado:** el campo **Nombre completo** se autocompleta si RENIEC responde.

### 3.2 Cargar resultado con Carnet de Extranjería

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** ingrese un CE de 9 a 12 dígitos y complete el nombre manualmente.
- **Esperado:** la carga procede. No se consulta RENIEC para CE.

### 3.3 Cargar con PDF cuyo nombre coincide con RENIEC

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** ingrese DNI con PDF adjunto cuyo nombre detectado coincide con RENIEC.
- **Esperado:** la carga se envía directamente sin pedir confirmación.

### 3.4 Cargar con PDF cuyo nombre NO coincide con RENIEC

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** ingrese DNI con un PDF cuyo nombre detectado difiere de RENIEC.
- **Esperado:** aparece diálogo **Verifica el nombre del paciente** con ambos nombres. El operador debe confirmar o cancelar.

### 3.5 Cargar sin archivos adjuntos (solo mensaje)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** complete DNI, teléfono, nombre y mensaje. No adjunte archivos. Haga clic en **Enviar resultados**.
- **Esperado:** la carga se registra correctamente. El paciente recibe SMS con el mensaje y la dirección del portal.

### 3.6 Cargar sin teléfono

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** complete DNI, nombre, mensaje y archivos. Deje el teléfono vacío.
- **Esperado:** la carga se registra con estado de SMS **Sin teléfono**. El paciente no recibe SMS.

### 3.7 Rechazo de archivo > 50 MB

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** intente arrastrar un archivo mayor a 50 MB.
- **Esperado:** el sistema rechaza el archivo y muestra el motivo.

### 3.8 Rechazo de formato no permitido

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** intente subir un archivo con extensión no permitida (por ejemplo `.exe` o `.docx`).
- **Esperado:** el sistema rechaza el archivo y muestra el motivo.

### 3.9 Consultar resultados con filtros

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** en **Consultar**, filtre por DNI, nombre o rango de fechas y haga clic en **Buscar**.
- **Esperado:** la tabla muestra solo los resultados coincidentes.

### 3.10 Ver detalle de un resultado

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** haga clic en el ícono de **ojo** de una fila.
- **Esperado:** se abre un modal con teléfono, nombre, lista de archivos y botón **Abrir visor**.

### 3.11 Editar archivos de un resultado (desactivar)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** haga clic en el ícono de **lápiz**. Apague el interruptor de un archivo, indique un motivo y guarde.
- **Esperado:** el archivo queda marcado **Desactivado** y deja de verse en el visor del paciente.

### 3.12 Editar archivos de un resultado (reactivar)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** en el mismo modal de edición, encienda el interruptor de un archivo desactivado.
- **Esperado:** el archivo vuelve a aparecer en el visor del paciente.

### 3.13 Editar archivos de un resultado (agregar nuevo)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** arrastre un archivo nuevo al recuadro **Arrastra archivos nuevos** y guarde.
- **Esperado:** el archivo nuevo aparece en el visor.

### 3.14 Reenviar SMS al paciente

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** en el modal de detalle o edición, marque la casilla **Reenviar SMS** y guarde.
- **Esperado:** el SMS vuelve a encolarse para el paciente.

---

## 4. Resultados — Pendientes y bitácora

### 4.1 Campana de pendientes (notificaciones)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** abra la campana de la barra superior.
- **Esperado:** muestra la lista de pacientes pendientes con DNI y nombre. La lista se actualiza automáticamente cada 60 segundos.

### 4.2 Abrir carga desde la campana

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** haga clic en un paciente de la campana.
- **Esperado:** se abre el formulario de carga con DNI, nombre y teléfono ya completados.

### 4.3 Vista de pendientes completa

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** abra **Resultados > Pendientes**.
- **Esperado:** tabla con DNI, paciente, teléfono y fecha. El botón **Subir resultados** abre el formulario precargado.

### 4.4 Revisar logs de carga

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** abra **Resultados > Logs de carga**, filtre por DNI o fecha.
- **Esperado:** tabla con cada intento: estado, usuario que cargó, nombre de RENIEC, nombre del PDF, y si coinciden.

---

## 5. Administración de usuarios internos

### 5.1 Crear usuario interno

- **Rol:** ADMIN.
- **Pasos:** en **Administración**, haga clic en **Nuevo usuario**, complete los datos y asigne uno o más roles. Guarde.
- **Esperado:** el usuario aparece en la lista con estado activo.

### 5.2 Editar datos de un usuario

- **Rol:** ADMIN.
- **Pasos:** haga clic en el ícono de **lápiz** sobre una fila. Modifique nombre, correo o DNI. Guarde.
- **Esperado:** los cambios se reflejan en la tabla.

### 5.3 Cambiar roles de un usuario

- **Rol:** ADMIN.
- **Pasos:** haga clic en el ícono de **roles**. Active o desactive los roles deseados. Guarde.
- **Esperado:** la próxima vez que el usuario ingrese verá los módulos correspondientes a sus nuevos roles.

### 5.4 Cambiar contraseña de un usuario

- **Rol:** ADMIN.
- **Pasos:** haga clic en el ícono de **llave** sobre una fila. Ingrese la nueva contraseña dos veces y guarde.
- **Esperado:** el usuario puede iniciar sesión con la nueva clave.

### 5.5 Activar o desactivar un usuario

- **Rol:** ADMIN.
- **Pasos:** use el interruptor de la columna **Estado** sobre una fila.
- **Esperado:** el usuario cambia su estado. Si está desactivado, no puede iniciar sesión.

### 5.6 Filtrar usuarios por rol o estado

- **Rol:** ADMIN.
- **Pasos:** aplique los filtros de rol o estado y haga clic en **Buscar**.
- **Esperado:** la tabla muestra solo los usuarios que coinciden.

---

## 6. Portal del paciente

### 6.1 Login con credencial

- **Rol:** PACIENTE.
- **Pasos:** ingrese con el usuario y la clave recibida en la credencial.
- **Esperado:** redirige a **Mis resultados**.

### 6.2 Ver el listado de mis resultados

- **Rol:** PACIENTE.
- **Pasos:** una vez autenticado, revise la pantalla principal.
- **Esperado:** cada resultado aparece como tarjeta con la fecha y la cantidad de archivos.

### 6.3 Filtrar mis resultados por fecha

- **Rol:** PACIENTE.
- **Pasos:** use los filtros **desde / hasta** y haga clic en **Buscar**.
- **Esperado:** la lista se limita al rango seleccionado.

### 6.4 Abrir un resultado

- **Rol:** PACIENTE.
- **Pasos:** haga clic en **Ver archivos** sobre la tarjeta de un estudio.
- **Esperado:** se abre un modal con el mensaje del laboratorio (si lo hay) y la lista de archivos.

### 6.5 Ver un archivo en línea

- **Rol:** PACIENTE.
- **Pasos:** dentro del modal, haga clic en **Ver** sobre un archivo.
- **Esperado:** el archivo se abre en una pestaña nueva del navegador.

### 6.6 Descargar un archivo

- **Rol:** PACIENTE.
- **Pasos:** haga clic en **Descargar** sobre un archivo.
- **Esperado:** el navegador descarga el archivo con su nombre original.

### 6.7 Verificar que un archivo desactivado no aparece

- **Rol:** PACIENTE.
- **Pasos:** abra un resultado donde el laboratorio desactivó un archivo.
- **Esperado:** el archivo desactivado no se muestra en la lista del paciente.

### 6.8 Cambiar contraseña (opcional)

- **Rol:** PACIENTE.
- **Pasos:** una vez autenticado, acceda al perfil o configuración (si está disponible) y cambie la clave.
- **Esperado:** la nueva clave reemplaza a la entregada en la credencial.

---

## 7. Casos borde y controles de seguridad

### 7.1 Acceso denegado por rol

- **Rol:** RESULTADOS.
- **Pasos:** intente ingresar a una URL restringida (por ejemplo, **Administración**).
- **Esperado:** el sistema no muestra la opción en el menú y, si se accede por URL, devuelve pantalla de "sin permisos" o redirige al inicio.

### 7.2 Archivos de formatos prohibidos

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** intente subir un archivo `.zip` o `.docx`.
- **Esperado:** el sistema rechaza el archivo y muestra el motivo del rechazo.

### 7.3 Documento de identidad inválido

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** en **Cargar** ingrese un DNI con menos de 8 dígitos o más de 12.
- **Esperado:** el botón **Enviar resultados** permanece deshabilitado hasta que el documento sea válido.

### 7.4 Teléfono con formato incorrecto

- **Rol:** ADMIN, RESULTADOS, ADMISION.
- **Pasos:** ingrese un teléfono con menos o más de 9 dígitos.
- **Esperado:** el formulario no permite enviar la carga / generar la credencial hasta corregir el teléfono.

### 7.5 Mensaje obligatorio (carga de resultados)

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** intente enviar una carga con el campo mensaje vacío.
- **Esperado:** el botón **Enviar resultados** permanece deshabilitado.

### 7.6 Cabecera sin archivos activos

- **Rol:** ADMIN, RESULTADOS.
- **Pasos:** intente desactivar el último archivo activo de un resultado.
- **Esperado:** el sistema impide la operación y muestra el mensaje "La cabecera debe quedar con al menos un archivo activo".

### 7.7 Desactivar todos los roles de un usuario interno

- **Rol:** ADMIN.
- **Pasos:** quite todos los roles a un usuario desde **Administración**.
- **Esperado:** el usuario, en su próximo ingreso, queda sin módulos disponibles.

---

## 8. Checklist general de una corrida de pruebas

Use esta lista como verificación rápida al final de cada ciclo de pruebas:

- [ ] Login con cada rol disponible (ADMIN, RESULTADOS, ADMISION, PACIENTE).
- [ ] Recuperar contraseña de un paciente con teléfono registrado.
- [ ] Recuperar contraseña con teléfono no registrado (mensaje genérico).
- [ ] Cerrar sesión desde la barra superior y desde el menú lateral.
- [ ] Crear cuenta para paciente nuevo y para paciente existente.
- [ ] Enviar credencial por SMS y confirmar recepción.
- [ ] Anular cuenta activa y regenerar la clave de una cuenta anulada.
- [ ] Cargar resultado con DNI peruano (verificar autocompletado RENIEC).
- [ ] Cargar resultado con CE (sin RENIEC).
- [ ] Cargar PDF con nombre coincidente y no coincidente con RENIEC.
- [ ] Cargar sin archivos (solo mensaje).
- [ ] Cargar sin teléfono.
- [ ] Rechazo de archivo > 50 MB y de formato no permitido.
- [ ] Consultar resultados con los tres tipos de filtros (DNI, nombre, fechas).
- [ ] Editar archivos: desactivar, reactivar y agregar nuevos.
- [ ] Reenviar SMS desde la edición.
- [ ] Verificar campana de pendientes y carga desde ella.
- [ ] Revisar logs de carga y la columna **Coincide**.
- [ ] Crear, editar, asignar roles, cambiar contraseña y activar/desactivar un usuario interno.
- [ ] Paciente ve sus resultados, abre un archivo y lo descarga.
- [ ] Archivo desactivado por el operador no aparece para el paciente.
- [ ] Acceso denegado al intentar ver módulos fuera del rol.

---

> **Recordatorio:** este documento es de soporte para pruebas manuales. Para el detalle paso a paso de cada pantalla, consulte el **Manual de Usuario** (`docs/manual-usuario.md`).
