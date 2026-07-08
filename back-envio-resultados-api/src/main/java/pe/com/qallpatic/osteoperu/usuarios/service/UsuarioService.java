package pe.com.qallpatic.osteoperu.usuarios.service;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.RolResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioUpdateRequest;

import java.util.List;

/** Casos de uso de administracion de usuarios (rol ADMIN). */
public interface UsuarioService {

	PaginaResponse<UsuarioResponse> listar(String buscar, String rol, Boolean estado,
			int pagina, int tamano, String ordenarPor, String direccion);

	UsuarioResponse obtener(Long id);

	UsuarioResponse crear(UsuarioRequest solicitud, String actor);

	UsuarioResponse actualizar(Long id, UsuarioUpdateRequest solicitud, String actor);

	void eliminar(Long id, String actor);

	UsuarioResponse cambiarEstado(Long id, boolean activo, String actor);

	UsuarioResponse asignarRoles(Long id, List<Long> rolesIds, String actor);

	void cambiarPassword(Long id, String nuevaPassword, String actor);

	List<RolResponse> listarRoles();
}
