package pe.com.qallpatic.osteoperu.usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Vista de un usuario para el panel de administracion. */
public record UsuarioResponse(
		Long id,
		String usuario,
		String nombre,
		String apellido,
		String correo,
		String dni,
		boolean estado,
		List<String> roles,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaModificacion) {
}
