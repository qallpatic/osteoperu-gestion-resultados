package pe.com.qallpatic.osteoperu.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** Se lanza cuando no existe el recurso solicitado (HTTP 404). */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje, HttpStatus.NOT_FOUND);
	}
}
