package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fila plana para el reporte Excel de "Usuarios registrados".
 * El service arma la lista desde la entidad {@code Usuario} (con roles
 * eager-loaded via EntityGraph) y mapea a este record.
 */
public record ReporteUsuarioRow(

		LocalDateTime fechaCreacion,
		String usuario,
		String nombreCompleto,
		String dni,
		String correo,
		List<String> roles,
		Boolean estado,
		LocalDateTime fechaModificacion,
		String usuarioCreacion,
		String usuarioModificacion) {
}
