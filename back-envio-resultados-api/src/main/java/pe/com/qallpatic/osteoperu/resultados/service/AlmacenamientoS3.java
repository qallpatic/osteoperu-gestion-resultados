package pe.com.qallpatic.osteoperu.resultados.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.config.StorageProperties;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Implementacion S3-compatible (MinIO). Usada cuando {@code storage.type=s3}.
 *
 * <p>Estructura del object key: {@code <DNI>/<YYYY-MM-DD>/<idGenerado>/<idArchivo>.<extension>}.
 * Operaciones: PutObject (subida), GetObject (descarga), HeadBucket (healthcheck).
 *
 * <p>Fallback: si la descarga con el key nuevo falla con NoSuchKey, intenta el
 * formato legacy {@code resultado_<idGenerado>_<idArchivo>.<ext>} para no romper
 * archivos pre-existentes subidos con el esquema plano anterior.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class AlmacenamientoS3 implements AlmacenamientoArchivos {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final String LEGACY_KEY_PREFIX = "resultado_";

	private final StorageProperties storageProperties;
	private final S3Client s3Client;

	@PostConstruct
	public void init() {
		StorageProperties.S3 cfg = storageProperties.getS3();
		log.info("Almacenamiento S3 listo: endpoint={}, bucket={}, region={}, path-style={}, layout='<DNI>/<YYYY-MM-DD>/<idGenerado>/<idArchivo>.<ext>'",
				cfg.getEndpoint(), cfg.getBucket(), cfg.getRegion(), cfg.isForcePathStyle());
	}

	@Override
	public String guardar(InputStream contenido, long sizeBytes, String contentType, String extension,
			String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		if (sizeBytes <= 0) {
			throw new IllegalArgumentException("Se recibio un archivo vacio.");
		}
		String idArchivo = GeneradorCodigos.generar(24);
		String key = construirKey(idGenerado, idArchivo, extension, documentoIdentidad, fechaHoraCarga);
		try {
			PutObjectRequest req = PutObjectRequest.builder()
					.bucket(storageProperties.getS3().getBucket())
					.key(key)
					.contentType(contentType != null ? contentType : "application/octet-stream")
					.contentLength(sizeBytes)
					.metadata(Map.of(
							"id-archivo", idArchivo,
							"id-generado", idGenerado,
							"documento-identidad", documentoIdentidad))
					.build();
			PutObjectResponse resp = s3Client.putObject(req, RequestBody.fromInputStream(contenido, sizeBytes));
			log.info("Archivo guardado (s3): bucket={}, key={}, etag={}",
					storageProperties.getS3().getBucket(), key, resp.eTag());
			return idArchivo;
		} catch (S3Exception e) {
			throw new IllegalStateException("Error al subir a S3: " + e.awsErrorDetails().errorMessage(), e);
		}
	}

	@Override
	public ArchivoDescargable descargar(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		String key = construirKey(idGenerado, idArchivo, extension, documentoIdentidad, fechaHoraCarga);
		try {
			return getObject(key);
		} catch (NoSuchKeyException e) {
			// Fallback: formato plano legacy `resultado_<idGenerado>_<idArchivo>.<ext>`.
			// Si el archivo existe ahi (subido antes del rediseño de keys), lo servimos.
			String legacyKey = legacyKey(idGenerado, idArchivo, extension);
			if (!legacyKey.equals(key)) {
				log.info("NoSuchKey en formato nuevo, intentando fallback legacy: {}", legacyKey);
				try {
					return getObject(legacyKey);
				} catch (NoSuchKeyException e2) {
					throw new RecursoNoEncontradoException("Archivo no encontrado.");
				}
			}
			throw new RecursoNoEncontradoException("Archivo no encontrado.");
		} catch (S3Exception e) {
			throw new IllegalStateException("Error al descargar de S3: " + e.awsErrorDetails().errorMessage(), e);
		}
	}

	@Override
	public boolean disponible() {
		try {
			s3Client.headBucket(HeadBucketRequest.builder()
					.bucket(storageProperties.getS3().getBucket())
					.build());
			return true;
		} catch (S3Exception e) {
			log.warn("S3 no disponible: {}", e.awsErrorDetails().errorMessage());
			return false;
		}
	}

	@Override
	public String construirCarpeta(String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		String fecha = fechaHoraCarga.format(ISO_DATE);
		return AlmacenamientoLocal.sanitizar(documentoIdentidad) + "/" + fecha + "/" + AlmacenamientoLocal.sanitizar(idGenerado);
	}

	@Override
	public String construirKey(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		StringBuilder sb = new StringBuilder(construirCarpeta(idGenerado, documentoIdentidad, fechaHoraCarga));
		if (idArchivo != null && !idArchivo.isBlank()) {
			sb.append('/').append(AlmacenamientoLocal.sanitizar(idArchivo));
		}
		if (extension != null && !extension.isBlank()) {
			sb.append('.').append(extension.toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}

	private ArchivoDescargable getObject(String key) {
		GetObjectRequest req = GetObjectRequest.builder()
				.bucket(storageProperties.getS3().getBucket())
				.key(key)
				.build();
		ResponseInputStream<GetObjectResponse> resp = s3Client.getObject(req);
		String contentType = resp.response().contentType();
		if (contentType == null || contentType.isBlank()) {
			contentType = MediaTypeFactory.getMediaType(key)
					.map(org.springframework.http.MediaType::toString)
					.orElse("application/octet-stream");
		}
		long size = resp.response().contentLength() != null ? resp.response().contentLength() : -1L;
		return new ArchivoDescargable(resp, size, contentType, key);
	}

	private static String legacyKey(String idGenerado, String idArchivo, String extension) {
		StringBuilder sb = new StringBuilder(LEGACY_KEY_PREFIX);
		sb.append(AlmacenamientoLocal.sanitizar(idGenerado))
				.append('_').append(AlmacenamientoLocal.sanitizar(idArchivo));
		if (extension != null && !extension.isBlank()) {
			sb.append('.').append(extension.toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}

	/** Util para purga futura (no usado por el flujo actual). */
	public void eliminar(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		String key = construirKey(idGenerado, idArchivo, extension, documentoIdentidad, fechaHoraCarga);
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(storageProperties.getS3().getBucket())
				.key(key)
				.build());
	}
}
