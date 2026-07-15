package pe.com.qallpatic.osteoperu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Parametros de almacenamiento bajo el prefijo {@code storage.*}.
 * Selecciona entre FS local y S3-compatible (MinIO) mediante {@link #type}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

	/** Tipo de almacenamiento: "local" (FS del contenedor) o "s3" (S3-compatible / MinIO). */
	private String type = "local";

	/** (local) Carpeta raiz donde se crean los archivos. */
	private String basePath;

	/** Content-types permitidos, separados por coma en el yml. */
	private String allowedContentTypes = "";

	/** Bloque de configuracion S3 / MinIO. */
	private S3 s3 = new S3();

	public List<String> allowedContentTypeList() {
		return Arrays.stream(allowedContentTypes.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	@Getter
	@Setter
	public static class S3 {
		/** Endpoint del servicio S3-compatible (ej: https://ia.qallpatic.com.pe). */
		private String endpoint;
		/** Region AWS (us-east-1 por defecto para MinIO). */
		private String region = "us-east-1";
		/** Access key. */
		private String accessKey;
		/** Secret key. */
		private String secretKey;
		/** Nombre del bucket destino. */
		private String bucket;
		/** true = path-style (requerido por MinIO); false = virtual-host style (AWS estandar). */
		private boolean forcePathStyle = true;
		/** TTL en minutos para URLs firmadas (presigner). */
		private int presignMinutes = 10;
	}
}
