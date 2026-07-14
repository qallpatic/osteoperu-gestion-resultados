package pe.com.qallpatic.osteoperu.usuarios.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioResponse;

import java.util.List;
import java.util.Set;

/** Mapea entidades de identidad a los DTO de respuesta del panel de administracion. */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

	@Mapping(source = "nombres", target = "nombres")
	@Mapping(source = "apellidoPaterno", target = "apellidoPaterno")
	@Mapping(source = "apellidoMaterno", target = "apellidoMaterno")
	UsuarioResponse toResponse(Usuario usuario);

	/** Usado por MapStruct para el campo {@code roles}: nombres ordenados. */
	default List<String> mapRoles(Set<Rol> roles) {
		return roles == null ? List.of()
				: roles.stream().map(Rol::getNombre).sorted().toList();
	}
}