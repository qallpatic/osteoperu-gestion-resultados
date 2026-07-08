package pe.com.qallpatic.osteoperu.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitud de renovacion del access token mediante el refresh token. */
public record RefreshRequest(
		@NotBlank(message = "El refresh token es obligatorio.") String refreshToken) {
}
