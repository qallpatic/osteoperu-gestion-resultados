package pe.com.qallpatic.osteoperu.resultados.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de la consulta de cabeceras (transacciones) de un paciente o del agente. */
public record ResultadoResumenResponse(
		Long id,
		String idGenerado,
		String dni,
		String nombrePaciente,
		String telefono,
		String estadoSms,
		int contadorVistas,
		int cantidadArchivos,
		BigDecimal pesoTotalMb,
		Integer estado,
		LocalDateTime fechaHoraCarga,
		LocalDateTime fechaVigenciaHasta,
		LocalDateTime fechaPrimeraVista) {
}
