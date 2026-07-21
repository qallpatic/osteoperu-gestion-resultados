package pe.com.qallpatic.osteoperu.seguridad.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.qallpatic.osteoperu.compartido.excepcion.LimiteIntentosSuperadoException;
import pe.com.qallpatic.osteoperu.config.LoginProperties;
import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoLogin;
import pe.com.qallpatic.osteoperu.seguridad.repository.IntentoLoginRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TasaLimitLoginServiceTest {

	@Mock
	private IntentoLoginRepository intentoRepository;

	private TasaLimitLoginService servicio;

	@BeforeEach
	void setUp() {
		LoginProperties propiedades = new LoginProperties();
		propiedades.setMaxIntentosIp(5);
		propiedades.setMaxIntentosUsuario(5);
		propiedades.setVentanaMinutos(10);
		servicio = new TasaLimitLoginService(intentoRepository, propiedades);
	}

	@Test
	void permiteOtroUsuarioAunqueCompartaIp() {
		when(intentoRepository.contarFallidosPorIpYUsuario(
				eq("203.0.113.25"), eq("usuario-b"), any(LocalDateTime.class))).thenReturn(0L);
		when(intentoRepository.contarFallidosPorUsuario(
				eq("usuario-b"), any(LocalDateTime.class))).thenReturn(0L);

		assertDoesNotThrow(() -> servicio.evaluar("203.0.113.25", "usuario-b"));
		verify(intentoRepository, never()).save(any(IntentoLogin.class));
	}

	@Test
	void bloqueaSoloLaDuplaIpUsuarioAlLlegarAlLimite() {
		when(intentoRepository.contarFallidosPorIpYUsuario(
				eq("203.0.113.25"), eq("usuario-a"), any(LocalDateTime.class))).thenReturn(5L);

		LimiteIntentosSuperadoException error = assertThrows(
				LimiteIntentosSuperadoException.class,
				() -> servicio.evaluar("203.0.113.25", "usuario-a"));

		assertTrue(error.getMessage().contains("este usuario"));
		verify(intentoRepository, never()).contarFallidosPorUsuario(any(String.class), any(LocalDateTime.class));
		ArgumentCaptor<IntentoLogin> captor = ArgumentCaptor.forClass(IntentoLogin.class);
		verify(intentoRepository).save(captor.capture());
		assertTrue(captor.getValue().isBloqueado());
		assertEquals("203.0.113.25", captor.getValue().getIpAddress());
		assertEquals("usuario-a", captor.getValue().getUsuario());
	}

	@Test
	void registraSoloFallosDeAutenticacionComoIntentosContables() {
		servicio.registrarFallo("203.0.113.25", " usuario-a ");

		ArgumentCaptor<IntentoLogin> captor = ArgumentCaptor.forClass(IntentoLogin.class);
		verify(intentoRepository).save(captor.capture());
		assertFalse(captor.getValue().isBloqueado());
		assertEquals("203.0.113.25", captor.getValue().getIpAddress());
		assertEquals("usuario-a", captor.getValue().getUsuario());
	}
}
