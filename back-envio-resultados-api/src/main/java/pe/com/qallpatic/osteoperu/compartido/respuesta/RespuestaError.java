package pe.com.qallpatic.osteoperu.compartido.respuesta;

import java.time.Instant;

/**
 * Cuerpo uniforme de error de la API. Se mantienen las claves convencionales
 * (timestamp, status, error, message, path) para compatibilidad con clientes y
 * herramientas estandar.
 */
public record RespuestaError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path) {
}
