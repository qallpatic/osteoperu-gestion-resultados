package pe.com.qallpatic.osteoperu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del gateway SMS bajo el prefijo {@code sms.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

	/** Si es false, no se envian SMS reales (util en local). */
	private boolean enabled = true;

	private Textbee textbee = new Textbee();
	private Message message = new Message();

	@Getter
	@Setter
	public static class Textbee {
		private String url;
		private String apiKey;
		private long frequencyMs = 5000;
	}

	@Getter
	@Setter
	public static class Message {
		/** URL al login del operador (sobreescribible por SMS_LOGIN_URL). Reemplaza {link} en todas las plantillas. */
		private String loginUrl;
		/** Plantilla del SMS de resultados. Placeholders: {nombre}, {link}. Valor en application.yml. */
		private String template;
		/** Plantilla del SMS de credencial. Placeholders: {nombre}, {usuario}, {clave}, {link}. Valor en application.yml. */
		private String credencialTemplate;
		/** Plantilla del SMS de recuperacion de password. Placeholders: {nombre}, {clave}, {link}. Valor en application.yml. */
		private String recuperacionTemplate;
	}
}
