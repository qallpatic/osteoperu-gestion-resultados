package pe.com.qallpatic.osteoperu.resultados.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una cabecera con sus archivos (N detalle por 1 cabecera). */
public record ResultadoDetalleResponse(
		Long id,
		String idGenerado,
		String dni,
		String nombrePaciente,
		String mensaje,
		String telefono,
		String tokenPublico,
		String viewerUrl,
		String estadoSms,
		int contadorVistas,
		Integer estado,
		LocalDateTime fechaEliminacion,
		String usuarioEliminacion,
		String motivoEliminacion,
		int cantidadArchivos,
		BigDecimal pesoTotalMb,
		LocalDateTime fechaHoraCarga,
		LocalDateTime fechaVigenciaHasta,
		LocalDateTime fechaPrimeraVista,
		LocalDateTime fechaUltimaVista,
		String soporteTelefono,
		List<ArchivoResponse> archivos) {
}
