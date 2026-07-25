package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.util.OrdenamientoUtil;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.ActualizarArchivosRequest;
import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoMedio;
import pe.com.qallpatic.osteoperu.resultados.mapper.ResultadoMapper;
import pe.com.qallpatic.osteoperu.resultados.repository.EnlaceCortoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.LogCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoArchivoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.spec.LogCargaSpecs;
import pe.com.qallpatic.osteoperu.resultados.spec.ResultadoCargaSpecs;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultaResultadoServiceImpl implements ConsultaResultadoService {

	private final ResultadoCargaRepository resultadoCargaRepository;
	private final LogCargaRepository logCargaRepository;
	private final EnlaceCortoRepository enlaceCortoRepository;
	private final ResultadoArchivoRepository resultadoArchivoRepository;
	private final ResultadoMapper resultadoMapper;
	private final SmsService smsService;
	private final PacienteRepository pacienteRepository;
	private final AlmacenamientoArchivos almacenamiento;
	private final AppProperties appProperties;
	private final SmsProperties smsProperties;

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<PacientePendienteResponse> pendientes(int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 20 : tamano);
		Page<PacientePendienteResponse> page = pacienteRepository
				.findByPendienteResultadosTrueOrderByFechaCreacionDesc(pageable)
				.map(p -> new PacientePendienteResponse(
						p.getDni(), p.getNombre(), p.getTelefono(), p.getFechaCreacion()));
		return PaginaResponse.de(page);
	}

	@Override
	@Transactional(readOnly = true)
	public long contarPendientes() {
		return pacienteRepository.countByPendienteResultadosTrue();
	}

	private static final Map<String, String> ALIAS_RESULTADO = Map.of(
			"dni", "documentoIdentidad",
			"paciente", "nombrePaciente");
	private static final Set<String> CAMPOS_RESULTADO = Set.of(
			"documentoIdentidad", "nombrePaciente", "telefono",
			"estadoSms", "contadorVistas", "fechaHoraCarga");

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<ResultadoResumenResponse> listar(String dni, String nombre,
			LocalDate desde, LocalDate hasta, int pagina, int tamano,
			String ordenarPor, String direccion) {
		Sort sort = OrdenamientoUtil.resolver(ordenarPor, direccion,
				ALIAS_RESULTADO, CAMPOS_RESULTADO, "fechaHoraCarga");
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano, sort);
		Page<ResultadoResumenResponse> page = resultadoCargaRepository
				.findAll(ResultadoCargaSpecs.filtro(dni, nombre, null, inicioDia(desde), finDia(hasta)), pageable)
				.map(resultadoMapper::toResumen);
		return PaginaResponse.de(page);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultadoDetalleResponse detalle(Long id) {
		return armarDetalle(buscar(id));
	}

	@Override
	@Transactional
	public ResultadoDetalleResponse actualizar(Long id, ResultadoUpdateRequest solicitud, String actor) {
		ResultadoCarga carga = buscar(id);

		if (solicitud.telefono() != null && !solicitud.telefono().isBlank()) {
			carga.setTelefono(UtilTelefono.normalizar(solicitud.telefono(),
					appProperties.getPhone().getCountryCode()));
		}
		if (solicitud.nombrePaciente() != null) {
			carga.setNombrePaciente(
					solicitud.nombrePaciente().isBlank() ? null : solicitud.nombrePaciente().trim());
		}
		carga.setUsuarioModificacion(actor);
		resultadoCargaRepository.save(carga);

		if (solicitud.reenviarSms()) {
			String link = loginUrl();
			smsService.enqueueResultLink(id, carga.getTelefono(), carga.getNombrePaciente(), link);
			log.info("SMS reencolado para carga {} con link {}", id, link);
		}

		return armarDetalle(carga);
	}

	@Override
	@Transactional
	public ResultadoDetalleResponse actualizarArchivos(Long id, ActualizarArchivosRequest solicitud, String actor) {
		ResultadoCarga carga = buscar(id);

		if (carga.getEstado() == null || carga.getEstado() != 1) {
			throw new SolicitudInvalidaException(
					"No se puede editar una carga anulada. Restaurela o cree una nueva.");
		}

		List<Long> desactivar = solicitud.idsADesactivar() == null ? List.of() : solicitud.idsADesactivar();
		List<Long> reactivar = solicitud.idsAReactivar() == null ? List.of() : solicitud.idsAReactivar();
		List<MultipartFile> nuevos = solicitud.archivosNuevos() == null ? List.of() : solicitud.archivosNuevos();

		if (desactivar.isEmpty() && reactivar.isEmpty()
				&& nuevos.stream().allMatch(f -> f == null || f.isEmpty())) {
			throw new SolicitudInvalidaException(
					"Debe indicar al menos un cambio: desactivar, reactivar o subir archivos.");
		}

		LocalDateTime ahora = LocalDateTime.now();

		int desactivados = aplicarDesactivaciones(carga, desactivar, solicitud.motivosPorArchivo(), actor, ahora);
		int reactivados = aplicarReactivaciones(carga, reactivar, actor, ahora);

		// Los archivos nuevos se persisten via repository directo para evitar
		// el cascade confuso de Hibernate sobre la PersistentBag con snapshot previa
		// (orphanRemoval=true). Al final se refresca la carga para tener la lista
		// sincronizada antes de recalcular contadores y armar la respuesta.
		int subidos = subirArchivosNuevos(carga, nuevos, actor);

		carga.setUsuarioModificacion(actor);
		ResultadoCarga guardada = resultadoCargaRepository.save(carga);

		ResultadoCarga cargaFresca = resultadoCargaRepository.findByIdConArchivos(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Carga no encontrada: " + id));
		recalcularContadores(cargaFresca);
		resultadoCargaRepository.save(cargaFresca);

		String mensajeLog = String.format(
				"Archivos modificados. desactivados=%d, reactivados=%d, subidos=%d.",
				desactivados, reactivados, subidos);
		registrarLog(cargaFresca.getIdResultadoCarga(), cargaFresca.getDocumentoIdentidad(), actor,
				EstadoCarga.MODIFICADO, mensajeLog, null, null, null);

		if (solicitud.reenviarSms()) {
			String link = loginUrl();
			smsService.enqueueResultLink(cargaFresca.getIdResultadoCarga(), cargaFresca.getTelefono(),
					cargaFresca.getNombrePaciente(), link);
			log.info("SMS reencolado por edicion de archivos. carga={}", cargaFresca.getIdResultadoCarga());
		}

		log.info("Archivos editados. carga={}, usuario={}, des={}, rea={}, sub={}",
				cargaFresca.getIdResultadoCarga(), actor, desactivados, reactivados, subidos);

		return armarDetalle(cargaFresca);
	}

	private static final Set<String> CAMPOS_LOG = Set.of(
			"estado", "dni", "usuarioCarga", "nombreReniec", "nombrePdf",
			"coincideNombre", "fechaCreacion");

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<LogCargaResponse> listarLogs(String dni, LocalDate desde, LocalDate hasta,
			int pagina, int tamano, String ordenarPor, String direccion) {
		Sort sort = OrdenamientoUtil.resolver(ordenarPor, direccion,
				Map.of(), CAMPOS_LOG, "fechaCreacion");
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano, sort);
		Page<LogCargaResponse> page = logCargaRepository
				.findAll(LogCargaSpecs.filtro(dni, inicioDia(desde), finDia(hasta)), pageable)
				.map(resultadoMapper::toLog);
		return PaginaResponse.de(page);
	}

	/**
	 * Marca como inactivos los archivos solicitados. Devuelve cuantos efectivamente
	 * pasaron de activo a inactivo. Lanza 400 si la operacion dejaria la cabecera
	 * sin archivos activos o si algun archivo a desactivar llega sin motivo.
	 *
	 * <p>Cada archivo desactivado debe llevar su motivo individual (obligatorio,
	 * queda en la bitacora). Si el operador lo deja vacio en el dialog, el
	 * frontend bloquea el envio; el backend valida de nuevo como defensa en
	 * profundidad para llamadas que no pasen por la UI.
	 */
	private int aplicarDesactivaciones(ResultadoCarga carga, List<Long> ids,
			Map<Long, String> motivosPorArchivo, String actor, LocalDateTime ahora) {
		if (ids.isEmpty()) {
			return 0;
		}
		int restantesActivos = contarActivos(carga) - contarCoincidencias(carga, ids, true);
		if (restantesActivos <= 0) {
			throw new SolicitudInvalidaException(
					"No se puede desactivar: la cabecera debe conservar al menos un archivo activo.");
		}

		List<Long> sinMotivo = new ArrayList<>();
		int aplicados = 0;
		for (ResultadoArchivo a : carga.getArchivos()) {
			if (ids.contains(a.getIdResultadoArchivo()) && (a.getEstado() == null || a.getEstado() != 0)) {
				String motivoIndividual = motivosPorArchivo == null ? null : motivosPorArchivo.get(a.getIdResultadoArchivo());
				String motivoFinal = motivoIndividual != null && !motivoIndividual.isBlank()
						? motivoIndividual.trim()
						: null;
				if (motivoFinal == null) {
					sinMotivo.add(a.getIdResultadoArchivo());
					continue;
				}
				a.setEstado(0);
				a.setFechaEliminacion(ahora);
				a.setUsuarioEliminacion(actor);
				a.setMotivoEliminacion(motivoFinal);
				aplicados++;
			}
		}
		if (!sinMotivo.isEmpty()) {
			throw new SolicitudInvalidaException(
					"Falta el motivo de desactivacion para los archivos: " + sinMotivo);
		}
		return aplicados;
	}

	/** Reactiva archivos previamente desactivados (limpia campos de soft-delete). */
	private int aplicarReactivaciones(ResultadoCarga carga, List<Long> ids, String actor, LocalDateTime ahora) {
		if (ids.isEmpty()) {
			return 0;
		}
		int aplicados = 0;
		for (ResultadoArchivo a : carga.getArchivos()) {
			if (ids.contains(a.getIdResultadoArchivo()) && a.getEstado() != null && a.getEstado() == 0) {
				a.setEstado(1);
				a.setFechaEliminacion(null);
				a.setUsuarioEliminacion(null);
				a.setMotivoEliminacion(null);
				aplicados++;
			}
		}
		return aplicados;
	}

	/**
	 * Sube N archivos al mismo visor (mismo idGenerado / dni / fechaHoraCarga) y
	 * los persiste como activos via repository directo. NO se agregan a la
	 * coleccion {@code carga.archivos} en memoria porque Hibernate se confunde
	 * con la snapshot previa de la PersistentBag y lanza TransientObjectException
	 * al hacer flush. El caller refresca la carga con findById despues.
	 *
	 * <p>No re-corre validarNombre: la cabecera original ya paso la validacion;
	 * los nuevos son una correccion posterior.
	 */
	private int subirArchivosNuevos(ResultadoCarga carga, List<MultipartFile> archivos, String actor) {
		int subidos = 0;
		for (MultipartFile file : archivos) {
			if (file == null || file.isEmpty()) {
				continue;
			}
			String extension = extraerExtension(file.getOriginalFilename());
			String contentType = MediaTypeFactory.getMediaType(
					file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
					.map(org.springframework.http.MediaType::toString)
					.orElse(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

			String idArchivo;
			try (var in = file.getInputStream()) {
				idArchivo = almacenamiento.guardar(in, file.getSize(), contentType, extension,
						carga.getIdGenerado(), carga.getDocumentoIdentidad(), carga.getFechaHoraCarga());
			} catch (IOException e) {
				throw new IllegalStateException(
						"No se pudo leer el archivo: " + file.getOriginalFilename(), e);
			}

			ResultadoArchivo archivo = ResultadoArchivo.builder()
					.resultadoCarga(carga)
					.idGenerado(carga.getIdGenerado())
					.idArchivo(idArchivo)
					.nombreOriginal(file.getOriginalFilename())
					.tipoContenido(contentType)
					.tipoMedio(TipoMedio.desdeTipoContenido(contentType))
					.extension(extension)
					.pesoMb(BigDecimal.valueOf(file.getSize())
							.divide(BigDecimal.valueOf(1024L * 1024L), 4, RoundingMode.HALF_UP))
					.fechaCreacion(LocalDateTime.now())
					.estado(1)
					.build();
			resultadoArchivoRepository.save(archivo);
			subidos++;
		}
		return subidos;
	}

	/** Recalcula cantidadArchivos y pesoTotalMb contando SOLO archivos activos (lo que ve el paciente). */
	private void recalcularContadores(ResultadoCarga carga) {
		int activos = 0;
		BigDecimal peso = BigDecimal.ZERO;
		for (ResultadoArchivo a : carga.getArchivos()) {
			if (a.getEstado() != null && a.getEstado() == 1) {
				activos++;
				peso = peso.add(a.getPesoMb() == null ? BigDecimal.ZERO : a.getPesoMb());
			}
		}
		carga.setCantidadArchivos(activos);
		carga.setPesoTotalMb(peso);
	}

	private int contarActivos(ResultadoCarga carga) {
		int n = 0;
		for (ResultadoArchivo a : carga.getArchivos()) {
			if (a.getEstado() != null && a.getEstado() == 1) {
				n++;
			}
		}
		return n;
	}

	private int contarCoincidencias(ResultadoCarga carga, List<Long> ids, boolean activos) {
		int n = 0;
		for (ResultadoArchivo a : carga.getArchivos()) {
			int estado = a.getEstado() == null ? 1 : a.getEstado();
			boolean cumpleEstado = activos ? estado == 1 : estado == 0;
			if (cumpleEstado && ids.contains(a.getIdResultadoArchivo())) {
				n++;
			}
		}
		return n;
	}

	private String extraerExtension(String nombre) {
		if (nombre == null) {
			return null;
		}
		int dot = nombre.lastIndexOf('.');
		if (dot < 0 || dot == nombre.length() - 1) {
			return null;
		}
		return nombre.substring(dot + 1).toLowerCase(Locale.ROOT);
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
			log.error("No se pudo registrar el log de modificacion. carga={}: {}", idResultado, e.getMessage());
		}
	}

	private ResultadoDetalleResponse armarDetalle(ResultadoCarga c) {
		List<ArchivoResponse> archivos = c.getArchivos().stream()
				.map(resultadoMapper::toArchivo)
				.toList();
		return new ResultadoDetalleResponse(
				c.getIdResultadoCarga(),
				c.getIdGenerado(),
				c.getDocumentoIdentidad(),
				c.getNombrePaciente(),
				c.getMensaje(),
				c.getTelefono(),
				c.getTokenPublico(),
				trimBase() + "/r/" + c.getTokenPublico(),
				c.getEstadoSms() == null ? null : c.getEstadoSms().name(),
				c.getContadorVistas(),
				c.getEstado(),
				c.getFechaEliminacion(),
				c.getUsuarioEliminacion(),
				c.getMotivoEliminacion(),
				c.getCantidadArchivos() == null ? 0 : c.getCantidadArchivos(),
				c.getPesoTotalMb(),
				c.getFechaHoraCarga(),
				c.getFechaVigenciaHasta(),
				c.getFechaPrimeraVista(),
				c.getFechaUltimaVista(),
				null,
				archivos);
	}

	private ResultadoCarga buscar(Long id) {
		return resultadoCargaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Carga no encontrada: " + id));
	}

	private LocalDateTime inicioDia(LocalDate d) {
		return d == null ? null : d.atStartOfDay();
	}

	private LocalDateTime finDia(LocalDate d) {
		return d == null ? null : d.atTime(LocalTime.MAX);
	}

	private String trimBase() {
		String base = appProperties.getPublicBaseUrl();
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	/** Link al login que viaja en el SMS (config sms.message.login-url). */
	private String loginUrl() {
		String url = smsProperties.getMessage().getLoginUrl();
		return url == null ? "" : url.trim();
	}
}