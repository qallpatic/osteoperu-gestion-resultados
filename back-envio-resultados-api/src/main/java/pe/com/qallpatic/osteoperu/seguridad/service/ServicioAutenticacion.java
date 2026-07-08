package pe.com.qallpatic.osteoperu.seguridad.service;

import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.TokenResponse;

/** Casos de uso de autenticacion. */
public interface ServicioAutenticacion {

	TokenResponse login(LoginRequest solicitud);

	TokenResponse refrescar(String refreshToken);

	void logout(String refreshToken);
}
