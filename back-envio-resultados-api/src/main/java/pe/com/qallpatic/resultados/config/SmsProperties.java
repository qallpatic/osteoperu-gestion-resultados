package pe.com.qallpatic.resultados.config;

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
		/** Plantilla del SMS. Placeholders soportados: {nombre} y {link}. */
		private String template = "Tus resultados estan disponibles: {link}";
	}
}
