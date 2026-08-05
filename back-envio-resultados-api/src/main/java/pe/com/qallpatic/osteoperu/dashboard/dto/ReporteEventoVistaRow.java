package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDateTime;

/**
 * Fila plana para el reporte Excel de "Vistas de pacientes". Una fila por
 * evento registrado en {@code EVENTO_VISTA} (CLICK/VISTA/DESCARGA).
 * El service arma la lista con los datos del evento + paciente del cabecere.
 *
 * <p>El {@code userAgent} llega como cadena cruda; el service lo resume
 * via {@link pe.com.qallpatic.osteoperu.compartido.util.UserAgentParser}
 * antes de volcarlo al Excel para no saturar al usuario con UAs completos.
 */
public record ReporteEventoVistaRow(

		LocalDateTime fechaCreacion,
		String tipoEvento,
		Long idResultado,
		String documentoIdentidad,
		String nombrePaciente,
		String ip,
		String userAgent) {
}
