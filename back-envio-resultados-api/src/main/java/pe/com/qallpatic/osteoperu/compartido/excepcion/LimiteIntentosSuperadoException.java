package pe.com.qallpatic.osteoperu.compartido.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando el rate-limit de un endpoint publico se ha agotado en
 * la ventana actual (HTTP 429). El manejador global la traduce a una
 * respuesta uniforme; el controlador puede enriquecerla con retryAfterSeconds
 * para que el cliente sepa cuando reintentar.
 */
public class LimiteIntentosSuperadoException extends ExcepcionNegocio {

	public LimiteIntentosSuperadoException(String mensaje) {
		super(mensaje, HttpStatus.TOO_MANY_REQUESTS);
	}
}
