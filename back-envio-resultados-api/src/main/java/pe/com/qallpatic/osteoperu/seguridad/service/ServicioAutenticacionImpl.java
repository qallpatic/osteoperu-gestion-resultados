package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.integracion.captcha.VerificadorCaptcha;
import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.TokenResponse;
import pe.com.qallpatic.osteoperu.seguridad.entity.TokenRefresco;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.jwt.ProveedorJwt;
import pe.com.qallpatic.osteoperu.seguridad.jwt.SeguridadProperties;
import pe.com.qallpatic.osteoperu.seguridad.jwt.UsuarioAutenticado;
import pe.com.qallpatic.osteoperu.seguridad.repository.TokenRefrescoRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioAutenticacionImpl implements ServicioAutenticacion {

	/** Caracteres permitidos en cualquier tipo de username (operario interno o paciente).
 *  El back es laxo con el formato porque la BD distingue el rol del usuario
 *  (RESULTADOS/ADMISION/CALLCENTER son username corto; PACIENTE usa DNI/CE).
 *  Solo se filtran entradas claramente basura (correos, espacios, símbolos). */
	private static final java.util.regex.Pattern USERNAME_LIMPIO = java.util.regex.Pattern.compile("^[A-Za-z0-9]{3,30}$");

	private final AuthenticationManager authenticationManager;
	private final ProveedorJwt proveedorJwt;
	private final TokenRefrescoRepository tokenRefrescoRepository;
	private final UsuarioRepository usuarioRepository;
	private final SeguridadProperties propiedades;
	private final TasaLimitLoginService tasaLimitLogin;
	private final VerificadorCaptcha verificadorCaptcha;

	@Override
	@Transactional
	public TokenResponse login(LoginRequest solicitud, String ip) {
		String usuario = solicitud.usuario() == null ? "" : solicitud.usuario().trim();

		// El operador interno (ADMIN) se loguea con su username no-DNI; el resto
		// de usuarios son pacientes y deben tipiar su número de DNI.
		String adminUsername = propiedades.getAdmin().getUsername();
		if (adminUsername == null || !adminUsername.equalsIgnoreCase(usuario)) {
			if (!USERNAME_LIMPIO.matcher(usuario).matches()) {
				throw new SolicitudInvalidaException(
						"El usuario debe ser alfanumerico, sin espacios ni simbolos (ejemplo: 12345678, E1234567, resultados).");
			}
		}

		// CAPTCHA antes de rate-limit y autenticacion: un bot no debe ni
		// consumir cupo de la ventana ni aprender si el usuario existe.
		// El impl no-op se activa cuando recaptcha.enabled=false (compat).
		verificadorCaptcha.verificar(solicitud.recaptchaToken(), ip);

		// Rate-limit ANTES de delegar a Spring Security. Si la dupla
		// ip+usuario esta bloqueada, devolvemos 429 sin enumerar usuarios.
		tasaLimitLogin.evaluar(ip, usuario);

		Authentication autenticacion;
		try {
			autenticacion = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(usuario, solicitud.contrasena()));
		} catch (AuthenticationException e) {
			tasaLimitLogin.registrarFallo(ip, usuario);
			log.info("Login fallido. usuario={}, ip={}", usuario, ip);
			throw e;
		}

		UsuarioAutenticado principal = (UsuarioAutenticado) autenticacion.getPrincipal();
		Usuario user = usuarioRepository.findByUsuario(principal.getUsername())
				.orElseThrow(() -> new SolicitudInvalidaException("Usuario no encontrado."));

		log.info("Login exitoso. usuario={}, ip={}, roles={}",
				principal.getUsername(), ip, principal.getRoles());
		return construirRespuesta(principal, user);
	}

	@Override
	@Transactional
	public TokenResponse refrescar(String refreshToken) {
		TokenRefresco token = tokenRefrescoRepository.findByToken(refreshToken)
				.orElseThrow(() -> new SolicitudInvalidaException("Refresh token invalido."));

		if (!token.esValido()) {
			throw new SolicitudInvalidaException("Refresh token expirado o revocado.");
		}

		// Rotacion: se revoca el refresh usado y se emite uno nuevo.
		token.setRevocado(true);
		tokenRefrescoRepository.save(token);

		Usuario usuario = token.getUsuario();
		UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
		log.info("Refresh de token. usuario={}", usuario.getUsuario());
		return construirRespuesta(principal, usuario);
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		tokenRefrescoRepository.findByToken(refreshToken).ifPresent(token -> {
			token.setRevocado(true);
			tokenRefrescoRepository.save(token);
			log.info("Logout. usuario={}", token.getUsuario().getUsuario());
		});
	}

	private TokenResponse construirRespuesta(UsuarioAutenticado principal, Usuario usuario) {
		String accessToken = proveedorJwt.generarAccessToken(principal);
		String refreshToken = crearRefreshToken(usuario);
		List<String> roles = principal.getRoles().stream().sorted().toList();

		return TokenResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.tokenType("Bearer")
				.expiresIn(proveedorJwt.getAccessExpirationSeconds())
				.usuario(principal.getUsername())
				.nombre(principal.getNombreCompleto())
				.roles(roles)
				.build();
	}

	private String crearRefreshToken(Usuario usuario) {
		LocalDateTime ahora = LocalDateTime.now();
		TokenRefresco token = TokenRefresco.builder()
				.usuario(usuario)
				.token(GeneradorCodigos.generar(48))
				.fechaExpiracion(ahora.plusSeconds(propiedades.getJwt().getRefreshExpirationMs() / 1000))
				.revocado(false)
				.fechaCreacion(ahora)
				.build();
		return tokenRefrescoRepository.save(token).getToken();
	}
}
