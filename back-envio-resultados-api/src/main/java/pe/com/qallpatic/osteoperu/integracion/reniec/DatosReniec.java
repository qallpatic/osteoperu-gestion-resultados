package pe.com.qallpatic.osteoperu.integracion.reniec;

/** Datos de identidad devueltos por RENIEC. */
public record DatosReniec(String dni, String nombres, String apellidoPaterno, String apellidoMaterno) {

	/** Nombre completo en formato "APELLIDOS NOMBRES". */
	public String nombreCompleto() {
		return (safe(apellidoPaterno) + " " + safe(apellidoMaterno) + " " + safe(nombres))
				.trim().replaceAll("\\s+", " ");
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
