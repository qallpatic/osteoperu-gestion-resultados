package pe.com.qallpatic.osteoperu.admision.dto;

import java.time.LocalDateTime;

/**
 * Fila del listado de pacientes en Admision: cada paciente con resultados y el
 * estado de su cuenta de acceso.
 * estadoCuenta = SIN_CUENTA | ACTIVO | ANULADO.
 */
public record PacienteAdmisionResponse(
		String dni,
		String nombre,
		String telefono,
		long totalResultados,
		LocalDateTime ultimaFecha,
		String estadoCuenta,
		String usuarioCreacion,
		LocalDateTime fechaCreacionCuenta,
		String usuarioModificacion,
		LocalDateTime fechaModificacionCuenta) {
}
