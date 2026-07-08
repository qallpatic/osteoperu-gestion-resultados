package pe.com.qallpatic.osteoperu.usuarios.exception;

import org.springframework.http.HttpStatus;

import pe.com.qallpatic.osteoperu.compartido.excepcion.ExcepcionNegocio;

/** Se lanza cuando ya existe un usuario con el mismo login o DNI (HTTP 409). */
public class UsuarioYaExisteException extends ExcepcionNegocio {

	public UsuarioYaExisteException(String mensaje) {
		super(mensaje, HttpStatus.CONFLICT);
	}
}
