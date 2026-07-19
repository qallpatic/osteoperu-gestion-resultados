# Manual de Usuario — OSTEOPERU Resultados

> Plataforma web para el envío de resultados médicos a pacientes por SMS, invitándolos a ingresar al portal para ver y descargar sus estudios.

---

## Tabla de contenidos

1. [Bienvenida](#1-bienvenida)
2. [Antes de empezar](#2-antes-de-empezar)
3. [Acceso a la plataforma](#3-acceso-a-la-plataforma)
   - 3.1 [Iniciar sesión](#31-iniciar-sesion)
   - 3.2 [Recuperar contraseña (pacientes)](#32-recuperar-contrasena-pacientes)
   - 3.3 [Cerrar sesión](#33-cerrar-sesion)
   - 3.4 [No puedo ingresar](#34-no-puedo-ingresar)
4. [Conocer la interfaz](#4-conocer-la-interfaz)
   - 4.1 [Menú lateral](#41-menu-lateral)
   - 4.2 [Barra superior](#42-barra-superior)
   - 4.3 [Página de inicio](#43-pagina-de-inicio)
   - 4.4 [Elementos comunes](#44-elementos-comunes)
5. [Módulo Resultados (ADMIN, RESULTADOS)](#5-modulo-resultados)
   - 5.1 [Cargar resultados a un paciente](#51-cargar-resultados-a-un-paciente)
   - 5.2 [Consultar resultados cargados](#52-consultar-resultados-cargados)
   - 5.3 [Editar o corregir un resultado](#53-editar-o-corregir-un-resultado)
   - 5.4 [Pacientes pendientes](#54-pacientes-pendientes)
   - 5.5 [Bitácora de carga (logs)](#55-bitacora-de-carga-logs)
6. [Módulo Gestión de usuarios (ADMIN, RESULTADOS, ADMISION)](#6-modulo-gestion-de-usuarios)
   - 6.1 [Buscar un paciente](#61-buscar-un-paciente)
   - 6.2 [Crear una cuenta nueva](#62-crear-una-cuenta-nueva)
   - 6.3 [Reactivar una cuenta anulada](#63-reactivar-una-cuenta-anulada)
   - 6.4 [Anular el acceso de un paciente](#64-anular-el-acceso-de-un-paciente)
   - 6.5 [Imprimir la credencial](#65-imprimir-la-credencial)
7. [Módulo Administración (ADMIN)](#7-modulo-administracion)
   - 7.1 [Listar y buscar usuarios internos](#71-listar-y-buscar-usuarios-internos)
   - 7.2 [Crear un usuario interno](#72-crear-un-usuario-interno)
   - 7.3 [Editar datos de un usuario](#73-editar-datos-de-un-usuario)
   - 7.4 [Asignar y cambiar roles](#74-asignar-y-cambiar-roles)
   - 7.5 [Cambiar contraseña](#75-cambiar-contrasena)
   - 7.6 [Activar o desactivar un usuario](#76-activar-o-desactivar-un-usuario)
8. [Portal del paciente (PACIENTE)](#8-portal-del-paciente)
   - 8.1 [Ver mis resultados](#81-ver-mis-resultados)
   - 8.2 [Abrir un resultado](#82-abrir-un-resultado)
   - 8.3 [Ver o descargar un archivo](#83-ver-o-descargar-un-archivo)
9. [Glosario](#9-glosario)
10. [Preguntas frecuentes y soporte](#10-preguntas-frecuentes-y-soporte)

---

## 1. Bienvenida

Bienvenido a **OSTEOPERU Resultados**, la plataforma que permite enviar los estudios médicos de un paciente a su celular por mensaje de texto (SMS), indicándole cómo ingresar al portal del paciente para revisarlos o descargarlos desde cualquier dispositivo.

Este manual explica cómo utilizar cada parte del sistema. Está pensado para el personal de la clínica o laboratorio (resultados, admisión, administración) y también para los pacientes que reciben sus estudios.

> **Aviso:** a lo largo del manual encontrará recuadros con tres tipos de notas:
> - **Aviso:** información importante que conviene recordar.
> - **Cuidado:** situaciones que pueden causar errores si no se tienen en cuenta.
> - **Tip:** recomendaciones que hacen más rápido el trabajo.

[pegar imagen de: logo de OSTEOPERU en la portada del manual]

---

## 2. Antes de empezar

### Requisitos

- Una computadora, tablet o celular con navegador moderno (Chrome, Edge, Firefox o Safari).
- Conexión a internet.
- Sus datos de acceso (usuario y contraseña). El personal los recibe del administrador; los pacientes los reciben en la credencial que se les entrega o por SMS.

### Roles de la plataforma

Cada usuario tiene uno o más roles que definen qué puede hacer dentro del sistema.

| Rol | Qué puede hacer |
|---|---|
| **ADMIN** | Acceso completo. Administra usuarios internos y realiza todas las tareas de los demás roles. |
| **RESULTADOS** | Carga, consulta y corrige resultados de pacientes. |
| **ADMISION** | Crea y anula cuentas de acceso para pacientes. |
| **PACIENTE** | Ingresa al portal del paciente para ver y descargar sus propios resultados. |
| **CALLCENTER** | Próximamente. |

> **Aviso:** cuando una sección está dirigida a un rol específico, se indica al inicio del capítulo.

---

## 3. Acceso a la plataforma

### 3.1 Iniciar sesión

1. Abra la URL de OSTEOPERU Resultados en su navegador.
2. En la pantalla **Iniciar sesión**, escriba su **usuario** y su **contraseña**.
3. Si desea ver la contraseña mientras la escribe, haga clic en el ícono de ojo a la derecha del campo.
4. Haga clic en **Ingresar**.

Si las credenciales son correctas, ingresará al sistema y verá la página de inicio con los módulos a los que tiene acceso.

[pegar imagen de: pantalla de Iniciar sesión con los campos Usuario y Contraseña]

> **Tip:** si es paciente y no recuerda su contraseña, consulte la sección [3.2 Recuperar contraseña (pacientes)](#32-recuperar-contrasena-pacientes).

### 3.2 Recuperar contraseña (pacientes)

Si es paciente y no recuerda su clave, puede solicitar que se la envíen por SMS al teléfono registrado.

1. En la pantalla de inicio de sesión, haga clic en **¿Olvidaste tu contraseña?**.
2. Ingrese su número de celular (9 dígitos, empezando con 9).
3. Haga clic en **Enviar SMS**.
4. Si el número está registrado, recibirá un SMS con su nueva clave.
5. Use esa clave para iniciar sesión y, si lo desea, cámbiela después desde el portal.

[pegar imagen de: pantalla Recuperar contraseña con el campo Teléfono]

> **Aviso:** el sistema siempre muestra el mismo mensaje, aunque el número no esté registrado, para proteger la privacidad. Si no recibe el SMS dentro de unos minutos, comuníquese al teléfono de soporte que aparece en pantalla.
>
> **Cuidado:** por seguridad, después de varios intentos fallidos el sistema le solicitará esperar unos minutos antes de volver a intentar.

### 3.3 Cerrar sesión

1. En la barra superior derecha, haga clic en el botón **Salir** (ícono de puerta con flecha).
2. También puede cerrar sesión desde el menú lateral izquierdo, haciendo clic en el mismo ícono junto a su nombre.

Es importante cerrar sesión cuando termine, sobre todo si comparte la computadora.

### 3.4 No puedo ingresar

Revise lo siguiente antes de solicitar ayuda:

- Que la tecla **Bloq Mayús** no esté activada al escribir la contraseña.
- Que el usuario esté bien escrito (sin espacios al principio o al final).
- Que la sesión no haya expirado (en ese caso, el sistema lo lleva de vuelta al login automáticamente).

Si el problema persiste, contacte al administrador del sistema.

---

## 4. Conocer la interfaz

Una vez que ingresa, la plataforma tiene siempre la misma estructura: un **menú lateral** a la izquierda y una **barra superior** arriba. El contenido de cada módulo aparece en el centro.

[pegar imagen de: vista general de la plataforma con el menú lateral y la barra superior señalados]

### 4.1 Menú lateral

Es la columna fija de la izquierda. Contiene las opciones principales según su rol. Algunos puntos a tener en cuenta:

- Puede **colapsar** el menú con la flecha ubicada arriba a la derecha del menú para ganar espacio.
- Si una opción tiene un submenú (por ejemplo, **Resultados**), haga clic en su nombre para desplegar las pantallas que contiene.
- Las opciones que todavía no están disponibles aparecen con la etiqueta **Pronto** y no se pueden seleccionar.

[pegar imagen de: menú lateral desplegado mostrando el grupo Resultados con sus submenús]

### 4.2 Barra superior

Es la franja horizontal ubicada en la parte superior. Contiene:

- El **menú hamburguesa** (tres líneas) en dispositivos móviles para abrir y cerrar el menú lateral.
- El **saludo** con el primer nombre y el rol principal.
- La **campana de notificaciones** (solo para roles ADMIN y RESULTADOS): muestra cuántos pacientes tienen resultados pendientes de subir.
- El **avatar** con las iniciales y el botón **Salir**.

[pegar imagen de: barra superior con saludo, campana y avatar señalados]

#### Campana de notificaciones (ADMIN, RESULTADOS)

La campana muestra un número rojo con la cantidad de pacientes que ya fueron registrados en admisión pero todavía no tienen resultados cargados. La lista se actualiza automáticamente cada 60 segundos.

1. Haga clic en la campana para abrir la lista.
2. Si hay pendientes, cada uno muestra el nombre y el DNI del paciente.
3. Haga clic en un paciente de la lista para abrir directamente el formulario de carga con sus datos ya completados.

[pegar imagen de: campana abierta mostrando la lista de pendientes]

> **Tip:** la campana es la forma más rápida de retomar el trabajo del día: ábrala y empiece a cargar los pendientes uno por uno.

### 4.3 Página de inicio

Es la pantalla a la que llega después de iniciar sesión. Muestra:

- Un saludo personalizado.
- Sus roles (como etiquetas).
- Tarjetas con cada módulo al que tiene acceso. Haga clic en una tarjeta para entrar al módulo.

[pegar imagen de: página de inicio con tarjetas de acceso]

### 4.4 Elementos comunes

A lo largo de toda la plataforma encontrará los mismos elementos visuales. Conocerlos facilita el uso.

#### Tablas

Las listas (resultados, pacientes, usuarios, bitácora) se muestran en tablas con las siguientes características:

- **Cabecera fija al hacer scroll**: el encabezado de las columnas permanece visible aunque la lista sea larga.
- **Orden por columna**: haga clic en el título de una columna para ordenar de menor a mayor; haga clic otra vez para invertir el orden; un tercer clic quita el orden.
- **Acciones por fila**: a la derecha de cada fila aparecen íconos para ver, editar u otras acciones, según el módulo.

[pegar imagen de: tabla con cabecera fija y columnas ordenables señaladas]

#### Filtros

La mayoría de las pantallas tienen una barra de filtros arriba de la tabla. Los filtros más comunes son:

- Búsqueda por texto (DNI o nombre).
- Rango de fechas (desde / hasta).
- Botón **Buscar** para aplicar los filtros.

> **Cuidado:** los filtros no se aplican automáticamente; siempre hay que hacer clic en **Buscar** (o presionar **Enter** en el campo de texto).

#### Paginación

Debajo de cada tabla se muestra el indicador **Página X de Y** con flechas para ir a la página anterior o siguiente. También se muestra el rango de resultados que está viendo (por ejemplo, "11–20 de 145").

#### Alertas y mensajes

Después de cada acción importante, el sistema muestra mensajes emergentes:

- Verde: la operación salió bien.
- Rojo: ocurrió un error. Lea el mensaje para entender qué pasó.

---

## 5. Módulo Resultados

> **Roles:** ADMIN, RESULTADOS

Este módulo reúne todo lo relacionado con la carga, consulta y seguimiento de los resultados de los pacientes. Se accede desde el menú lateral, dentro del grupo **Resultados**.

### 5.1 Cargar resultados a un paciente

Esta es la tarea principal: enviar un aviso por SMS al paciente para que ingrese al portal a ver y descargar sus resultados.

1. En el menú lateral, abra **Resultados** y luego **Cargar**.
2. Complete el formulario:
   - **Doc. identidad (DNI/CE)**: ingrese 8 dígitos para DNI peruano o entre 9 y 12 para Carnet de Extranjería. Al completar 8 dígitos, el sistema intenta traer el nombre automáticamente desde RENIEC.
   - **Número de teléfono (opcional)**: ingrese el celular del paciente (9 dígitos, empieza con 9). Si lo deja vacío, no se enviará SMS.
   - **Nombre completo**: aparece autocompletado cuando RENIEC responde, pero puede corregirlo.
   - **Mensaje**: es **obligatorio**. Escriba el mensaje que recibirá el paciente avisándole que sus resultados ya están disponibles (máximo 500 caracteres).
   - **Archivos adjuntos**: arrastre uno o varios archivos al recuadro, o haga clic para seleccionarlos. Se aceptan **PDF, imágenes y videos**, hasta 50 MB por archivo.
3. Haga clic en **Enviar resultados**.

[pegar imagen de: formulario Subir resultados del paciente con todos los campos señalados]

> **Cuidado:** los archivos son opcionales, pero el mensaje siempre es obligatorio. Si desea compartir estudios pesados o complementar la entrega, puede indicar el enlace a un sistema externo dentro del mensaje.
>
> **Aviso:** el SMS que recibe el paciente **no contiene un enlace directo a los archivos**. Solo le avisa que ya tiene resultados disponibles y le indica la dirección del portal. Para ver sus estudios, el paciente debe iniciar sesión con su usuario y contraseña.

#### ¿Qué pasa si el nombre del PDF no coincide con RENIEC?

Si sube un PDF, el sistema compara el nombre detectado dentro del PDF con el de RENIEC. Si no coinciden, aparece un diálogo de confirmación:

1. Se muestra el **nombre según RENIEC** y el **nombre detectado en el PDF**.
2. Elija una opción:
   - **No, cancelar**: detiene la carga. Verifique el DNI y el PDF antes de intentar de nuevo.
   - **Sí, cargar**: confirma que el archivo es correcto y la carga continúa.

[pegar imagen de: diálogo Verifica el nombre del paciente con los nombres de RENIEC y PDF]

#### Resultado de la carga

Cuando la carga termina, el sistema muestra un mensaje verde confirmando que el resultado quedó registrado y que el SMS fue encolado (queda en la cola para enviarse al teléfono del paciente).

### 5.2 Consultar resultados cargados

Permite buscar y revisar todas las cargas realizadas.

1. En el menú lateral, abra **Resultados** y luego **Consultar**.
2. Filtre por **DNI/CE**, **nombre** o **rango de fechas** (desde / hasta).
3. Haga clic en **Buscar**.
4. La tabla muestra: DNI, paciente, teléfono, estado del SMS, cantidad de vistas y fecha de carga.

[pegar imagen de: pantalla Consultar resultados con filtros y tabla]

#### Ver el detalle de un resultado

1. Haga clic en el ícono de **ojo** a la derecha de la fila.
2. Se abre un panel con:
   - DNI y cantidad de vistas.
   - Teléfono y nombre del paciente (editables).
   - Lista de archivos con su tipo (PDF, imagen, video) y una etiqueta **Desactivado** si ya fueron dados de baja.
   - Casilla para decidir si desea **reenviar el SMS** al paciente al guardar los cambios.
   - Botón para **abrir el visor** en una pestaña nueva y ver lo mismo que ve el paciente.

[pegar imagen de: modal Detalle de resultado con campos editables]

### 5.3 Editar o corregir un resultado

Si después de cargar un resultado detecta un error (por ejemplo, subió un PDF equivocado), puede corregirlo sin tener que empezar de cero.

1. En **Resultados > Consultar**, ubique el resultado que desea corregir.
2. Haga clic en el ícono de **lápiz** a la derecha de la fila.
3. En el panel **Editar archivos** verá:
   - La lista de archivos actuales con un interruptor para **activar** o **desactivar** cada uno. Los desactivados dejan de mostrarse en el visor del paciente, pero no se borran.
   - Un recuadro para **arrastrar archivos nuevos** que se sumarán al mismo visor.
   - Una casilla para decidir si desea **reenviar el SMS** al paciente avisándole de la corrección.
4. Para desactivar un archivo, mueva el interruptor; el sistema le pedirá un motivo opcional que queda registrado en la bitácora.
5. Haga clic en **Guardar cambios**.

[pegar imagen de: modal Editar archivos con interruptores y dropzone]

> **Cuidado:** no puede dejar un resultado sin archivos activos. Si intenta desactivar el último archivo, el sistema le avisa y no se lo permite.
>
> **Tip:** si después de desactivar un archivo se arrepiente, vuelva a la misma pantalla y active el interruptor de nuevo. No hay límite de tiempo para hacerlo.

### 5.4 Pacientes pendientes

Esta vista muestra los pacientes que ya fueron registrados en admisión pero todavía no tienen resultados cargados. Es la misma información que ve en la campana, pero en forma de tabla completa.

1. En el menú lateral, abra **Resultados** y luego **Pendientes**.
2. La tabla muestra DNI, paciente, teléfono y fecha de registro.
3. Haga clic en **Subir resultados** en la fila del paciente para abrir el formulario con los datos ya cargados.
4. Si desea actualizar la lista, haga clic en **Actualizar** arriba a la derecha.

[pegar imagen de: pantalla Pendientes de subir con la tabla]

### 5.5 Bitácora de carga (logs)

Es el registro de todos los intentos de carga, útil para auditoría y para entender qué ocurrió con un paciente en particular.

1. En el menú lateral, abra **Resultados** y luego **Logs de carga**.
2. Filtre por **DNI/CE** o por **rango de fechas**.
3. La tabla muestra: fecha, estado, DNI, usuario que cargó, nombre de RENIEC, nombre del PDF, si coinciden y un mensaje.

[pegar imagen de: pantalla Logs de carga con la tabla de bitácora]

> **Tip:** la columna **Coincide** indica si el nombre del PDF coincidió con RENIEC. Es la primera columna a revisar si desea entender por qué una carga requirió confirmación manual.

---

## 6. Módulo Gestión de usuarios

> **Roles:** ADMIN, RESULTADOS, ADMISION

Este módulo permite crear, reactivar y anular el acceso de los pacientes al portal. Si su rol es solo **RESULTADOS**, verá el listado pero las acciones de crear y anular están reservadas a admisión y al administrador.

### 6.1 Buscar un paciente

1. En el menú lateral, abra **Gestión de usuarios**.
2. En la barra superior, escriba el DNI en el campo de búsqueda.
3. Haga clic en **Buscar** o presione **Enter**.
4. La tabla muestra: documento de identidad, paciente, cantidad de resultados, fecha de última generación y estado de la cuenta (Sin cuenta, Activo o Anulado).

[pegar imagen de: pantalla Gestión de usuarios con filtros y tabla]

### 6.2 Crear una cuenta nueva

1. Si el paciente no tiene cuenta, la fila muestra la etiqueta **Sin cuenta** y un botón **Generar**.
2. Haga clic en **Generar**.
3. Se abre un formulario con el DNI ya completado. Complete:
   - **Nombre completo**: si lo deja vacío, el sistema intenta traerlo de RENIEC cuando ingresó un DNI de 8 dígitos.
   - **Teléfono**: si lo ingresa, marque la casilla **Enviar credencial por SMS** para que la clave le llegue al paciente por mensaje de texto.
4. Haga clic en **Generar**.
5. Aparece una pantalla con la **credencial** del paciente: web, nombre, usuario, clave y un **código QR** para que pueda acceder desde el celular.

[pegar imagen de: modal Generar credencial con campos DNI, nombre y teléfono]

[pegar imagen de: modal Credencial del paciente con los datos y el QR]

> **Aviso:** la clave es aleatoria y se muestra una sola vez. Asegúrese de imprimirla o entregarla antes de cerrar la pantalla.
>
> **Tip:** si necesita crear la cuenta de un paciente que todavía no tiene resultados cargados, use el botón **Nuevo usuario** arriba a la derecha de la tabla.

### 6.3 Reactivar una cuenta anulada

1. Si la cuenta del paciente fue anulada, la fila muestra la etiqueta **Anulado** y un botón **Regenerar**.
2. Haga clic en **Regenerar**.
3. Se abre el mismo formulario de generación. Al confirmar, se crea una **nueva clave** para el paciente.

> **Cuidado:** regenerar la clave invalida la anterior. Informe al paciente por el canal que utilice para que use la nueva.

### 6.4 Anular el acceso de un paciente

1. Si el paciente tiene una cuenta activa, la fila muestra la etiqueta **Activo** y un botón **Anular**.
2. Haga clic en **Anular**.
3. El sistema pide confirmación. Haga clic en **Sí, anular** para confirmar o **No** para cancelar.
4. Una vez anulada, el paciente no podrá iniciar sesión en el portal.

[pegar imagen de: diálogo Confirmar anulación de acceso]

> **Cuidado:** la anulación no borra los resultados ya entregados al paciente. Mientras su cuenta siga activa, podrá seguir viendo sus archivos en el portal hasta que el visor deje de estar disponible.

### 6.5 Imprimir la credencial

Después de generar o regenerar una credencial:

1. Haga clic en **Imprimir** en la pantalla de la credencial.
2. Se abre una ventana lista para imprimir en formato de ticket (80 mm de ancho).
3. Confirme la impresión en la ventana del navegador.

[pegar imagen de: vista previa del ticket de credencial a imprimir]

> **Tip:** utilice una impresora de tickets térmica para entregar la credencial al paciente en el momento.

---

## 7. Módulo Administración

> **Rol:** ADMIN

Este módulo es exclusivo del administrador del sistema y permite gestionar los usuarios internos (personal del laboratorio o clínica).

### 7.1 Listar y buscar usuarios internos

1. En el menú lateral, abra **Administración**.
2. La tabla muestra: usuario, nombre, correo, DNI, estado (activo/inactivo), roles y fechas.
3. Filtre por **texto libre**, **rol** o **estado** y haga clic en **Buscar**.

[pegar imagen de: pantalla Administración con la lista de usuarios internos]

### 7.2 Crear un usuario interno

1. Haga clic en **Nuevo usuario** arriba a la derecha.
2. Complete:
   - **Usuario**: nombre de inicio de sesión (generalmente el DNI).
   - **Nombre completo**: en formato "APELLIDO_P APELLIDO_M, NOMBRES".
   - **Correo** y **DNI** (opcionales).
   - **Contraseña**: ingrésela dos veces.
   - **Roles**: marque uno o más roles.
3. Haga clic en **Guardar**.

[pegar imagen de: modal Nuevo usuario interno]

> **Cuidado:** la contraseña se ingresa en texto plano en el formulario. Comunique la clave al usuario nuevo por un canal seguro y solicite que la cambie al primer ingreso.

### 7.3 Editar datos de un usuario

1. En la fila del usuario, haga clic en el ícono de **lápiz**.
2. Modifique los datos que necesite (nombre, correo, DNI).
3. Haga clic en **Guardar**.

> **Aviso:** desde aquí no se cambia la contraseña ni los roles. Use las acciones específicas para eso.

### 7.4 Asignar y cambiar roles

1. En la fila del usuario, haga clic en el ícono de **roles** (escudo o etiqueta).
2. Active o desactive los roles según corresponda.
3. Haga clic en **Guardar**.

[pegar imagen de: modal Asignar roles con checkboxes]

> **Cuidado:** si quita todos los roles, el usuario no podrá ingresar a ningún módulo. Verifique antes de guardar.

### 7.5 Cambiar contraseña

1. En la fila del usuario, haga clic en el ícono de **llave**.
2. Ingrese la nueva contraseña dos veces.
3. Haga clic en **Guardar**.

> **Tip:** avise al usuario por un canal seguro cuál es su nueva contraseña y solicite que la cambie al primer ingreso.

### 7.6 Activar o desactivar un usuario

1. En la fila del usuario, use el interruptor de la columna **Estado** o el botón correspondiente.
2. Confirme el cambio.

Un usuario desactivado no puede iniciar sesión, pero su historial se conserva. Sirve, por ejemplo, cuando alguien se va del equipo.

---

## 8. Portal del paciente

> **Rol:** PACIENTE (también accesible para ADMIN en modo consulta)

Este es el portal al que ingresan los pacientes para ver y descargar sus estudios.

### 8.1 Ver mis resultados

1. Inicie sesión con su usuario y contraseña.
2. Ingresará automáticamente a **Mis resultados**.
3. La pantalla muestra una tarjeta por cada estudio que tenga disponible, con la fecha y la cantidad de archivos.
4. Si tiene muchos resultados, use los filtros **desde / hasta** para acotar la búsqueda.

[pegar imagen de: portal del paciente con tarjetas de resultados]

### 8.2 Abrir un resultado

1. Haga clic en **Ver archivos** sobre la tarjeta del estudio.
2. Se abre un panel con:
   - El mensaje que le envió el laboratorio (si incluye enlaces, puede hacer clic).
   - La lista de archivos disponibles, cada uno con su tipo (PDF, imagen, video).

[pegar imagen de: modal de detalle de un resultado del paciente]

### 8.3 Ver o descargar un archivo

Para cada archivo:

- **Ver**: abre el archivo en una pestaña nueva del navegador.
- **Descargar**: guarda el archivo en su computadora o celular con el nombre original.

> **Cuidado:** los archivos quedan disponibles durante 6 meses desde la entrega. Descargue los que desee guardar pasado ese plazo.
>
> **Aviso:** si un archivo aparece como **Desactivado** en el visor (caso del panel de consulta del laboratorio), no lo verá en el portal.

---

## 9. Glosario

| Término | Significado |
|---|---|
| **DNI** | Documento Nacional de Identidad peruano (8 dígitos). |
| **CE** | Carnet de Extranjería (9 a 12 dígitos). |
| **RENIEC** | Registro Nacional de Identificación y Estado Civil. La plataforma consulta ahí para validar el nombre del paciente. |
| **SMS** | Mensaje corto de texto que llega al celular del paciente avisándole que sus resultados ya están disponibles y la dirección del portal para que pueda ingresar. |
| **Portal del paciente** | Sitio web al que accede el paciente con su usuario y contraseña para ver y descargar sus estudios. Es donde se abre el visor. |
| **Visor** | Pantalla dentro del portal del paciente donde se listan y abren los archivos de un estudio. |
| **Enlace acortado** | Dirección corta que el sistema usa internamente para identificar una carga de resultados. **No se comparte al paciente**: el paciente siempre ingresa al portal con su usuario y contraseña. Solo el personal autorizado puede abrirlo desde la opción **Abrir visor** del detalle de un resultado. |
| **Pendiente** | Paciente registrado en admisión que todavía no tiene resultados cargados. |
| **Credencial** | Tarjeta (física o impresa) con usuario, clave y código QR que se entrega al paciente para acceder al portal. |
| **Bitácora** | Registro cronológico de todos los intentos de carga. |
| **Soft-delete** | Dar de baja un archivo sin borrarlo físicamente: el paciente deja de verlo, pero el archivo sigue existiendo. |
| **RENIEC (validación)** | Comparación automática entre el nombre detectado en el PDF y el nombre oficial de RENIEC. |

---

## 10. Preguntas frecuentes y soporte

### No me llegó el SMS al paciente

Revise en orden:

1. Que el número esté bien escrito y empiece con 9.
2. Que el operador del celular del paciente tenga cobertura.
3. En **Resultados > Consultar**, abra el detalle del resultado y revise la columna **SMS**. Si dice **Encolado**, está pendiente de envío. Si dice **Fallido**, hubo un error al enviar.
4. Si todo lo anterior está bien y el problema persiste, contacte al teléfono de soporte que aparece en la pantalla de recuperación de contraseña.

### ¿Cómo le reenvío el SMS al paciente?

1. Entre a **Resultados > Consultar**.
2. Ubique el resultado del paciente.
3. Abra el detalle y marque **Reenviar SMS al paciente**.
4. Haga clic en **Guardar**.

### Me equivoqué al subir un PDF

No hace falta empezar de cero:

1. Entre a **Resultados > Consultar** y localice el resultado.
2. Haga clic en el ícono de **lápiz** para abrir el editor de archivos.
3. Desactive el archivo equivocado (indique un motivo breve) y arrastre el PDF correcto.
4. Si desea avisarle al paciente, marque **Reenviar SMS** antes de guardar.

### ¿Puedo borrar definitivamente un archivo?

No. La plataforma solo permite **desactivar** archivos (soft-delete). Esto preserva la trazabilidad: si más adelante necesita revisar la historia del paciente, el archivo seguirá disponible para auditoría.

### Olvidé mi usuario o contraseña de paciente

- Si recuerda el teléfono, use la opción **¿Olvidaste tu contraseña?** desde la pantalla de login.
- Si no recuerda el teléfono registrado, comuníquese con el laboratorio o la clínica para que le generen una nueva credencial.

### ¿Por cuánto tiempo están disponibles mis resultados?

El visor permanece activo durante 6 meses desde la entrega. Después de ese plazo los archivos dejan de estar disponibles en línea. Se recomienda descargar todo lo que se desee conservar antes de que se cumpla el período.

### No puedo ver o descargar un archivo

- Pruebe con otro navegador o con la ventana de incógnito.
- Verifique que no esté en una red corporativa que bloquee descargas.
- Si el problema persiste, comuníquese con el laboratorio.

### Soporte

Para cualquier consulta adicional, comuníquese al teléfono de soporte que aparece en la pantalla de recuperación de contraseña o al administrador del sistema en su institución.

---

> **Aviso final:** este manual describe el uso de la plataforma. La plataforma se actualiza de forma continua; pueden aparecer pequeñas diferencias visuales entre las capturas y la versión actual del sistema, pero los flujos de trabajo siguen siendo los mismos.
