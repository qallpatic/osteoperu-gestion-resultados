package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;

/** Fila de la consulta de logs de carga. */
public record LogCargaResponse(
		Long id,
		Long idResultado,
		String dni,
		String usuarioCarga,
		String estado,
		String mensaje,
		String nombreReniec,
		String nombrePdf,
		Boolean coincideNombre,
		LocalDateTime fechaCreacion) {
}
