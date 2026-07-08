package pe.com.qallpatic.osteoperu.integracion.reniec;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion del cliente RENIEC bajo el prefijo {@code reniec.*}. */
@Getter
@Setter
@ConfigurationProperties(prefix = "reniec")
public class ReniecProperties {

	/** false = modo mock (sin proveedor real). */
	private boolean enabled = false;
	private String url;
	private String token;
	private long timeoutMs = 6000;
}
