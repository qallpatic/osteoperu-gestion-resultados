package pe.com.qallpatic.osteoperu.compartido.util;

import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

/**
 * Resuelve un {@link Sort} a partir de los parametros de query
 * {@code ordenarPor} y {@code direccion} que llegan del frontend.
 *
 * <p>Se usa una whitelist explicita para evitar que el cliente envie nombres de
 * columnas arbitrarias y dispare errores de Hibernate o exponga campos
 * sensibles. Si el alias enviado no esta en la whitelist, se cae al default
 * (con la direccion solicitada por el front).
 */
public final class OrdenamientoUtil {

	private OrdenamientoUtil() {
	}

	/**
	 * @param ordenarPor    alias del campo (logico del front).
	 * @param direccion     "asc" o "desc". Cualquier otro valor se trata como "asc".
	 * @param alias         mapa alias -> propiedad JPA. Si el alias no esta, se
	 *                      prueba el valor tal cual.
	 * @param camposValidos whitelist final de propiedades JPA aceptadas.
	 * @param defaultField  propiedad JPA usada cuando el alias es invalido o vacio.
	 */
	public static Sort resolver(String ordenarPor, String direccion,
			Map<String, String> alias, Set<String> camposValidos, String defaultField) {
		Sort.Direction dir = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
		if (ordenarPor == null || ordenarPor.isBlank()) {
			return Sort.by(Sort.Direction.DESC, defaultField);
		}
		String prop = alias.getOrDefault(ordenarPor, ordenarPor);
		if (!camposValidos.contains(prop)) {
			return Sort.by(Sort.Direction.DESC, defaultField);
		}
		return Sort.by(dir, prop);
	}
}