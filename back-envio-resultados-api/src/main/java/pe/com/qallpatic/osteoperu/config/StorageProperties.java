package pe.com.qallpatic.osteoperu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Parametros de almacenamiento bajo el prefijo {@code storage.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

	/** Carpeta raiz donde se crean las subcarpetas {telefono}_{dni}. */
	private String basePath;

	/** Content-types permitidos, separados por coma en el yml. */
	private String allowedContentTypes = "";

	public List<String> allowedContentTypeList() {
		return Arrays.stream(allowedContentTypes.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}
}
