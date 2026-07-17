package pe.com.qallpatic.osteoperu.seguridad.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.jwt.ProveedorJwt;
import pe.com.qallpatic.osteoperu.seguridad.jwt.SeguridadProperties;
import pe.com.qallpatic.osteoperu.seguridad.repository.TokenRefrescoRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
	private SeguridadProperties propiedades;
	@Mock
	private TasaLimitLoginService tasaLimitLogin;
	@InjectMocks
	private ServicioAutenticacionImpl servicio;

	@Test
	void registraElIntentoSoloCuandoFallaLaAutenticacion() {
		LoginRequest solicitud = new LoginRequest(" usuario-a ", "incorrecta");
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Credenciales invalidas"));

		assertThrows(BadCredentialsException.class,
				() -> servicio.login(solicitud, "203.0.113.25"));

		verify(tasaLimitLogin).evaluar("203.0.113.25", "usuario-a");
		verify(tasaLimitLogin).registrarFallo("203.0.113.25", "usuario-a");
	}
}
