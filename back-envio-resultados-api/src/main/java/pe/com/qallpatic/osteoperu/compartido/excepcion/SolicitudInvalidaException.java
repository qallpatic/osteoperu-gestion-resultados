package pe.com.qallpatic.osteoperu.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** Se lanza cuando la solicitud del cliente es invalida (HTTP 400). */
public class SolicitudInvalidaException extends ExcepcionNegocio {

	public SolicitudInvalidaException(String mensaje) {
		super(mensaje, HttpStatus.BAD_REQUEST);
	}
}
