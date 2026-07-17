package pe.com.qallpatic.osteoperu.seguridad.service;

import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.TokenResponse;

/** Casos de uso de autenticacion. */
public interface ServicioAutenticacion {

	/**
	 * Login con rate-limit por IP y por usuario. Si la dupla ip+usuario esta
	 * bloqueada, lanza 429 antes de invocar el AuthenticationManager.
	 *
	 * @param solicitud credenciales.
	 * @param ip IP del cliente (X-Forwarded-For o remoteAddr).
	 */
	TokenResponse login(LoginRequest solicitud, String ip);

	TokenResponse refrescar(String refreshToken);

	void logout(String refreshToken);
}
