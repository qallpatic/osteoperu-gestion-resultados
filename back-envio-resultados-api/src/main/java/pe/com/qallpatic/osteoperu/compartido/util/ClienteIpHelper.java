package pe.com.qallpatic.osteoperu.compartido.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClienteIpHelper {

	private ClienteIpHelper() {
	}

	public static String resolver(HttpServletRequest request) {
		if (request == null) {
			return "desconocida";
		}
		String remota = request.getRemoteAddr();
		return remota == null || remota.isBlank() ? "desconocida" : remota;
	}
}
