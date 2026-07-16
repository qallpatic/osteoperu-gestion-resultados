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
import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.config.StorageProperties;
import pe.com.qallpatic.osteoperu.integracion.pdf.ValidadorPdf;
import pe.com.qallpatic.osteoperu.integracion.reniec.ClienteReniec;
import pe.com.qallpatic.osteoperu.integracion.reniec.DatosReniec;
import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.UploadResultadoResponse;
import pe.com.qallpatic.osteoperu.resultados.entity.EnlaceCorto;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoSms;
import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoMedio;
import pe.com.qallpatic.osteoperu.resultados.repository.LogCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta la carga de resultados de un paciente:
 *  1. valida telefono/dni,
 *  2. valida el nombre del PDF contra RENIEC (si hay proveedor),
 *  3. sube los archivos al backend de almacenamiento (FS local o S3),
 *  4. persiste la cabecera + detalles, genera el link corto y encola el SMS,
 *  5. registra la operacion en la bitacora (LOG_CARGA) y limpia pendiente del paciente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResultadoService {

	/** Proporcion minima de tokens del nombre RENIEC que deben aparecer en el PDF. */
	private static final double UMBRAL_COINCIDENCIA = 0.6;
	private static final int LONGITUD_ID_GENERADO = 32;

	private final AlmacenamientoArchivos almacenamiento;
	private final StorageProperties storageProperties;
	private final ShortLinkService shortLinkService;
	private final SmsService smsService;
	private final ResultadoCargaRepository resultadoCargaRepository;
	private final LogCargaRepository logCargaRepository;
	private final PacienteRepository pacienteRepository;
	private final ClienteReniec clienteReniec;
	private final ValidadorPdf validadorPdf;
	private final AppProperties appProperties;
	private final SmsProperties smsProperties;

	@Transactional
	public UploadResultadoResponse uploadResultados(String rawPhone, String rawDni, String patientName,
			String mensaje, List<MultipartFile> files, String createdBy, boolean confirmarNombre) {

		String phone = UtilTelefono.normalizar(rawPhone, appProperties.getPhone().getCountryCode());
		String dni = UtilTelefono.validarDocumentoIdentidad(rawDni);
		boolean tieneTelefono = phone != null;
		List<MultipartFile> archivos = files == null ? List.of() : files;

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
					.storedFileIds(List.of())
					.smsStatus("NO_ENVIADO")
					.requiereConfirmacion(true)
					.nombreReniec(val.nombreReniec())
					.nombrePdf(val.nombrePdf())
					.build();
		}

		String nombrePaciente = val.nombreReniec() != null ? val.nombreReniec()
				: (patientName != null && !patientName.isBlank() ? patientName.trim() : null);

		String idGenerado = generateUniqueIdGenerado();
		LocalDateTime ahora = LocalDateTime.now();

		ResultadoCarga carga = ResultadoCarga.builder()
				.idGenerado(idGenerado)
				.tokenPublico(generateUniqueToken())
				.documentoIdentidad(dni)
				.telefono(phone)
				.nombrePaciente(nombrePaciente)
				.mensaje(mensaje != null && !mensaje.isBlank() ? mensaje.trim() : null)
				.estado(1)
				.estadoSms(tieneTelefono ? EstadoSms.PENDIENTE : EstadoSms.DESHABILITADO)
				.contadorVistas(0)
				.cantidadArchivos(0)
				.pesoTotalMb(BigDecimal.ZERO)
				.fechaHoraCarga(ahora)
				.archivos(new ArrayList<>())
				.build();
		carga.setUsuarioCreacion(createdBy);

		List<String> storedIds = new ArrayList<>();
		int count = 0;
		BigDecimal pesoTotal = BigDecimal.ZERO;
		for (MultipartFile file : archivos) {
			if (file.isEmpty()) {
				continue;
			}
			validateContentType(file);
			String extension = extraerExtension(file.getOriginalFilename());
			String contentType = MediaTypeFactory.getMediaType(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
					.map(org.springframework.http.MediaType::toString)
					.orElse(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

			String idArchivo;
			try (var in = file.getInputStream()) {
				idArchivo = almacenamiento.guardar(in, file.getSize(), contentType, extension,
						idGenerado, dni, ahora);
			} catch (IOException e) {
				throw new IllegalStateException("No se pudo leer el archivo: " + file.getOriginalFilename(), e);
			}
			storedIds.add(idArchivo);

			ResultadoArchivo archivo = ResultadoArchivo.builder()
					.idGenerado(idGenerado)
					.idArchivo(idArchivo)
					.nombreOriginal(file.getOriginalFilename())
					.tipoContenido(contentType)
					.tipoMedio(TipoMedio.desdeTipoContenido(contentType))
					.extension(extension)
					.pesoMb(BigDecimal.valueOf(file.getSize()).divide(BigDecimal.valueOf(1024L * 1024L), 4, RoundingMode.HALF_UP))
					.fechaCreacion(LocalDateTime.now())
					.build();
			carga.addArchivo(archivo);
			count++;
			pesoTotal = pesoTotal.add(archivo.getPesoMb());
		}
		carga.setCantidadArchivos(count);
		carga.setPesoTotalMb(pesoTotal);

		carga = resultadoCargaRepository.save(carga);

		registrarPacienteConResultados(dni, nombrePaciente, phone, createdBy);

		String viewerUrl = buildViewerUrl(carga.getTokenPublico());
		EnlaceCorto enlace = shortLinkService.create(viewerUrl, "RESULTADO", carga.getIdResultadoCarga());
		String smsLink = buildShortUrl(enlace.getCodigoCorto());
		String smsSendLink = loginUrl();

		String smsStatus;
		if (tieneTelefono) {
			smsService.enqueueResultLink(carga.getIdResultadoCarga(), phone, nombrePaciente, smsSendLink);
			smsStatus = EstadoSms.EN_COLA.name();
		} else {
			smsStatus = "SIN_TELEFONO";
		}

		registrarLog(carga.getIdResultadoCarga(), dni, createdBy, EstadoCarga.EXITO,
				"Carga exitosa. Archivos: " + count,
				val.nombreReniec(), val.nombrePdf(), val.verificado() ? val.coincide() : null);

		String folderKey = almacenamiento.construirCarpeta(idGenerado, dni, ahora);

		log.info("Resultados cargados. carga={}, idGenerado={}, archivos={}, link={}, sms={}",
				carga.getIdResultadoCarga(), idGenerado, count, smsLink, smsStatus);

		return UploadResultadoResponse.builder()
				.cargaId(carga.getIdResultadoCarga())
				.idGenerado(idGenerado)
				.phone(phone)
				.dni(dni)
				.folderKey(folderKey)
				.totalFiles(count)
				.storedFileIds(storedIds)
				.shortLink(smsLink)
				.viewerUrl(viewerUrl)
				.smsStatus(smsStatus)
				.requiereConfirmacion(false)
				.nombreReniec(val.nombreReniec())
				.nombrePdf(val.nombrePdf())
				.build();
	}

	private void validateContentType(MultipartFile file) {
		List<String> allowed = storageProperties.allowedContentTypeList();
		if (allowed.isEmpty()) {
			return;
		}
		String clientCt = file.getContentType();
		String extCt = MediaTypeFactory.getMediaType(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
				.map(org.springframework.http.MediaType::toString)
				.orElse(null);
		boolean clientOk = clientCt != null && allowed.contains(clientCt.toLowerCase(java.util.Locale.ROOT));
		boolean extOk = extCt != null && allowed.contains(extCt.toLowerCase(java.util.Locale.ROOT));
		if (!clientOk && !extOk) {
			throw new SolicitudInvalidaException(
					"Tipo de archivo no permitido: " + clientCt + " (" + file.getOriginalFilename() + ")");
		}
	}

	private String extraerExtension(String nombre) {
		if (nombre == null) {
			return null;
		}
		int dot = nombre.lastIndexOf('.');
		if (dot < 0 || dot == nombre.length() - 1) {
			return null;
		}
		return nombre.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
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
			return new ValidacionNombre(false, true, nombreReniec, null);
		}

		String texto = NormalizadorTexto.normalizar(validadorPdf.extraerTexto(pdf));
		String[] tokens = NormalizadorTexto.normalizar(nombreReniec).split(" ");

		long total = Arrays.stream(tokens).filter(t -> t.length() >= 3).count();
		long encontrados = Arrays.stream(tokens)
				.filter(t -> t.length() >= 3 && texto.contains(t))
				.count();
		boolean coincide = total > 0 && ((double) encontrados / total) >= UMBRAL_COINCIDENCIA;

		String nombrePdf = coincide ? nombreReniec : tokensEncontrados(tokens, texto);
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

	/** Link al login que viaja en el SMS de resultados (config sms.message.login-url). */
	private String loginUrl() {
		String url = smsProperties.getMessage().getLoginUrl();
		return url == null ? "" : url.trim();
	}

	private String generateUniqueToken() {
		String token;
		do {
			token = GeneradorCodigos.generar(24);
		} while (resultadoCargaRepository.findByTokenPublico(token).isPresent());
		return token;
	}

	private String generateUniqueIdGenerado() {
		String id;
		do {
			id = GeneradorCodigos.generar(LONGITUD_ID_GENERADO);
		} while (resultadoCargaRepository.findByIdGenerado(id).isPresent());
		return id;
	}

	private record ValidacionNombre(boolean verificado, boolean coincide, String nombreReniec, String nombrePdf) {
		static ValidacionNombre noVerificado() {
			return new ValidacionNombre(false, true, null, null);
		}
	}
}
