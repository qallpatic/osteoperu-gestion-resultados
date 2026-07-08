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
import pe.com.qallpatic.osteoperu.integracion.reniec.ClienteReniec;
import pe.com.qallpatic.osteoperu.integracion.reniec.DatosReniec;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.NombreReniecResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;
import pe.com.qallpatic.osteoperu.resultados.mapper.ResultadoMapper;
import pe.com.qallpatic.osteoperu.resultados.repository.EnlaceCortoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.LogCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoRepository;
import pe.com.qallpatic.osteoperu.resultados.spec.LogCargaSpecs;
import pe.com.qallpatic.osteoperu.resultados.spec.ResultadoSpecs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultaResultadoServiceImpl implements ConsultaResultadoService {

	private final ResultadoRepository resultadoRepository;
	private final LogCargaRepository logCargaRepository;
	private final EnlaceCortoRepository enlaceCortoRepository;
	private final ResultadoMapper resultadoMapper;
	private final SmsService smsService;
	private final ClienteReniec clienteReniec;
	private final PacienteRepository pacienteRepository;
	private final AppProperties appProperties;

	@Override
	@Transactional(readOnly = true)
	public NombreReniecResponse consultarReniec(String dni) {
		String d = UtilTelefono.validarDni(dni);
		String nombre = clienteReniec.consultar(d).map(DatosReniec::nombreCompleto).orElse(null);
		return new NombreReniecResponse(d, nombre);
	}

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
				Sort.by(Sort.Direction.DESC, "fechaCreacion"));
		Page<ResultadoResumenResponse> page = resultadoRepository
				.findAll(ResultadoSpecs.filtro(dni, nombre, inicioDia(desde), finDia(hasta)), pageable)
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
		Resultado resultado = buscar(id);

		if (solicitud.telefono() != null && !solicitud.telefono().isBlank()) {
			resultado.setTelefono(UtilTelefono.normalizar(solicitud.telefono(),
					appProperties.getPhone().getCountryCode()));
		}
		if (solicitud.nombrePaciente() != null) {
			resultado.setNombrePaciente(
					solicitud.nombrePaciente().isBlank() ? null : solicitud.nombrePaciente().trim());
		}
		resultado.setUsuarioModificacion(actor);
		resultadoRepository.save(resultado);

		if (solicitud.reenviarSms()) {
			enlaceCortoRepository.findFirstByIdResultadoOrderByIdDesc(id).ifPresent(enlace -> {
				String smsLink = trimBase() + "/s/" + enlace.getCodigoCorto();
				smsService.enqueueResultLink(id, resultado.getTelefono(), resultado.getNombrePaciente(), smsLink);
				log.info("SMS reencolado para resultado {}", id);
			});
		}

		return armarDetalle(resultado);
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

	private ResultadoDetalleResponse armarDetalle(Resultado r) {
		List<ArchivoResponse> archivos = r.getArchivos().stream()
				.map(resultadoMapper::toArchivo)
				.toList();
		return new ResultadoDetalleResponse(
				r.getId(),
				r.getDni(),
				r.getNombrePaciente(),
				r.getMensaje(),
				r.getTelefono(),
				r.getCarpeta(),
				r.getTokenPublico(),
				trimBase() + "/r/" + r.getTokenPublico(),
				r.getEstadoSms() == null ? null : r.getEstadoSms().name(),
				r.getContadorVistas(),
				r.getFechaPrimeraVista(),
				r.getFechaUltimaVista(),
				r.getFechaCreacion(),
				archivos);
	}

	private Resultado buscar(Long id) {
		return resultadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultado no encontrado: " + id));
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
}
