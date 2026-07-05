package pe.com.qallpatic.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.com.qallpatic.resultados.config.AppProperties;
import pe.com.qallpatic.resultados.dto.UploadResultadoResponse;
import pe.com.qallpatic.resultados.exception.BadRequestException;
import pe.com.qallpatic.resultados.model.MediaType;
import pe.com.qallpatic.resultados.model.PatientResultBatch;
import pe.com.qallpatic.resultados.model.ResultFile;
import pe.com.qallpatic.resultados.model.ShortLink;
import pe.com.qallpatic.resultados.model.SmsStatus;
import pe.com.qallpatic.resultados.repository.PatientResultBatchRepository;
import pe.com.qallpatic.resultados.repository.ShortLinkRepository;
import pe.com.qallpatic.resultados.util.PhoneUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la carga de resultados de un paciente:
 *  1. valida telefono/dni,
 *  2. crea la carpeta {telefono}_{dni} y guarda los archivos,
 *  3. persiste el lote y sus archivos,
 *  4. genera el token del visor y el link corto,
 *  5. encola el SMS con el link.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResultadoService {

	private final StorageService storageService;
	private final ShortLinkService shortLinkService;
	private final SmsService smsService;
	private final PatientResultBatchRepository batchRepository;
	private final ShortLinkRepository shortLinkRepository;
	private final AppProperties appProperties;

	@Transactional
	public UploadResultadoResponse uploadResultados(String rawPhone, String rawDni, String patientName,
			List<MultipartFile> files, String createdBy) {

		if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
			throw new BadRequestException("Debe adjuntar al menos un archivo de resultado.");
		}

		String phone = PhoneUtil.normalize(rawPhone, appProperties.getPhone().getCountryCode());
		String dni = PhoneUtil.validateDni(rawDni);
		String folderName = storageService.buildFolderName(phone, dni);

		// 1) Carpeta fisica del paciente
		Path patientDir = storageService.ensurePatientFolder(folderName);

		// 2) Lote + token unico del visor
		PatientResultBatch batch = PatientResultBatch.builder()
				.phone(phone)
				.dni(dni)
				.folderName(folderName)
				.publicToken(generateUniqueToken())
				.smsStatus(SmsStatus.PENDING)
				.viewCount(0)
				.createdBy(createdBy)
				.createdAt(LocalDateTime.now())
				.build();

		// 3) Guardar archivos en disco y registrar metadatos
		List<String> storedNames = new ArrayList<>();
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			String storedName = storageService.store(patientDir, file);
			storedNames.add(storedName);

			// El Content-Type que envia el cliente (Postman/navegador) no es confiable:
			// puede llegar vacio o equivocado (ej. un PDF marcado como video/mp4). Por eso
			// resolvemos el tipo real desde la extension del archivo guardado, que es la
			// misma fuente que usa el visor para servirlo (MediaTypeFactory).
			String resolvedContentType = MediaTypeFactory.getMediaType(storedName)
					.map(org.springframework.http.MediaType::toString)
					.orElse(file.getContentType());

			ResultFile rf = ResultFile.builder()
					.originalFilename(file.getOriginalFilename())
					.storedFilename(storedName)
					.contentType(resolvedContentType)
					.mediaType(MediaType.fromContentType(resolvedContentType))
					.sizeBytes(file.getSize())
					.createdAt(LocalDateTime.now())
					.build();
			batch.addFile(rf);
		}

		batch = batchRepository.save(batch);

		// 4) Link del visor + link corto para el SMS
		String viewerUrl = buildViewerUrl(batch.getPublicToken());
		ShortLink shortLink = shortLinkService.create(viewerUrl, "RESULTADO", batch.getId());
		String smsLink = buildShortUrl(shortLink.getShortCode());

		// 5) Encolar SMS
		smsService.enqueueResultLink(batch.getId(), phone, patientName, smsLink);

		log.info("Resultados cargados. batch={}, archivos={}, folder={}, link={}",
				batch.getId(), storedNames.size(), folderName, smsLink);

		return UploadResultadoResponse.builder()
				.batchId(batch.getId())
				.phone(phone)
				.dni(dni)
				.folderName(folderName)
				.totalFiles(storedNames.size())
				.storedFiles(storedNames)
				.shortLink(smsLink)
				.viewerUrl(viewerUrl)
				.smsStatus(SmsStatus.QUEUED.name())
				.build();
	}

	private String buildViewerUrl(String token) {
		return trimBase() + "/r/" + token;
	}

	private String buildShortUrl(String shortCode) {
		return trimBase() + "/s/" + shortCode;
	}

	private String trimBase() {
		String base = appProperties.getPublicBaseUrl();
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private String generateUniqueToken() {
		String token;
		do {
			token = ShortCodeGenerator.generate(24);
		} while (batchRepository.findByPublicToken(token).isPresent());
		return token;
	}
}
