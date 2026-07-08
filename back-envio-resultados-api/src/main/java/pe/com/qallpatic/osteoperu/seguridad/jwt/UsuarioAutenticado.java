package pe.com.qallpatic.osteoperu.seguridad.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Principal autenticado (adaptador de {@link Usuario} a {@link UserDetails}).
 * Las autoridades se exponen como {@code ROLE_<NOMBRE>} para el control por roles.
 */
@Getter
public class UsuarioAutenticado implements UserDetails {

	private final Long id;
	private final String usuario;
	private final String contrasena;
	private final String nombreCompleto;
	private final boolean activo;
	private final Set<String> roles;

	public UsuarioAutenticado(Usuario u) {
		this.id = u.getId();
		this.usuario = u.getUsuario();
		this.contrasena = u.getContrasena();
		this.nombreCompleto = ((u.getNombre() == null ? "" : u.getNombre()) + " "
				+ (u.getApellido() == null ? "" : u.getApellido())).trim();
		this.activo = u.isEstado();
		this.roles = u.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
				.map(nombre -> new SimpleGrantedAuthority("ROLE_" + nombre))
				.collect(Collectors.toList());
	}

	/** Construye las autoridades a partir de una lista de nombres de rol (sin prefijo). */
	public static List<SimpleGrantedAuthority> autoridadesDesdeRoles(Collection<String> roles) {
		return roles.stream()
				.map(nombre -> new SimpleGrantedAuthority("ROLE_" + nombre))
				.collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return contrasena;
	}

	@Override
	public String getUsername() {
		return usuario;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return activo;
	}
}
