package pe.com.qallpatic.osteoperu.integracion.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementacion no-op. Se activa cuando {@code recaptcha.enabled=false} o la
 * propiedad no esta seteada. Aprueba siempre, manteniendo compat con flujos
 * donde el front todavia no envia el token (ej. desarrollo, transicion).
 */
@Component("verificadorCaptchaDeshabilitado")
@ConditionalOnProperty(prefix = "recaptcha", name = "enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnMissingBean(name = "verificadorCaptchaHttp")
public class VerificadorCaptchaDeshabilitado implements VerificadorCaptcha {

	@Override
	public void verificar(String token, String ipRemota) {
		// No hace nada: el CAPTCHA esta apagado por configuracion.
	}
}
