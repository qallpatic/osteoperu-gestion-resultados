package pe.com.qallpatic.osteoperu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del rate-limit del endpoint publico /api/auth/login bajo el
 * prefijo {@code login.*}. Las variables de entorno que los sobreescriben se
 * documentan en .env.example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "login")
public class LoginProperties {

	/** Maximo de intentos fallidos por dupla IP y usuario en la ventana. */
	private int maxIntentosIp = 5;

	/** Maximo de intentos por usuario en la ventana. */
	private int maxIntentosUsuario = 5;

	/** Tamano de la ventana deslizante, en minutos. */
	private int ventanaMinutos = 10;

	/** Retencion de la tabla de intentos antes de la purga (@Scheduled), en horas. */
	private int retentionHoras = 24;
}
