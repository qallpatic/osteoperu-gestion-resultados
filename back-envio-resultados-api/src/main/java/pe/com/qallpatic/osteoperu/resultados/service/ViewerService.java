package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.excepcion.ResultadoCaducadoException;
import pe.com.qallpatic.osteoperu.resultados.entity.EventoVista;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento;
import pe.com.qallpatic.osteoperu.resultados.repository.EventoVistaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;

import java.time.LocalDateTime;

/** Logica del visor de resultados: registra vistas/clicks y marca "visto". */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewerService {

	private final ResultadoCargaRepository resultadoCargaRepository;
	private final EventoVistaRepository eventoVistaRepository;

	@Transactional
	public void registerClick(Long idResultado, String codigoCorto, String ip, String userAgent) {
		eventoVistaRepository.save(EventoVista.builder()
				.idResultado(idResultado)
				.codigoCorto(codigoCorto)
				.tipoEvento(TipoEvento.CLICK)
				.ip(ip)
				.userAgent(userAgent)
				.fechaCreacion(LocalDateTime.now())
				.build());
	}

	@Transactional
	public ResultadoCarga loadAndRegisterView(String token, String ip, String userAgent) {
		ResultadoCarga carga = resultadoCargaRepository.findByTokenPublico(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultados no encontrados."));
		assertVisible(carga);
		applyViewCountersAndEvent(carga, ip, userAgent, LocalDateTime.now());
		return carga;
	}

	/**
	 * Idem {@link #loadAndRegisterView} pero a partir del ID interno de la cabecera
	 * (usado por el portal del paciente cuando hace click en "Ver archivo": el
	 * paciente ya paso por las validaciones de autorizacion del portal, pero
	 * igualmente se valida estado activo y vigencia para no inflar contadores
	 * de cabeceras soft-deleted o caducadas).
	 */
	@Transactional
	public void registerView(Long idResultado, String ip, String userAgent) {
		ResultadoCarga carga = resultadoCargaRepository.findById(idResultado)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultados no encontrados."));
		assertVisible(carga);
		applyViewCountersAndEvent(carga, ip, userAgent, LocalDateTime.now());
	}

	private void assertVisible(ResultadoCarga carga) {
		if (carga.getEstado() == null || carga.getEstado() != 1) {
			throw new RecursoNoEncontradoException("Resultados no encontrados.");
		}
		if (carga.getFechaVigenciaHasta() != null && carga.getFechaVigenciaHasta().isBefore(LocalDateTime.now())) {
			throw new ResultadoCaducadoException("Resultados caducados.", carga.getDocumentoIdentidad());
		}
	}

	/**
	 * Logica comun: incrementa CONTADOR_VISTAS, setea FECHA_PRIMERA_VISTA si
	 * era null, actualiza FECHA_ULTIMA_VISTA y persiste un EVENTO_VISTA.
	 * Usada por el visor Thymeleaf publico ({@link #loadAndRegisterView}) y
	 * por el portal del paciente ({@link #registerView}).
	 */
	private void applyViewCountersAndEvent(ResultadoCarga carga, String ip, String userAgent, LocalDateTime now) {
		carga.setContadorVistas(carga.getContadorVistas() + 1);
		if (carga.getFechaPrimeraVista() == null) {
			carga.setFechaPrimeraVista(now);
			log.info("Paciente vio sus resultados por primera vez. carga={}", carga.getIdResultadoCarga());
		}
		carga.setFechaUltimaVista(now);
		resultadoCargaRepository.save(carga);

		eventoVistaRepository.save(EventoVista.builder()
				.idResultado(carga.getIdResultadoCarga())
				.tipoEvento(TipoEvento.VISTA)
				.ip(ip)
				.userAgent(userAgent)
				.fechaCreacion(now)
				.build());
	}

	@Transactional(readOnly = true)
	public ResultadoCarga getByToken(String token) {
		ResultadoCarga carga = resultadoCargaRepository.findByTokenPublico(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultados no encontrados."));
		if (carga.getEstado() == null || carga.getEstado() != 1) {
			throw new RecursoNoEncontradoException("Resultados no encontrados.");
		}
		if (carga.getFechaVigenciaHasta() != null && carga.getFechaVigenciaHasta().isBefore(LocalDateTime.now())) {
			throw new ResultadoCaducadoException("Resultados caducados.", carga.getDocumentoIdentidad());
		}
		return carga;
	}

	@Transactional
	public void registerDownload(Long idResultado, String ip, String userAgent) {
		eventoVistaRepository.save(EventoVista.builder()
				.idResultado(idResultado)
				.tipoEvento(TipoEvento.DESCARGA)
				.ip(ip)
				.userAgent(userAgent)
				.fechaCreacion(LocalDateTime.now())
				.build());
	}

	public ResultadoArchivo findFile(ResultadoCarga carga, Long fileId) {
		return carga.getArchivos().stream()
				.filter(a -> a.getIdResultadoArchivo().equals(fileId))
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado."));
	}
}
