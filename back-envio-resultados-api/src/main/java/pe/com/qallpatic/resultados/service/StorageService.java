package pe.com.qallpatic.resultados.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import pe.com.qallpatic.resultados.config.StorageProperties;
import pe.com.qallpatic.resultados.exception.BadRequestException;
import pe.com.qallpatic.resultados.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Maneja el sistema de archivos: crea una carpeta por paciente ({telefono}_{dni})
 * y guarda/lee los resultados. La ruta base se toma del ambiente (local/server).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

	private final StorageProperties storageProperties;

	/** Crea la carpeta base de 'uploads' al arrancar si aun no existe. */
	@PostConstruct
	public void initBaseFolder() {
		try {
			Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
			Files.createDirectories(base);
			log.info("Carpeta base de resultados lista: {}", base);
		} catch (IOException e) {
			throw new IllegalStateException(
					"No se pudo crear la carpeta base de resultados: " + storageProperties.getBasePath(), e);
		}
	}

	public String buildFolderName(String phone, String dni) {
		return phone + "_" + dni;
	}

	/** Crea (si no existe) la carpeta del paciente y devuelve su ruta absoluta. */
	public Path ensurePatientFolder(String folderName) {
		try {
			Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
			Path patientDir = base.resolve(folderName).normalize();
			if (!patientDir.startsWith(base)) {
				throw new BadRequestException("Ruta de carpeta invalida.");
			}
			Files.createDirectories(patientDir);
			return patientDir;
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo crear la carpeta del paciente: " + folderName, e);
		}
	}

	public void validateContentType(MultipartFile file) {
		List<String> allowed = storageProperties.allowedContentTypeList();
		if (allowed.isEmpty()) {
			return;
		}
		// El content-type del cliente (Postman/navegador) no es confiable: puede llegar
		// vacio o equivocado. Aceptamos si el tipo declarado por el cliente O el tipo real
		// resuelto por la extension del archivo esta en la lista de permitidos.
		String clientCt = file.getContentType();
		String extCt = MediaTypeFactory.getMediaType(
						file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
				.map(org.springframework.http.MediaType::toString)
				.orElse(null);

		boolean clientOk = clientCt != null && allowed.contains(clientCt.toLowerCase(Locale.ROOT));
		boolean extOk = extCt != null && allowed.contains(extCt.toLowerCase(Locale.ROOT));

		if (!clientOk && !extOk) {
			throw new BadRequestException(
					"Tipo de archivo no permitido: " + clientCt + " (" + file.getOriginalFilename() + ")");
		}
	}

	/**
	 * Guarda un archivo dentro de la carpeta del paciente con un nombre saneado y
	 * unico. Devuelve el nombre final almacenado.
	 */
	public String store(Path patientDir, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Se recibio un archivo vacio.");
		}
		validateContentType(file);

		String storedName = buildStoredFilename(file.getOriginalFilename());
		Path destination = patientDir.resolve(storedName).normalize();
		if (!destination.startsWith(patientDir)) {
			throw new BadRequestException("Nombre de archivo invalido.");
		}

		try (InputStream in = file.getInputStream()) {
			Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
			log.info("Archivo guardado: {}", destination);
			return storedName;
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo guardar el archivo: " + storedName, e);
		}
	}

	/** Resuelve la ruta de un archivo del paciente validando que no salga de su carpeta. */
	public Path resolveFile(String folderName, String storedFilename) {
		Path base = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
		Path patientDir = base.resolve(folderName).normalize();
		Path file = patientDir.resolve(storedFilename).normalize();
		if (!file.startsWith(patientDir)) {
			throw new BadRequestException("Ruta de archivo invalida.");
		}
		if (!Files.exists(file)) {
			throw new NotFoundException("Archivo no encontrado.");
		}
		return file;
	}

	private String buildStoredFilename(String originalFilename) {
		String cleaned = StringUtils.cleanPath(originalFilename == null ? "archivo" : originalFilename);
		String baseName = StringUtils.getFilename(cleaned);
		String ext = StringUtils.getFilenameExtension(baseName);

		String nameNoExt = (ext != null) ? baseName.substring(0, baseName.length() - ext.length() - 1) : baseName;
		// Solo caracteres seguros
		nameNoExt = nameNoExt.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (nameNoExt.isBlank()) {
			nameNoExt = "archivo";
		}

		String timestamp = LocalDateTime.now().format(TS);
		return nameNoExt + "_" + timestamp + (ext != null ? "." + ext.toLowerCase(Locale.ROOT) : "");
	}
}
