package pe.com.qallpatic.osteoperu.usuarios.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del panel de administracion de usuarios.
 * {@code nombre} es el string canonico "APELLIDO_P APELLIDO_M, NOMBRES"
 * (lo que se muestra en el front y se imprime); los segmentos
 * {@code nombres}/{@code apellidoPaterno}/{@code apellidoMaterno} estan
 * disponibles para ordenamiento o busqueda por apellido en futuras
 * pantallas.
 */
public record UsuarioResponse(
		Long id,
		String usuario,
		String nombre,
		String nombres,
		String apellidoPaterno,
		String apellidoMaterno,
		String correo,
		String dni,
		boolean estado,
		List<String> roles,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaModificacion) {
}