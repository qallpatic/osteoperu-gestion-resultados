package pe.com.qallpatic.resultados.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parametros de negocio bajo el prefijo {@code app.*} del application.yml.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private Security security = new Security();
	private Phone phone = new Phone();
	private Shortlink shortlink = new Shortlink();
	private Cors cors = new Cors();

	/** URL publica base con la que se construye el link que viaja en el SMS. */
	private String publicBaseUrl;

	@Getter
	@Setter
	public static class Security {
		private String username;
		private String password;
	}

	@Getter
	@Setter
	public static class Phone {
		/** Prefijo de pais para normalizar numeros (Peru = 51). */
		private String countryCode = "51";
	}

	@Getter
	@Setter
	public static class Shortlink {
		private int codeLength = 7;
		/** Dias de validez del link (0 = sin expiracion). */
		private int ttlDays = 0;
	}

	@Getter
	@Setter
	public static class Cors {
		/** Origenes del front autorizados a consumir la API (panel de carga). */
		private List<String> allowedOrigins = List.of();
	}
}
