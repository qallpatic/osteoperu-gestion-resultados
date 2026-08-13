package pe.com.qallpatic.osteoperu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Parametros de Google reCAPTCHA v3 bajo el prefijo {@code recaptcha.*}.
 * <p>
 * Si {@code enabled=false} (default) no se carga el script en el front ni se
 * valida el token en el back; el endpoint de login sigue funcionando como
 * antes. Las variables de entorno que los sobreescriben se documentan en
 * {@code .env.example} (RECAPTCHA_*).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "recaptcha")
public class RecaptchaProperties {

	/** Habilita la verificacion de reCAPTCHA v3 en POST /api/auth/login. */
	private boolean enabled = false;

	/** Site key publica de Google (se expone al front via /api/auth/captcha-config). */
	private String siteKey;

	/** Secret key privada; NUNCA se loggea ni se devuelve al front. */
	private String secretKey;

	/** URL del endpoint siteverify de Google. No suele cambiar. */
	private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";

	/**
	 * Puntaje minimo aceptado (0.0 = bot, 1.0 = humano). Google recomienda
	 * 0.5 como umbral razonable; subir si genera falsos positivos, bajar si
	 * pasan bots.
	 */
	private double minScore = 0.5;

	/** Timeout de la llamada siteverify, en milisegundos. */
	private long timeoutMs = 3000;
}
