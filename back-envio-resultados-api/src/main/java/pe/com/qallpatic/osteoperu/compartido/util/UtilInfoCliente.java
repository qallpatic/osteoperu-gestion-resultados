package pe.com.qallpatic.osteoperu.compartido.util;

import jakarta.servlet.http.HttpServletRequest;

/** Extrae IP y user-agent respetando cabeceras de proxy (nginx). */
public final class UtilInfoCliente {

	private UtilInfoCliente() {
	}

	public static String ipCliente(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public static String userAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String ua = request.getHeader("User-Agent");
		if (ua != null && ua.length() > 500) {
			return ua.substring(0, 500);
		}
		return ua;
	}
}
