package pe.com.qallpatic.osteoperu.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Datos para actualizar un usuario.
 * El nombre llega como string unico "APELLIDO_P APELLIDO_M, NOMBRES" y
 * el backend lo segmenta antes de persistir.
 */
public record UsuarioUpdateRequest(
		@Size(max = 250) String nombre,

		@Email(message = "El correo no es valido.")
		@Size(max = 150) String correo,

		@Size(max = 15) String dni) {
}