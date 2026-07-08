package pe.com.qallpatic.osteoperu.usuarios.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Roles a asignar a un usuario (reemplaza los actuales). */
public record AsignarRolesRequest(
		@NotEmpty(message = "Debe asignar al menos un rol.") List<Long> rolesIds) {
}
