package pe.com.qallpatic.osteoperu.seguridad.dto;

/**
 * Respuesta del endpoint publico de recuperacion de password. Por seguridad
 * siempre se devuelve el mismo mensaje (en {@code mensajeUsuario}) y el
 * telefono de soporte, exista o no el paciente. El campo
 * {@code telefonoOfuscado} muestra los ultimos 4 digitos del telefono al que
 * se envio (o del DNI tipeado, si no se encontro paciente) para que el
 * usuario verifique visualmente sin comprometer la informacion.
 *
 * @param mensajeUsuario texto a mostrar (generico).
 * @param soporteTelefono telefono de soporte configurado (app.soporte.telefono).
 * @param telefonoOfuscado ultimos 4 digitos enmascarados (ej: "•••••6789").
 * @param retryAfterSeconds segundos a esperar si el caller fue bloqueado (429). Null en 200.
 */
public record RecuperarPasswordResponse(
		String mensajeUsuario,
		String soporteTelefono,
		String telefonoOfuscado,
		Long retryAfterSeconds) {
}
