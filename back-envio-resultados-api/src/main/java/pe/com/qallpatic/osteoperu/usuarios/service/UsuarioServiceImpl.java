package pe.com.qallpatic.osteoperu.usuarios.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.util.NombreParser;
import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.repository.RolRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;
import pe.com.qallpatic.osteoperu.usuarios.dto.RolResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioRequest;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioResponse;
import pe.com.qallpatic.osteoperu.usuarios.dto.UsuarioUpdateRequest;
import pe.com.qallpatic.osteoperu.usuarios.exception.UsuarioYaExisteException;
import pe.com.qallpatic.osteoperu.usuarios.mapper.UsuarioMapper;
import pe.com.qallpatic.osteoperu.usuarios.spec.UsuarioSpecs;

import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;
	private final UsuarioMapper usuarioMapper;

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<UsuarioResponse> listar(String buscar, String rol, Boolean estado,
			int pagina, int tamano, String ordenarPor, String direccion) {
		Sort.Direction dir = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
		String campo = (ordenarPor == null || ordenarPor.isBlank()) ? "id" : ordenarPor;
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano, Sort.by(dir, campo));

		Page<UsuarioResponse> page = usuarioRepository
				.findAll(UsuarioSpecs.filtro(buscar, rol, estado), pageable)
				.map(usuarioMapper::toResponse);
		return PaginaResponse.de(page);
	}

	@Override
	@Transactional(readOnly = true)
	public UsuarioResponse obtener(Long id) {
		return usuarioMapper.toResponse(buscar(id));
	}

	@Override
	public UsuarioResponse crear(UsuarioRequest solicitud, String actor) {
		if (usuarioRepository.existsByUsuario(solicitud.usuario())) {
			throw new UsuarioYaExisteException("Ya existe un usuario con el login '" + solicitud.usuario() + "'.");
		}
		if (tieneDni(solicitud.dni()) && usuarioRepository.existsByDni(solicitud.dni())) {
			throw new UsuarioYaExisteException("Ya existe un usuario con el DNI '" + solicitud.dni() + "'.");
		}

		Usuario usuario = Usuario.builder()
				.usuario(solicitud.usuario().trim())
				.nombre(solicitud.nombre())
				.correo(solicitud.correo())
				.dni(solicitud.dni())
				.contrasena(passwordEncoder.encode(solicitud.password()))
				.estado(true)
				.roles(new HashSet<>(resolverRoles(solicitud.rolesIds())))
				.build();
		aplicarNombre(usuario, solicitud.nombre());
		usuario.setUsuarioCreacion(actor);

		Usuario guardado = usuarioRepository.save(usuario);
		log.info("Usuario creado id={}, usuario={}, por={}", guardado.getId(), guardado.getUsuario(), actor);
		return usuarioMapper.toResponse(guardado);
	}

	@Override
	public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest solicitud, String actor) {
		Usuario usuario = buscar(id);

		if (tieneDni(solicitud.dni()) && !solicitud.dni().equals(usuario.getDni())
				&& usuarioRepository.existsByDni(solicitud.dni())) {
			throw new UsuarioYaExisteException("Ya existe un usuario con el DNI '" + solicitud.dni() + "'.");
		}

		aplicarNombre(usuario, solicitud.nombre());
		usuario.setCorreo(solicitud.correo());
		usuario.setDni(solicitud.dni());
		usuario.setUsuarioModificacion(actor);

		return usuarioMapper.toResponse(usuarioRepository.save(usuario));
	}

	/** Aplica el nombre canonico al usuario, segmentando en las 3 columnas. */
	private void aplicarNombre(Usuario usuario, String nombreCompleto) {
		if (nombreCompleto == null) {
			return;
		}
		NombreParser np = NombreParser.parsear(nombreCompleto);
		if (np.esVacio()) {
			return;
		}
		usuario.setNombre(np.completo());
		usuario.setNombres(np.nombres());
		usuario.setApellidoPaterno(np.apellidoPaterno());
		usuario.setApellidoMaterno(np.apellidoMaterno());
	}

	@Override
	public void eliminar(Long id, String actor) {
		Usuario usuario = buscar(id);
		if (usuario.getUsuario().equals(actor)) {
			throw new SolicitudInvalidaException("No puede eliminar su propia cuenta.");
		}
		usuarioRepository.delete(usuario);
		log.info("Usuario eliminado id={}, por={}", id, actor);
	}

	@Override
	public UsuarioResponse cambiarEstado(Long id, boolean activo, String actor) {
		Usuario usuario = buscar(id);
		if (!activo && usuario.getUsuario().equals(actor)) {
			throw new SolicitudInvalidaException("No puede desactivar su propia cuenta.");
		}
		usuario.setEstado(activo);
		usuario.setUsuarioModificacion(actor);
		return usuarioMapper.toResponse(usuarioRepository.save(usuario));
	}

	@Override
	public UsuarioResponse asignarRoles(Long id, List<Long> rolesIds, String actor) {
		Usuario usuario = buscar(id);
		usuario.setRoles(new HashSet<>(resolverRoles(rolesIds)));
		usuario.setUsuarioModificacion(actor);
		return usuarioMapper.toResponse(usuarioRepository.save(usuario));
	}

	@Override
	public void cambiarPassword(Long id, String nuevaPassword, String actor) {
		Usuario usuario = buscar(id);
		usuario.setContrasena(passwordEncoder.encode(nuevaPassword));
		usuario.setUsuarioModificacion(actor);
		usuarioRepository.save(usuario);
		log.info("Contrasena actualizada. usuario id={}, por={}", id, actor);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RolResponse> listarRoles() {
		return rolRepository.findAll().stream()
				.map(r -> new RolResponse(r.getId(), r.getNombre(), r.getDescripcion()))
				.toList();
	}

	private Usuario buscar(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
	}

	private List<Rol> resolverRoles(List<Long> ids) {
		List<Rol> roles = rolRepository.findAllById(ids);
		if (roles.size() != new HashSet<>(ids).size()) {
			throw new SolicitudInvalidaException("Uno o mas roles indicados no existen.");
		}
		return roles;
	}

	private boolean tieneDni(String dni) {
		return dni != null && !dni.isBlank();
	}
}
