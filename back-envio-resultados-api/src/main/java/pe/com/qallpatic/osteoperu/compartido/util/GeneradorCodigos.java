package pe.com.qallpatic.osteoperu.compartido.util;

import java.security.SecureRandom;

/** Genera codigos cortos y tokens aleatorios (URL-safe). */
public final class GeneradorCodigos {

	private static final String ALFANUMERICO =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private GeneradorCodigos() {
	}

	public static String generar(int longitud) {
		StringBuilder sb = new StringBuilder(longitud);
		for (int i = 0; i < longitud; i++) {
			sb.append(ALFANUMERICO.charAt(RANDOM.nextInt(ALFANUMERICO.length())));
		}
		return sb.toString();
	}
}
