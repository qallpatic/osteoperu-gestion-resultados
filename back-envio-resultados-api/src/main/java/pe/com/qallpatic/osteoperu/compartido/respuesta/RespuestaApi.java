package pe.com.qallpatic.osteoperu.compartido.respuesta;

import java.time.Instant;

/**
 * Envoltura uniforme para las respuestas exitosas de la API.
 * Estandar para los endpoints nuevos (autenticacion, administracion, etc.).
 *
 * @param success indica si la operacion fue exitosa
 * @param message mensaje legible para el cliente
 * @param data    carga util de la respuesta
 * @param timestamp instante de generacion
 */
public record RespuestaApi<T>(boolean success, String message, T data, Instant timestamp) {

	public static <T> RespuestaApi<T> ok(T data) {
		return new RespuestaApi<>(true, "OK", data, Instant.now());
	}

	public static <T> RespuestaApi<T> ok(String message, T data) {
		return new RespuestaApi<>(true, message, data, Instant.now());
	}

	public static RespuestaApi<Void> mensaje(String message) {
		return new RespuestaApi<>(true, message, null, Instant.now());
	}
}
