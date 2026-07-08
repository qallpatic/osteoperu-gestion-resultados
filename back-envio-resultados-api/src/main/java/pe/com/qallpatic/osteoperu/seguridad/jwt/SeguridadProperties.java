package pe.com.qallpatic.osteoperu.seguridad.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parametros de seguridad bajo el prefijo {@code seguridad.*}. */
@Getter
@Setter
@ConfigurationProperties(prefix = "seguridad")
public class SeguridadProperties {

	private Jwt jwt = new Jwt();
	private Admin admin = new Admin();

	@Getter
	@Setter
	public static class Jwt {
		/** Clave HS256 (minimo 32 bytes). */
		private String secret;
		/** Duracion del access token en milisegundos. */
		private long accessExpirationMs = 900_000L;
		/** Duracion del refresh token en milisegundos. */
		private long refreshExpirationMs = 604_800_000L;
		private String issuer = "osteoperu";
	}

	@Getter
	@Setter
	public static class Admin {
		private String username = "admin";
		private String password = "Admin@2026";
		private String nombre = "Administrador";
	}
}
