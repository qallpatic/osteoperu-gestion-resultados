package pe.com.qallpatic.osteoperu.seguridad.dto;

/**
 * Respuesta del endpoint publico de recuperacion de password. Por seguridad
 * siempre se devuelve el mismo mensaje (en {@code mensajeUsuario}), exista o
 * no el paciente, y se incluye el telefono de soporte que el front muestra
 * cuando el SMS no llega.
 *
 * @param mensajeUsuario texto a mostrar (generico).
 * @param soporteTelefono telefono de soporte configurado (app.soporte.telefono).
 * @param retryAfterSeconds segundos a esperar si el caller fue bloqueado (429). Null en 200.
 */
public record RecuperarPasswordResponse(
		String mensajeUsuario,
		String soporteTelefono,
		Long retryAfterSeconds) {
}
