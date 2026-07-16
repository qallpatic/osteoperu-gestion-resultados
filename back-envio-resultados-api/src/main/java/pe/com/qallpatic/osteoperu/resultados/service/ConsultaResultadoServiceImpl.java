package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.mapper.ResultadoMapper;
import pe.com.qallpatic.osteoperu.resultados.repository.EnlaceCortoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.LogCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.spec.LogCargaSpecs;
import pe.com.qallpatic.osteoperu.resultados.spec.ResultadoCargaSpecs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultaResultadoServiceImpl implements ConsultaResultadoService {

	private final ResultadoCargaRepository resultadoCargaRepository;
	private final LogCargaRepository logCargaRepository;
	private final EnlaceCortoRepository enlaceCortoRepository;
	private final ResultadoMapper resultadoMapper;
	private final SmsService smsService;
	private final PacienteRepository pacienteRepository;
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

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<ResultadoResumenResponse> listar(String dni, String nombre,
			LocalDate desde, LocalDate hasta, int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano,
				Sort.by(Sort.Direction.DESC, "fechaHoraCarga"));
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
	@Transactional(readOnly = true)
	public PaginaResponse<LogCargaResponse> listarLogs(String dni, LocalDate desde, LocalDate hasta,
			int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano,
				Sort.by(Sort.Direction.DESC, "fechaCreacion"));
		Page<LogCargaResponse> page = logCargaRepository
				.findAll(LogCargaSpecs.filtro(dni, inicioDia(desde), finDia(hasta)), pageable)
				.map(resultadoMapper::toLog);
		return PaginaResponse.de(page);
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
				c.getFechaPrimeraVista(),
				c.getFechaUltimaVista(),
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
