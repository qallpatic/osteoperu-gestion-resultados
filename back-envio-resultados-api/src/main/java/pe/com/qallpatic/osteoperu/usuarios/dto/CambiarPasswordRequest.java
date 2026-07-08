package pe.com.qallpatic.osteoperu.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nueva contrasena para un usuario. */
public record CambiarPasswordRequest(
		@NotBlank(message = "La contrasena es obligatoria.")
		@Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres.")
		String password) {
}
