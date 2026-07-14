package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.compartido.util.NormalizadorTexto;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.integracion.pdf.ValidadorPdf;
import pe.com.qallpatic.osteoperu.integracion.reniec.ClienteReniec;
import pe.com.qallpatic.osteoperu.integracion.reniec.DatosReniec;
import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.UploadResultadoResponse;
import pe.com.qallpatic.osteoperu.resultados.entity.ArchivoResultado;
import pe.com.qallpatic.osteoperu.resultados.entity.EnlaceCorto;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoSms;
import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoMedio;
import pe.com.qallpatic.osteoperu.resultados.repository.LogCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta la carga de resultados de un paciente:
 *  1. valida telefono/dni,
 *  2. valida el nombre del PDF contra RENIEC (si hay proveedor),
 *  3. crea la carpeta {telefono}_{dni} y guarda los archivos,
 *  4. persiste el resultado, genera el link corto y encola el SMS,
 *  5. registra la operacion en la bitacora (LOG_CARGA).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResultadoService {

	/** Proporcion minima de tokens del nombre RENIEC que deben aparecer en el PDF. */
	private static final double UMBRAL_COINCIDENCIA = 0.6;

	private final StorageService storageService;
	private final ShortLinkService shortLinkService;
	private final SmsService smsService;
	private final ResultadoRepository resultadoRepository;
	private final LogCargaRepository logCargaRepository;
	private final PacienteRepository pacienteRepository;
	private final ClienteReniec clienteReniec;
	private final ValidadorPdf validadorPdf;
	private final AppProperties appProperties;

	@Transactional
	public UploadResultadoResponse uploadResultados(String rawPhone, String rawDni, String patientName,
			String mensaje, List<MultipartFile> files, String createdBy, boolean confirmarNombre) {

		String phone = UtilTelefono.normalizar(rawPhone, appProperties.getPhone().getCountryCode());
		String dni = UtilTelefono.validarDocumentoIdentidad(rawDni);
		String folderName = storageService.buildFolderName(phone, dni);
		boolean tieneTelefono = phone != null;
		List<MultipartFile> archivos = files == null ? List.of() : files;

		// Validacion de nombre (RENIEC vs PDF). Si no hay PDF, no bloquea la carga.
		ValidacionNombre val = validarNombre(dni, archivos);
		if (val.verificado() && !val.coincide() && !confirmarNombre) {
			registrarLog(null, dni, createdBy, EstadoCarga.ADVERTENCIA,
					"El nombre del PDF no coincide con RENIEC. Requiere confirmacion del agente.",
					val.nombreReniec(), val.nombrePdf(), false);
			log.warn("Carga detenida por nombre no coincidente. dni={}, reniec='{}', pdf='{}'",
					dni, val.nombreReniec(), val.nombrePdf());
			return UploadResultadoResponse.builder()
					.phone(phone)
					.dni(dni)
					.totalFiles(0)
					.storedFiles(List.of())
					.smsStatus("NO_ENVIADO")
					.requiereConfirmacion(true)
					.nombreReniec(val.nombreReniec())
					.nombrePdf(val.nombrePdf())
					.build();
		}

		String nombrePaciente = val.nombreReniec() != null ? val.nombreReniec()
				: (patientName != null && !patientName.isBlank() ? patientName.trim() : null);

		Path patientDir = storageService.ensurePatientFolder(folderName);

		Resultado resultado = Resultado.builder()
				.telefono(phone)
				.dni(dni)
				.nombrePaciente(nombrePaciente)
				.mensaje(mensaje != null && !mensaje.isBlank() ? mensaje.trim() : null)
				.carpeta(folderName)
				.tokenPublico(generateUniqueToken())
				.estadoSms(tieneTelefono ? EstadoSms.PENDIENTE : EstadoSms.DESHABILITADO)
				.contadorVistas(0)
				.build();
		resultado.setUsuarioCreacion(createdBy);

		List<String> storedNames = new ArrayList<>();
		for (MultipartFile file : archivos) {
			if (file.isEmpty()) {
				continue;
			}
			String storedName = storageService.store(patientDir, file);
			storedNames.add(storedName);

			String resolvedContentType = MediaTypeFactory.getMediaType(storedName)
					.map(org.springframework.http.MediaType::toString)
					.orElse(file.getContentType());

			ArchivoResultado archivo = ArchivoResultado.builder()
					.nombreOriginal(file.getOriginalFilename())
					.nombreAlmacenado(storedName)
					.tipoContenido(resolvedContentType)
					.tipoMedio(TipoMedio.desdeTipoContenido(resolvedContentType))
					.tamanoBytes(file.getSize())
					.fechaCreacion(LocalDateTime.now())
					.build();
			resultado.addArchivo(archivo);
		}

		resultado = resultadoRepository.save(resultado);

		// Registra/actualiza el paciente y limpia el "pendiente de subir".
		registrarPacienteConResultados(dni, nombrePaciente, phone, createdBy);

		String viewerUrl = buildViewerUrl(resultado.getTokenPublico());
		EnlaceCorto enlace = shortLinkService.create(viewerUrl, "RESULTADO", resultado.getId());
		String smsLink = buildShortUrl(enlace.getCodigoCorto());

		String smsStatus;
		if (tieneTelefono) {
			smsService.enqueueResultLink(resultado.getId(), phone, nombrePaciente, smsLink);
			smsStatus = EstadoSms.EN_COLA.name();
		} else {
			smsStatus = "SIN_TELEFONO";
		}

		registrarLog(resultado.getId(), dni, createdBy, EstadoCarga.EXITO,
				"Carga exitosa. Archivos: " + storedNames.size(),
				val.nombreReniec(), val.nombrePdf(), val.verificado() ? val.coincide() : null);

		log.info("Resultados cargados. resultado={}, archivos={}, folder={}, link={}, sms={}",
				resultado.getId(), storedNames.size(), folderName, smsLink, smsStatus);

		return UploadResultadoResponse.builder()
				.batchId(resultado.getId())
				.phone(phone)
				.dni(dni)
				.folderName(folderName)
				.totalFiles(storedNames.size())
				.storedFiles(storedNames)
				.shortLink(smsLink)
				.viewerUrl(viewerUrl)
				.smsStatus(smsStatus)
				.requiereConfirmacion(false)
				.nombreReniec(val.nombreReniec())
				.nombrePdf(val.nombrePdf())
				.build();
	}

	/** Compara el nombre oficial (RENIEC) con el texto del primer PDF adjunto. */
	private ValidacionNombre validarNombre(String dni, List<MultipartFile> files) {
		Optional<DatosReniec> datos = clienteReniec.consultar(dni);
		if (datos.isEmpty()) {
			return ValidacionNombre.noVerificado();
		}
		String nombreReniec = datos.get().nombreCompleto();

		byte[] pdf = primerPdf(files);
		if (pdf == null) {
			// Hay nombre RENIEC pero no hay PDF para comparar: no se bloquea.
			return new ValidacionNombre(false, true, nombreReniec, null);
		}

		String texto = NormalizadorTexto.normalizar(validadorPdf.extraerTexto(pdf));
		String[] tokens = NormalizadorTexto.normalizar(nombreReniec).split(" ");

		long total = Arrays.stream(tokens).filter(t -> t.length() >= 3).count();
		long encontrados = Arrays.stream(tokens)
				.filter(t -> t.length() >= 3 && texto.contains(t))
				.count();
		boolean coincide = total > 0 && ((double) encontrados / total) >= UMBRAL_COINCIDENCIA;

		String nombrePdf = coincide
				? nombreReniec
				: tokensEncontrados(tokens, texto);
		return new ValidacionNombre(true, coincide, nombreReniec, nombrePdf);
	}

	private String tokensEncontrados(String[] tokens, String texto) {
		String encontrados = Arrays.stream(tokens)
				.filter(t -> t.length() >= 3 && texto.contains(t))
				.reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b);
		return encontrados.isBlank() ? "(no detectado en el PDF)" : encontrados;
	}

	private byte[] primerPdf(List<MultipartFile> files) {
		if (files == null) {
			return null;
		}
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			String nombre = file.getOriginalFilename();
			boolean esPdf = "application/pdf".equalsIgnoreCase(file.getContentType())
					|| (nombre != null && nombre.toLowerCase().endsWith(".pdf"));
			if (esPdf) {
				try {
					return file.getBytes();
				} catch (IOException e) {
					log.warn("No se pudo leer el PDF para validacion: {}", e.getMessage());
					return null;
				}
			}
		}
		return null;
	}

	/** Crea el paciente si no existe y marca que ya tiene resultados (pendiente=false). */
	private void registrarPacienteConResultados(String dni, String nombre, String telefono, String actor) {
		try {
			Paciente paciente = pacienteRepository.findByDni(dni).orElse(null);
			if (paciente == null) {
				paciente = new Paciente();
				paciente.setDni(dni);
				paciente.setEstado(true);
				paciente.setUsuarioCreacion(actor);
			} else {
				paciente.setUsuarioModificacion(actor);
			}
			if (nombre != null && !nombre.isBlank()) {
				paciente.setNombre(nombre);
			}
			if (telefono != null && !telefono.isBlank()) {
				paciente.setTelefono(telefono);
			}
			paciente.setPendienteResultados(false);
			pacienteRepository.save(paciente);
		} catch (Exception e) {
			log.error("No se pudo registrar/actualizar el paciente. dni={}: {}", dni, e.getMessage());
		}
	}

	private void registrarLog(Long idResultado, String dni, String usuario, EstadoCarga estado,
			String mensaje, String nombreReniec, String nombrePdf, Boolean coincide) {
		try {
			logCargaRepository.save(LogCarga.builder()
					.idResultado(idResultado)
					.dni(dni)
					.usuarioCarga(usuario)
					.estado(estado)
					.mensaje(mensaje)
					.nombreReniec(nombreReniec)
					.nombrePdf(nombrePdf)
					.coincideNombre(coincide)
					.fechaCreacion(LocalDateTime.now())
					.build());
		} catch (Exception e) {
			log.error("No se pudo registrar el log de carga. dni={}: {}", dni, e.getMessage());
		}
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
			token = GeneradorCodigos.generar(24);
		} while (resultadoRepository.findByTokenPublico(token).isPresent());
		return token;
	}

	/** Resultado de la validacion de nombre (RENIEC vs PDF). */
	private record ValidacionNombre(boolean verificado, boolean coincide, String nombreReniec, String nombrePdf) {
		static ValidacionNombre noVerificado() {
			return new ValidacionNombre(false, true, null, null);
		}
	}
}
