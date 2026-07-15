package pe.com.qallpatic.osteoperu.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud publica al endpoint de recuperacion de password. El telefono se
 * valida y normaliza en el backend con {@code UtilTelefono.normalizar}; aqui
 * solo exigimos el formato basico (9 digitos, Peru) para responder rapido
 * con 400 antes de tocar el rate-limit.
 *
 * @param telefono 9 digitos empezando en 9.
 */
public record RecuperarPasswordRequest(
		@NotBlank(message = "El telefono es obligatorio.")
		@Pattern(regexp = "^9\\d{8}$", message = "Telefono invalido. Debe tener 9 digitos y empezar en 9.")
		String telefono) {
}
