package pe.com.qallpatic.osteoperu.integracion.reniec;

import java.util.Optional;

/**
 * Puerto de consulta a RENIEC. Las implementaciones (mock / HTTP real) se
 * seleccionan por configuracion ({@code reniec.enabled}).
 */
public interface ClienteReniec {

	/** Devuelve los datos del DNI, o vacio si no se pudo verificar. */
	Optional<DatosReniec> consultar(String dni);
}
