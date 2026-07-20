package pe.com.qallpatic.osteoperu.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de inicio de sesion.
 *
 * @param recaptchaToken Token emitido por {@code grecaptcha.execute} cuando
 *                        {@code recaptcha.enabled=true} en el back; opcional
 *                        si el CAPTCHA esta deshabilitado por configuracion.
 *                        NUNCA se loggea.
 */
public record LoginRequest(
		@NotBlank(message = "El usuario es obligatorio.") String usuario,
		@NotBlank(message = "La contrasena es obligatoria.") String contrasena,
		String recaptchaToken) {
}
