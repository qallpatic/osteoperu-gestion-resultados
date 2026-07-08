package pe.com.qallpatic.osteoperu.compartido.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de negocio. Cada dominio define sus excepciones propias
 * extendiendo esta clase; el manejador global las traduce a la respuesta HTTP
 * correspondiente usando {@link #getEstado()}.
 */
public abstract class ExcepcionNegocio extends RuntimeException {

	private final transient HttpStatus estado;

	protected ExcepcionNegocio(String mensaje, HttpStatus estado) {
		super(mensaje);
		this.estado = estado;
	}

	public HttpStatus getEstado() {
		return estado;
	}
}
