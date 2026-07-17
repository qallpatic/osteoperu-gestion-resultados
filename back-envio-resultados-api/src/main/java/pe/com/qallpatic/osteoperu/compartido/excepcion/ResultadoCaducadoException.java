package pe.com.qallpatic.osteoperu.compartido.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un paciente intenta acceder a un resultado que ya vencio
 * (HTTP 410 Gone). El ADMIN no dispara esta excepcion: ve todo.
 */
public class ResultadoCaducadoException extends ExcepcionNegocio {

	private final String dni;

	public ResultadoCaducadoException(String mensaje, String dni) {
		super(mensaje, HttpStatus.GONE);
		this.dni = dni;
	}

	public String getDni() {
		return dni;
	}
}
