package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDateTime;

/**
 * Cabecera "mas vista" del bucket de Vistas (top N).
 * Alimenta una tabla con los resultados que mas veces fueron
 * abiertos por pacientes.
 */
public record ResultadoMasVistoResponse(

		/** ID inmutable del resultado (estable, sin filtrar soft-delete). */
		String idGenerado,

		/** DNI/CE del paciente titular. */
		String documentoIdentidad,

		/** Nombre del paciente en formato canonico "APELLIDOS, NOMBRES". */
		String nombrePaciente,

		/** Total absoluto de aperturas registradas (campo CONTADOR_VISTAS). */
		long contadorVistas,

		/** Cuando se cargo el resultado (YYYY-MM-DDTHH:MM:SS). */
		LocalDateTime fechaHoraCarga) {
}
