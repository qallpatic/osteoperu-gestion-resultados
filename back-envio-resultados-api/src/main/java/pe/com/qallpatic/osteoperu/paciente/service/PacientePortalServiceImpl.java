package pe.com.qallpatic.osteoperu.paciente.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.paciente.dto.ArchivoLocalizado;
import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.mapper.ResultadoMapper;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.service.AlmacenamientoArchivos;
import pe.com.qallpatic.osteoperu.resultados.service.ArchivoDescargable;
import pe.com.qallpatic.osteoperu.resultados.spec.ResultadoCargaSpecs;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacientePortalServiceImpl implements PacientePortalService {

	private final ResultadoCargaRepository resultadoCargaRepository;
	private final ResultadoMapper resultadoMapper;
	private final AlmacenamientoArchivos almacenamiento;
	private final UsuarioRepository usuarioRepository;
	private final AppProperties appProperties;

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<ResultadoResumenResponse> misResultados(String username, boolean esAdmin,
			LocalDate desde, LocalDate hasta, int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano,
				Sort.by(Sort.Direction.DESC, "fechaHoraCarga"));

		Specification<ResultadoCarga> spec = esAdmin
				? ResultadoCargaSpecs.filtro(null, null, 1, inicioDia(desde), finDia(hasta))
				: ResultadoCargaSpecs.delPacienteActivo(resolverDni(username), inicioDia(desde), finDia(hasta));

		Page<ResultadoResumenResponse> page = resultadoCargaRepository.findAll(spec, pageable)
				.map(resultadoMapper::toResumen);
		return PaginaResponse.de(page);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultadoDetalleResponse detalle(String username, boolean esAdmin, Long id) {
		return armarDetalle(obtenerPorId(id, username, esAdmin));
	}

	@Override
	@Transactional(readOnly = true)
	public ResultadoDetalleResponse detallePorIdGenerado(String username, boolean esAdmin, String idGenerado) {
		return armarDetalle(obtenerPorIdGenerado(idGenerado, username, esAdmin));
	}

	@Override
	@Transactional(readOnly = true)
	public ArchivoLocalizado localizarArchivo(String username, boolean esAdmin, Long idResultadoCarga, Long idArchivo) {
		ResultadoCarga carga = obtenerPorId(idResultadoCarga, username, esAdmin);
		ResultadoArchivo archivo = carga.getArchivos().stream()
				.filter(a -> a.getIdResultadoArchivo().equals(idArchivo))
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado."));
		return new ArchivoLocalizado(
				archivo.getIdGenerado(),
				archivo.getIdArchivo(),
				archivo.getExtension(),
				archivo.getNombreOriginal(),
				carga.getIdResultadoCarga());
	}

	@Override
	@Transactional(readOnly = true)
	public ArchivoDescargable descargarArchivo(String username, boolean esAdmin, Long idResultadoCarga, Long idArchivo) {
		ResultadoCarga carga = obtenerPorId(idResultadoCarga, username, esAdmin);
		ResultadoArchivo archivo = carga.getArchivos().stream()
				.filter(a -> a.getIdResultadoArchivo().equals(idArchivo))
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado."));
		return almacenamiento.descargar(
				archivo.getIdGenerado(), archivo.getIdArchivo(), archivo.getExtension(),
				carga.getDocumentoIdentidad(), carga.getFechaHoraCarga());
	}

	@Override
	@Transactional
	public void softDelete(String username, boolean esAdmin, Long idResultadoCarga, String motivo) {
		ResultadoCarga carga = obtenerPorId(idResultadoCarga, username, esAdmin);
		carga.setEstado(0);
		carga.setFechaEliminacion(LocalDateTime.now());
		carga.setUsuarioEliminacion(username);
		carga.setMotivoEliminacion(motivo);
		resultadoCargaRepository.save(carga);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal pesoTotalCabecera(String idGenerado) {
		return resultadoCargaRepository.findByIdGenerado(idGenerado)
				.map(ResultadoCarga::getPesoTotalMb)
				.orElse(BigDecimal.ZERO);
	}

	private ResultadoCarga obtenerPorId(Long id, String username, boolean esAdmin) {
		ResultadoCarga carga = resultadoCargaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Carga no encontrada."));
		if (!esAdmin && !carga.getEstado().equals(1)) {
			throw new RecursoNoEncontradoException("Carga no encontrada.");
		}
		if (!esAdmin && !resolverDni(username).equals(carga.getDocumentoIdentidad())) {
			throw new RecursoNoEncontradoException("Carga no encontrada.");
		}
		return carga;
	}

	private ResultadoCarga obtenerPorIdGenerado(String idGenerado, String username, boolean esAdmin) {
		ResultadoCarga carga = resultadoCargaRepository.findByIdGenerado(idGenerado)
				.orElseThrow(() -> new RecursoNoEncontradoException("Carga no encontrada."));
		if (!esAdmin && !carga.getEstado().equals(1)) {
			throw new RecursoNoEncontradoException("Carga no encontrada.");
		}
		if (!esAdmin && !resolverDni(username).equals(carga.getDocumentoIdentidad())) {
			throw new RecursoNoEncontradoException("Carga no encontrada.");
		}
		return carga;
	}

	private String resolverDni(String username) {
		return usuarioRepository.findByUsuario(username)
				.map(Usuario::getDni)
				.filter(d -> d != null && !d.isBlank())
				.orElse(username);
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
