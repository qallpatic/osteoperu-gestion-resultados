package pe.com.qallpatic.osteoperu.seguridad.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.jwt.SeguridadProperties;
import pe.com.qallpatic.osteoperu.seguridad.repository.RolRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

/**
 * Crea el usuario ADMIN inicial al arrancar si aun no existe (idempotente).
 * Los roles ya vienen sembrados por la migracion Flyway.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InicializadorDatos implements ApplicationRunner {

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;
	private final SeguridadProperties propiedades;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String username = propiedades.getAdmin().getUsername();

		if (usuarioRepository.existsByUsuario(username)) {
			log.info("Usuario ADMIN '{}' ya existe. No se crea de nuevo.", username);
			return;
		}

		Rol rolAdmin = rolRepository.findByNombre("ADMIN")
				.orElseThrow(() -> new IllegalStateException(
						"No existe el rol ADMIN. Verifique la semilla de la migracion Flyway."));

		Usuario admin = Usuario.builder()
				.usuario(username)
				.nombre(propiedades.getAdmin().getNombre())
				.contrasena(passwordEncoder.encode(propiedades.getAdmin().getPassword()))
				.estado(true)
				.build();
		admin.agregarRol(rolAdmin);
		admin.setUsuarioCreacion("SISTEMA");

		usuarioRepository.save(admin);
		log.warn("Usuario ADMIN '{}' creado con la clave por defecto. CAMBIELA en produccion.", username);
	}
}
