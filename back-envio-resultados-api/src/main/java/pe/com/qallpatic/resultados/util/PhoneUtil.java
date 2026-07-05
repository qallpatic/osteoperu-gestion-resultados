package pe.com.qallpatic.resultados.util;

import pe.com.qallpatic.resultados.exception.BadRequestException;

/** Normaliza y valida numeros de telefono peruanos. */
public final class PhoneUtil {

	private PhoneUtil() {
	}

	/**
	 * Deja el numero como {countryCode}{9 digitos}, aceptando entradas con
	 * espacios, guiones o el prefijo +51. Ej: "987 654 321" -> "51987654321".
	 */
	public static String normalize(String rawPhone, String countryCode) {
		if (rawPhone == null || rawPhone.isBlank()) {
			throw new BadRequestException("El numero de telefono es obligatorio.");
		}
		String digits = rawPhone.replaceAll("\\D", "");

		if (digits.startsWith(countryCode) && digits.length() == countryCode.length() + 9) {
			// ya viene con prefijo de pais
			return digits;
		}
		if (digits.length() == 9) {
			return countryCode + digits;
		}
		throw new BadRequestException("Numero de telefono invalido. Debe tener 9 digitos (Peru).");
	}

	public static String validateDni(String dni) {
		if (dni == null || dni.isBlank()) {
			throw new BadRequestException("El DNI es obligatorio.");
		}
		String clean = dni.replaceAll("\\D", "");
		if (clean.length() != 8) {
			throw new BadRequestException("DNI invalido. Debe tener 8 digitos.");
		}
		return clean;
	}
}
