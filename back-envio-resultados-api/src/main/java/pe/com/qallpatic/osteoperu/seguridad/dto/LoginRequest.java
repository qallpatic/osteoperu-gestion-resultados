package pe.com.qallpatic.osteoperu.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales de inicio de sesion. */
public record LoginRequest(
		@NotBlank(message = "El usuario es obligatorio.") String usuario,
		@NotBlank(message = "La contrasena es obligatoria.") String contrasena) {
}
