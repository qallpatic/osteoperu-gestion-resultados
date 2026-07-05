package pe.com.qallpatic.resultados.service;

import java.security.SecureRandom;

/** Genera codigos cortos y tokens aleatorios (URL-safe). */
public final class ShortCodeGenerator {

	private static final String ALPHANUM =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private ShortCodeGenerator() {
	}

	public static String generate(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
		}
		return sb.toString();
	}
}
