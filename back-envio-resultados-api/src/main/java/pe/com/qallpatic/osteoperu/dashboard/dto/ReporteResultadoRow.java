package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila plana para el reporte Excel de "Resultados cargados".
 * Incluye registros vivos y soft-deleted (estado = 0); las columnas
 * de eliminacion quedan null/vacias cuando el registro esta vivo.
 *
 * <p>El service mapea desde {@code Object[]} que devuelve la query de
 * {@code ResultadoCargaRepository.listarParaReporte} (mismo patron que
 * las otras proyecciones crudas del dashboard).
 */
public record ReporteResultadoRow(

		LocalDateTime fechaHoraCarga,
		String documentoIdentidad,
		String nombrePaciente,
		String telefono,
		Integer cantidadArchivos,
		BigDecimal pesoTotalMb,
		String estadoSms,
		Integer estado,
		LocalDateTime fechaEliminacion,
		String usuarioEliminacion,
		String motivoEliminacion,
		String usuarioCreacion) {
}
