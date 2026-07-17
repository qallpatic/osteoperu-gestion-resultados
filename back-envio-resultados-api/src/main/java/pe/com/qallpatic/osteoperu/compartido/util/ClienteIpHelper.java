package pe.com.qallpatic.osteoperu.compartido.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Helper centralizado para extraer la IP del cliente. Considera:
 * <ul>
 *   <li>Header X-Forwarded-For (si hay proxy reverso / Nginx) tomando el
 *       primer valor antes de la primera coma.</li>
 *   <li>request.getRemoteAddr() como fallback.</li>
 *   <li>Si no se puede determinar, devuelve "desconocida" para que el
 *       rate-limit registre la fila en lugar de fallar.</li>
 * </ul>
 * Requiere {@code server.forward-headers-strategy=framework} (o NATIVE) en
 * application.yml para que Spring resuelva los forwarded headers.
 */
public final class ClienteIpHelper {

	private ClienteIpHelper() {
	}

	public static String resolver(HttpServletRequest request) {
		if (request == null) {
			return "desconocida";
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int coma = forwarded.indexOf(',');
			String primera = (coma > 0 ? forwarded.substring(0, coma) : forwarded).trim();
			if (!primera.isEmpty()) {
				return primera;
			}
		}
		String remota = request.getRemoteAddr();
		return remota == null || remota.isBlank() ? "desconocida" : remota;
	}
}
