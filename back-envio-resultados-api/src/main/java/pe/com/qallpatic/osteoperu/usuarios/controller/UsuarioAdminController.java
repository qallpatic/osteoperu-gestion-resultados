package pe.com.qallpatic.osteoperu.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.usuarios.dto.AsignarRolesRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.CambiarEstadoRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.CambiarPasswordRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.RolResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioUpdateRequest;
import pe.com.qallpatic.osteoperu.usuarios.service.UsuarioService;

import java.util.List;

/** Administracion de usuarios y roles. Todas las rutas exigen rol ADMIN. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administracion de usuarios", description = "ABM de usuarios, roles, estado y contrasena (solo ADMIN)")
public class UsuarioAdminController {

	private final UsuarioService usuarioService;

	@Operation(summary = "Listar usuarios", description = "Paginado con busqueda y filtros por rol y estado.")
	@GetMapping("/usuarios")
	public ResponseEntity<RespuestaApi<PaginaResponse<UsuarioResponse>>> listar(
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "10") int tamano,
			@RequestParam(required = false) String buscar,
			@RequestParam(required = false) String rol,
			@RequestParam(required = false) Boolean estado,
			@RequestParam(defaultValue = "id") String ordenarPor,
			@RequestParam(defaultValue = "asc") String direccion) {
		PaginaResponse<UsuarioResponse> pagina2 =
				usuarioService.listar(buscar, rol, estado, pagina, tamano, ordenarPor, direccion);
		return ResponseEntity.ok(RespuestaApi.ok(pagina2));
	}

	@Operation(summary = "Obtener usuario por id")
	@GetMapping("/usuarios/{id}")
	public ResponseEntity<RespuestaApi<UsuarioResponse>> obtener(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioService.obtener(id)));
	}

	@Operation(summary = "Crear usuario")
	@PostMapping("/usuarios")
	public ResponseEntity<RespuestaApi<UsuarioResponse>> crear(
			@Valid @RequestBody UsuarioRequest solicitud, Authentication auth) {
		UsuarioResponse creado = usuarioService.crear(solicitud, actor(auth));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaApi.ok("Usuario creado correctamente.", creado));
	}

	@Operation(summary = "Actualizar datos del usuario")
	@PutMapping("/usuarios/{id}")
	public ResponseEntity<RespuestaApi<UsuarioResponse>> actualizar(
			@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest solicitud, Authentication auth) {
		UsuarioResponse actualizado = usuarioService.actualizar(id, solicitud, actor(auth));
		return ResponseEntity.ok(RespuestaApi.ok("Usuario actualizado correctamente.", actualizado));
	}

	@Operation(summary = "Eliminar usuario")
	@DeleteMapping("/usuarios/{id}")
	public ResponseEntity<RespuestaApi<Void>> eliminar(@PathVariable Long id, Authentication auth) {
		usuarioService.eliminar(id, actor(auth));
		return ResponseEntity.ok(RespuestaApi.mensaje("Usuario eliminado correctamente."));
	}

	@Operation(summary = "Activar / desactivar usuario")
	@PatchMapping("/usuarios/{id}/estado")
	public ResponseEntity<RespuestaApi<UsuarioResponse>> cambiarEstado(
			@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest solicitud, Authentication auth) {
		UsuarioResponse actualizado = usuarioService.cambiarEstado(id, solicitud.activo(), actor(auth));
		return ResponseEntity.ok(RespuestaApi.ok("Estado actualizado.", actualizado));
	}

	@Operation(summary = "Asignar roles (reemplaza los actuales)")
	@PatchMapping("/usuarios/{id}/roles")
	public ResponseEntity<RespuestaApi<UsuarioResponse>> asignarRoles(
			@PathVariable Long id, @Valid @RequestBody AsignarRolesRequest solicitud, Authentication auth) {
		UsuarioResponse actualizado = usuarioService.asignarRoles(id, solicitud.rolesIds(), actor(auth));
		return ResponseEntity.ok(RespuestaApi.ok("Roles actualizados.", actualizado));
	}

	@Operation(summary = "Cambiar contrasena del usuario")
	@PatchMapping("/usuarios/{id}/password")
	public ResponseEntity<RespuestaApi<Void>> cambiarPassword(
			@PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest solicitud, Authentication auth) {
		usuarioService.cambiarPassword(id, solicitud.password(), actor(auth));
		return ResponseEntity.ok(RespuestaApi.mensaje("Contrasena actualizada correctamente."));
	}

	@Operation(summary = "Listar roles disponibles")
	@GetMapping("/roles")
	public ResponseEntity<RespuestaApi<List<RolResponse>>> listarRoles() {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioService.listarRoles()));
	}

	private String actor(Authentication auth) {
		return auth != null ? auth.getName() : "SISTEMA";
	}
}
