package pe.com.qallpatic.osteoperu.admision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para generar la cuenta y credencial del paciente. */
public record GenerarUsuarioRequest(
		@NotBlank @Size(max = 15) String dni,
		@Size(max = 120) String nombre,
		@Size(max = 20) String telefono) {
}
