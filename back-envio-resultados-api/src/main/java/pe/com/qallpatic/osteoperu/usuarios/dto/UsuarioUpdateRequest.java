package pe.com.qallpatic.osteoperu.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Datos editables de un usuario (no incluye login, contrasena ni roles). */
public record UsuarioUpdateRequest(
		@Size(max = 120) String nombre,
		@Size(max = 120) String apellido,
		@Email(message = "El correo no es valido.") @Size(max = 150) String correo,
		@Size(max = 15) String dni) {
}
