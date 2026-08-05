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
		/**
		 * Duracion del access token en milisegundos. Default 4 horas para soportar
		 * jornadas continuas de carga (RESULTADOS/ADMISION), reduciendo a la minima
		 * expresion los ciclos 401 -> refresh -> retry que se observaban con 15 min.
		 * Override por env JWT_ACCESS_MS.
		 */
		private long accessExpirationMs = 14_400_000L;
		/** Duracion del refresh token en milisegundos (default 7 dias). */
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
