package pe.com.qallpatic.osteoperu.integracion.captcha;

import pe.com.qallpatic.osteoperu.compartido.excepcion.ExcepcionCaptchaInvalidoException;

/**
 * Puerto de verificacion de tokens de Google reCAPTCHA v3. La implementacion
 * activa se selecciona por configuracion ({@code recaptcha.enabled}).
 * <p>
 * Cuando el CAPTCHA esta deshabilitado, la implementacion cae en un no-op que
 * aprueba siempre (mantiene compatibilidad con entornos donde el frontend
 * no envia el token).
 */
public interface VerificadorCaptcha {

	/**
	 * Verifica el token obtenido por el front con Google siteverify.
	 *
	 * @param token    valor emitido por {@code grecaptcha.execute(...)}.
	 * @param ipRemota IP del cliente que envio el login (se envia a Google
	 *                 como pista opcional pero no es decisivo).
	 * @throws ExcepcionCaptchaInvalidoException si el token es nulo/vacio,
	 *         si Google responde error, o si score &lt; umbral configurado.
	 */
	void verificar(String token, String ipRemota);
}
