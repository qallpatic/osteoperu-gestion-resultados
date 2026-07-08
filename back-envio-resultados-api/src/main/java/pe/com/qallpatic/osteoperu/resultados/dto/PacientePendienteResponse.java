package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;

/** Paciente registrado por Admision, pendiente de subir resultados (campanita). */
public record PacientePendienteResponse(
		String dni,
		String nombre,
		String telefono,
		LocalDateTime fechaRegistro) {
}
