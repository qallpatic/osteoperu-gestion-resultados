package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;

/** Fila de la consulta de resultados. */
public record ResultadoResumenResponse(
		Long id,
		String dni,
		String nombrePaciente,
		String telefono,
		String estadoSms,
		int contadorVistas,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaPrimeraVista) {
}
