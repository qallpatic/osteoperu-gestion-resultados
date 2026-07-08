package pe.com.qallpatic.osteoperu.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Datos para crear un usuario. */
public record UsuarioRequest(
		@NotBlank(message = "El usuario es obligatorio.")
		@Size(max = 100) String usuario,

		@Size(max = 120) String nombre,
		@Size(max = 120) String apellido,

		@Email(message = "El correo no es valido.")
		@Size(max = 150) String correo,

		@Size(max = 15) String dni,

		@NotBlank(message = "La contrasena es obligatoria.")
		@Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres.")
		String password,

		@NotEmpty(message = "Debe asignar al menos un rol.")
		List<Long> rolesIds) {
}
