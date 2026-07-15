package pe.com.qallpatic.osteoperu.resultados.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Implementacion basada en filesystem local. Usada cuando
 * {@code storage.type=local} (default si no se configura).
 *
 * <p>Estructura en disco: {@code <basePath>/<DNI>/<YYYY-MM-DD>/<idGenerado>/<idArchivo>.<ext>}
 * (jerarquica, igual que en S3 para mantener parity entre modos).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class AlmacenamientoLocal implements AlmacenamientoArchivos {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final StorageProperties storageProperties;

	@PostConstruct
	public void init() {
		try {
			Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
			Files.createDirectories(base);
			log.info("Almacenamiento LOCAL listo en: {}", base);
		} catch (IOException e) {
			throw new IllegalStateException(
					"No se pudo crear la carpeta base de almacenamiento: " + storageProperties.getBasePath(), e);
		}
	}

	@Override
	public String guardar(InputStream contenido, long sizeBytes, String contentType, String extension,
			String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		if (sizeBytes <= 0) {
			throw new SolicitudInvalidaException("Se recibio un archivo vacio.");
		}
		String idArchivo = GeneradorCodigos.generar(24);
		String key = construirKey(idGenerado, idArchivo, extension, documentoIdentidad, fechaHoraCarga);
		Path destino = resolverPathSeguro(key);
		try {
			Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
			log.info("Archivo guardado (local): {} ({} bytes)", destino, sizeBytes);
			return idArchivo;
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo guardar el archivo: " + key, e);
		}
	}

	@Override
	public ArchivoDescargable descargar(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		String key = construirKey(idGenerado, idArchivo, extension, documentoIdentidad, fechaHoraCarga);
		Path path = resolverPathSeguro(key);
		if (!Files.exists(path)) {
			throw new RecursoNoEncontradoException("Archivo no encontrado.");
		}
		return leerArchivo(path);
	}

	@Override
	public boolean disponible() {
		Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
		return Files.isDirectory(base) && Files.isWritable(base);
	}

	@Override
	public String construirCarpeta(String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		String fecha = fechaHoraCarga.format(ISO_DATE);
		return sanitizar(documentoIdentidad) + "/" + fecha + "/" + sanitizar(idGenerado);
	}

	@Override
	public String construirKey(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga) {
		StringBuilder sb = new StringBuilder(construirCarpeta(idGenerado, documentoIdentidad, fechaHoraCarga));
		if (idArchivo != null && !idArchivo.isBlank()) {
			sb.append('/').append(sanitizar(idArchivo));
		}
		if (extension != null && !extension.isBlank()) {
			sb.append('.').append(extension.toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}

	/** Valida que el path resuelto este dentro de basePath (anti path-traversal). */
	private Path resolverPathSeguro(String key) {
		Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
		Path destino = base.resolve(key).normalize();
		if (!destino.startsWith(base)) {
			throw new SolicitudInvalidaException("Ruta de archivo invalida.");
		}
		return destino;
	}

	private ArchivoDescargable leerArchivo(Path path) {
		String contentType = MediaTypeFactory.getMediaType(path.getFileName().toString())
				.map(org.springframework.http.MediaType::toString)
				.orElse("application/octet-stream");
		long size;
		try {
			size = Files.size(path);
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo leer el tamano del archivo: " + path, e);
		}
		try {
			return new ArchivoDescargable(Files.newInputStream(path), size, contentType, path.getFileName().toString());
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo abrir el archivo: " + path, e);
		}
	}

	/**
	 * Anti path-traversal defensivo. Aunque validamos DNI/extension en la capa de
	 * servicio (solo digitos / [a-z]+), sanitizamos aca por si en algun escenario
	 * futuro el dato viene de otra fuente.
	 */
	static String sanitizar(String s) {
		if (s == null) {
			return "_";
		}
		String limpio = s.replaceAll("[^a-zA-Z0-9_-]", "_");
		return limpio.isBlank() ? "_" : limpio;
	}
}
