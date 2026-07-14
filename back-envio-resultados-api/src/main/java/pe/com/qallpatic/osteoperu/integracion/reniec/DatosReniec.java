package pe.com.qallpatic.osteoperu.integracion.reniec;

import java.util.Optional;

/**
 * Datos de identidad devueltos por RENIEC.
 * Formato canonico del nombre completo: "APELLIDO_P APELLIDO_M, NOMBRES".
 */
public record DatosReniec(
		String dni,
		String nombres,
		String apellidoPaterno,
		String apellidoMaterno,
		Integer codigoVerificacion) {

	/** Constructor de compatibilidad (4 argumentos) — asume sin codigo de verificacion. */
	public DatosReniec(String dni, String nombres, String apellidoPaterno, String apellidoMaterno) {
		this(dni, nombres, apellidoPaterno, apellidoMaterno, null);
	}

	/** Nombre completo canonico: "APELLIDO_P APELLIDO_M, NOMBRES". */
	public String nombreCompleto() {
		String apellidos = (safe(apellidoPaterno) + " " + safe(apellidoMaterno)).trim().replaceAll("\\s+", " ");
		String noms = safe(nombres).trim().replaceAll("\\s+", " ");
		if (apellidos.isEmpty()) {
			return noms;
		}
		if (noms.isEmpty()) {
			return apellidos;
		}
		return apellidos + ", " + noms;
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}