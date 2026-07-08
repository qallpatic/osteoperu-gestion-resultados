package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Detalle de un resultado con sus archivos. */
public record ResultadoDetalleResponse(
		Long id,
		String dni,
		String nombrePaciente,
		String mensaje,
		String telefono,
		String carpeta,
		String tokenPublico,
		String viewerUrl,
		String estadoSms,
		int contadorVistas,
		LocalDateTime fechaPrimeraVista,
		LocalDateTime fechaUltimaVista,
		LocalDateTime fechaCreacion,
		List<ArchivoResponse> archivos) {
}
