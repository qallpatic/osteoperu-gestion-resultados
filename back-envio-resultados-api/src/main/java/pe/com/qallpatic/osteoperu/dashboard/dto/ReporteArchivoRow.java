package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila plana para el reporte Excel de "Almacenamiento usado".
 * El service mapea desde {@code Object[]} que devuelve la query de
 * {@code ResultadoArchivoRepository.listarParaReporte} (con join a
 * ResultadoCarga para traer paciente y DNI del cabecere).
 */
public record ReporteArchivoRow(

		LocalDateTime fechaCreacion,
		String documentoIdentidad,
		String nombrePaciente,
		String nombreOriginal,
		String tipoContenido,
		String extension,
		BigDecimal pesoMb,
		String tipoMedio,
		Integer estado,
		LocalDateTime fechaEliminacion,
		String usuarioEliminacion,
		String motivoEliminacion,
		String usuarioCreacion) {
}
