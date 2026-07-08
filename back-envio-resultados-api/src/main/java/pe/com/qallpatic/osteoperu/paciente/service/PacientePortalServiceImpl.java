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
import pe.com.qallpatic.osteoperu.resultados.entity.ArchivoResultado;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;
import pe.com.qallpatic.osteoperu.resultados.mapper.ResultadoMapper;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoRepository;
import pe.com.qallpatic.osteoperu.resultados.service.StorageService;
import pe.com.qallpatic.osteoperu.resultados.spec.ResultadoSpecs;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacientePortalServiceImpl implements PacientePortalService {

	private final ResultadoRepository resultadoRepository;
	private final ResultadoMapper resultadoMapper;
	private final StorageService storageService;
	private final UsuarioRepository usuarioRepository;
	private final AppProperties appProperties;

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<ResultadoResumenResponse> misResultados(String username, boolean esAdmin,
			LocalDate desde, LocalDate hasta, int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano,
				Sort.by(Sort.Direction.DESC, "fechaCreacion"));

		// ADMIN ve todos los resultados; el paciente solo los de su DNI.
		Specification<Resultado> spec = esAdmin
				? ResultadoSpecs.filtro(null, null, inicioDia(desde), finDia(hasta))
				: ResultadoSpecs.delPaciente(resolverDni(username), inicioDia(desde), finDia(hasta));

		Page<ResultadoResumenResponse> page = resultadoRepository.findAll(spec, pageable)
				.map(resultadoMapper::toResumen);
		return PaginaResponse.de(page);
	}

	@Override
	@Transactional(readOnly = true)
	public ResultadoDetalleResponse detalle(String username, boolean esAdmin, Long id) {
		return armarDetalle(obtener(id, username, esAdmin));
	}

	@Override
	@Transactional(readOnly = true)
	public ArchivoLocalizado localizarArchivo(String username, boolean esAdmin, Long idResultado, Long idArchivo) {
		Resultado resultado = obtener(idResultado, username, esAdmin);
		ArchivoResultado archivo = resultado.getArchivos().stream()
				.filter(a -> a.getId().equals(idArchivo))
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado."));
		Path ruta = storageService.resolveFile(resultado.getCarpeta(), archivo.getNombreAlmacenado());
		return new ArchivoLocalizado(ruta, archivo.getNombreOriginal(), resultado.getId());
	}

	/** Carga el resultado; si no es ADMIN, verifica que pertenezca al DNI del paciente. */
	private Resultado obtener(Long id, String username, boolean esAdmin) {
		Resultado resultado = resultadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultado no encontrado."));
		if (!esAdmin && !resolverDni(username).equals(resultado.getDni())) {
			// No se revela la existencia de resultados ajenos.
			throw new RecursoNoEncontradoException("Resultado no encontrado.");
		}
		return resultado;
	}

	private String resolverDni(String username) {
		return usuarioRepository.findByUsuario(username)
				.map(Usuario::getDni)
				.filter(d -> d != null && !d.isBlank())
				.orElse(username);
	}

	private ResultadoDetalleResponse armarDetalle(Resultado r) {
		List<ArchivoResponse> archivos = r.getArchivos().stream()
				.map(resultadoMapper::toArchivo)
				.toList();
		return new ResultadoDetalleResponse(
				r.getId(), r.getDni(), r.getNombrePaciente(), r.getMensaje(), r.getTelefono(), r.getCarpeta(),
				r.getTokenPublico(), trimBase() + "/r/" + r.getTokenPublico(),
				r.getEstadoSms() == null ? null : r.getEstadoSms().name(),
				r.getContadorVistas(), r.getFechaPrimeraVista(), r.getFechaUltimaVista(),
				r.getFechaCreacion(), archivos);
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
