package pe.com.qallpatic.osteoperu.compartido.util;

/**
 * Parser simple de User-Agent para reducir la cadena completa a un par
 * "Browser / OS" entendible. No pretende cubrir todos los UAs del mundo:
 * solo los mas comunes para fines de auditoria (Chrome/Edge/Firefox/Safari
 * + Windows/macOS/Linux/Android/iOS). Si no reconoce nada, devuelve un
 * resumen corto del UA original.
 */
public final class UserAgentParser {

	private UserAgentParser() {
	}

	/**
	 * Devuelve un resumen legible del User-Agent.
	 * Formato: "Browser / OS" o "Desconocido" si no se reconoce.
	 */
	public static String resumir(String ua) {
		if (ua == null || ua.isBlank()) {
			return "";
		}
		String browser = detectarBrowser(ua);
		String os = detectarOs(ua);
		if (browser.isEmpty() && os.isEmpty()) {
			return truncar(ua, 60);
		}
		if (browser.isEmpty()) {
			return os;
		}
		if (os.isEmpty()) {
			return browser;
		}
		return browser + " / " + os;
	}

	private static String detectarBrowser(String ua) {
		// Orden importa: navegadores que se disfrazan de Chrome van primero.
		if (ua.contains("Edg/") || ua.contains("Edge/")) {
			return "Edge";
		}
		if (ua.contains("OPR/") || ua.contains("Opera")) {
			return "Opera";
		}
		if (ua.contains("SamsungBrowser/")) {
			return "Samsung Browser";
		}
		if (ua.contains("Firefox/")) {
			return "Firefox";
		}
		// Chrome aparece en UAs de Edge/Opera tambien, por eso va despues.
		if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
			return "Chrome";
		}
		if (ua.contains("Safari/") && !ua.contains("Chrome/")) {
			return "Safari";
		}
		if (ua.contains("MSIE ") || ua.contains("Trident/")) {
			return "Internet Explorer";
		}
		return "";
	}

	private static String detectarOs(String ua) {
		if (ua.contains("Windows NT")) {
			return "Windows";
		}
		if (ua.contains("Android")) {
			return "Android";
		}
		if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
			return "iOS";
		}
		if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
			return "macOS";
		}
		if (ua.contains("Linux") || ua.contains("X11")) {
			return "Linux";
		}
		return "";
	}

	private static String truncar(String s, int max) {
		if (s.length() <= max) {
			return s;
		}
		return s.substring(0, max - 1) + "...";
	}
}
