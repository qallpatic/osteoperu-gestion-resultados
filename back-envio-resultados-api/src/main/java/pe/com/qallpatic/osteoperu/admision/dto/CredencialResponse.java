package pe.com.qallpatic.osteoperu.admision.dto;

/**
 * Credencial del paciente para imprimir/entregar. La clave se muestra UNA sola
 * vez (en la BD queda cifrada con BCrypt). El QR apunta al portal.
 */
public record CredencialResponse(
		String dni,
		String nombre,
		String usuario,
		String clave,
		String portalUrl,
		String qrUrl,
		boolean smsEnviado) {
}
