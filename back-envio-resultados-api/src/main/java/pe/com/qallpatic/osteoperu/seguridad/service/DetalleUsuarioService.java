package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.jwt.UsuarioAutenticado;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

/** Carga el usuario y sus roles desde la base de datos para Spring Security. */
@Service
@RequiredArgsConstructor
public class DetalleUsuarioService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		Usuario usuario = usuarioRepository.findByUsuario(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
		return new UsuarioAutenticado(usuario);
	}
}
