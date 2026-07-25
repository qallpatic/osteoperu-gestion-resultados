package pe.com.qallpatic.osteoperu.compartido.util;

import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;

/** Normaliza y valida numeros de telefono y documentos de identidad peruanos. */
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

	/**
	 * Valida un DNI peruano estricto (8 digitos). Pensado para endpoints que
	 * requieren DNI real (ej. consulta a RENIEC). Para carga o admision de
	 * pacientes extranjeros usar {@link #validarDocumentoIdentidad(String)}.
	 */
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

	/**
	 * Valida un documento de identidad peruano: DNI (8 digitos) o Carnet de
	 * Extranjeria (CE, 9 a 12 caracteres alfanumericos en mayusculas). Usar en
	 * endpoints donde se aceptan extranjeros (carga de resultados, alta de
	 * cuenta del paciente).
	 */
	public static String validarDocumentoIdentidad(String doc) {
		if (doc == null || doc.isBlank()) {
			throw new SolicitudInvalidaException("El documento de identidad es obligatorio.");
		}
		String clean = doc.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
		int len = clean.length();
		boolean esDni = len == 8;
		boolean esCe = len >= 9 && len <= 12;
		if (!esDni && !esCe) {
			throw new SolicitudInvalidaException(
					"Documento invalido. DNI debe tener 8 digitos y CE entre 9 y 12 caracteres alfanumericos.");
		}
		return clean;
	}
}
