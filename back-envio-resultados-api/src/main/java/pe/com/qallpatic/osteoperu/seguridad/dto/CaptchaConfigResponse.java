package pe.com.qallpatic.osteoperu.seguridad.dto;

/**
 * Configuracion publica de reCAPTCHA expuesta al frontend. El {@code siteKey}
 * es publico por diseno (es el equivalente al "client id" de OAuth). La
 * secret key NUNCA sale del backend.
 *
 * @param enabled  si esta activo el CAPTCHA el front debe cargar el script y
 *                 enviar {@code recaptchaToken} en cada login.
 * @param siteKey  clave publica de Google reCAPTCHA v3; {@code null} cuando
 *                 {@code enabled=false}.
 */
public record CaptchaConfigResponse(boolean enabled, String siteKey) {
}
