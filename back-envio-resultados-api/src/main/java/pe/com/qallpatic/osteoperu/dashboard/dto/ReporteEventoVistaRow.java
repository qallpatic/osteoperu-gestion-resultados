package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDateTime;

/**
 * Fila plana para el reporte Excel de "Vistas de pacientes". Una fila por
 * evento registrado en {@code EVENTO_VISTA} (CLICK/VISTA/DESCARGA).
 * El service arma la lista con los datos del evento + paciente del cabecere.
 */
public record ReporteEventoVistaRow(

		LocalDateTime fechaCreacion,
		String tipoEvento,
		String codigoCorto,
		Long idResultado,
		String documentoIdentidad,
		String nombrePaciente,
		String ip,
		String userAgent) {
}
