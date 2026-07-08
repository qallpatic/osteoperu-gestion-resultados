package pe.com.qallpatic.osteoperu.seguridad.dto;

import lombok.Builder;

import java.util.List;

/** Respuesta de autenticacion: tokens y datos basicos de la sesion. */
@Builder
public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		String usuario,
		String nombre,
		List<String> roles) {
}
