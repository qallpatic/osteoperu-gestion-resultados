package pe.com.qallpatic.osteoperu.seguridad.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.jwt.ProveedorJwt;
import pe.com.qallpatic.osteoperu.seguridad.jwt.SeguridadProperties;
import pe.com.qallpatic.osteoperu.seguridad.repository.TokenRefrescoRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionImplTest {

	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private ProveedorJwt proveedorJwt;
	@Mock
	private TokenRefrescoRepository tokenRefrescoRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private TasaLimitLoginService tasaLimitLogin;

	private ServicioAutenticacionImpl servicio;

	@BeforeEach
	void setUp() {
		SeguridadProperties propiedades = new SeguridadProperties();
		servicio = new ServicioAutenticacionImpl(
				authenticationManager,
				proveedorJwt,
				tokenRefrescoRepository,
				usuarioRepository,
				propiedades,
				tasaLimitLogin);
	}

	@Test
	void registraElIntentoSoloCuandoFallaLaAutenticacion() {
		LoginRequest solicitud = new LoginRequest("12345678", "incorrecta");
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(tasaLimitLogin).evaluar("203.0.113.25", "12345678");
		verify(tasaLimitLogin).registrarFallo("203.0.113.25", "12345678");
	}

	@Test
	void rechazaEmailAntesDeAutenticar() {
		LoginRequest solicitud = new LoginRequest("alejandrogallegos8483@gmail.com", "x");

		SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		assertTrue(error.getMessage().contains("alfanumerico"));
		verify(authenticationManager, never()).authenticate(any());
		verify(tasaLimitLogin, never()).evaluar(any(), any());
		verify(tasaLimitLogin, never()).registrarFallo(any(), any());
	}

	@Test
	void permiteLoginDelAdminConUsernameNoDni() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		LoginRequest solicitud = new LoginRequest("admin", "incorrecta");

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager).authenticate(any());
	}

	@Test
	void permiteLoginDeOperarioInternoConUsernameCorto() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		LoginRequest solicitud = new LoginRequest("resultados", "incorrecta");

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager).authenticate(any());
		verify(tasaLimitLogin).evaluar("203.0.113.25", "resultados");
	}

	@Test
	void permiteLoginDePacienteConDni() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		LoginRequest solicitud = new LoginRequest("12345678", "incorrecta");

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager).authenticate(any());
	}

	@Test
	void permiteLoginDePacienteConCe() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		LoginRequest solicitud = new LoginRequest("E1234567", "incorrecta");

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager).authenticate(any());
	}

	@Test
	void rechazaUsernameConArrobaTipicoEmail() {
		LoginRequest solicitud = new LoginRequest("alejandrogallegos8483@gmail.com", "x");

		SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		assertTrue(error.getMessage().contains("alfanumerico"));
		verify(authenticationManager, never()).authenticate(any());
		verify(tasaLimitLogin, never()).evaluar(any(), any());
	}

	@Test
	void rechazaUsernameConEspacios() {
		LoginRequest solicitud = new LoginRequest("juan perez", "x");

		assertThrows(SolicitudInvalidaException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager, never()).authenticate(any());
	}

	@Test
	void rechazaUsernameConSimbolos() {
		LoginRequest solicitud = new LoginRequest("12345678-x", "x");

		assertThrows(SolicitudInvalidaException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(authenticationManager, never()).authenticate(any());
	}
}
