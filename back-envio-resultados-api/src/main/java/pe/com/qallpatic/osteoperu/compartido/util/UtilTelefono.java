package pe.com.qallpatic.osteoperu.compartido.util;

import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;

/** Normaliza y valida numeros de telefono y DNI peruanos. */
public final class UtilTelefono {

	private UtilTelefono() {
	}

	/**
	 * Deja el numero como {countryCode}{9 digitos}, aceptando entradas con
	 * espacios, guiones o el prefijo +51. Ej: "987 654 321" -> "51987654321".
	 * Si el valor llega vacio o null devuelve null (telefono opcional).
	 */
	public static String normalizar(String rawPhone, String countryCode) {
		String digits = sanitizarDigitos(rawPhone);
		if (digits.isEmpty()) {
			return null;
		}

		if (digits.startsWith(countryCode) && digits.length() == countryCode.length() + 9) {
			return digits;
		}
		if (digits.length() == 9) {
			return countryCode + digits;
		}
		throw new SolicitudInvalidaException("Numero de telefono invalido. Debe tener 9 digitos (Peru).");
	}

	private static String sanitizarDigitos(String rawPhone) {
		return rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
	}

	public static String validarDni(String dni) {
		if (dni == null || dni.isBlank()) {
			throw new SolicitudInvalidaException("El DNI es obligatorio.");
		}
		String clean = dni.replaceAll("\\D", "");
		if (clean.length() != 8) {
			throw new SolicitudInvalidaException("DNI invalido. Debe tener 8 digitos.");
		}
		return clean;
	}
}
