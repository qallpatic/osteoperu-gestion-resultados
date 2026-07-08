package pe.com.qallpatic.osteoperu.resultados.dto;

import jakarta.validation.constraints.Size;

/** Actualizacion de un resultado: datos basicos y reenvio opcional del SMS. */
public record ResultadoUpdateRequest(
		@Size(max = 20) String telefono,
		@Size(max = 150) String nombrePaciente,
		boolean reenviarSms) {
}
