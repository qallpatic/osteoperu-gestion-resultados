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
		if (carga.getEstado() == null || carga.getEstado() != 1) {
			throw new RecursoNoEncontradoException("Resultados no encontrados.");
		}
		if (carga.getFechaVigenciaHasta() != null && carga.getFechaVigenciaHasta().isBefore(LocalDateTime.now())) {
			throw new ResultadoCaducadoException("Resultados caducados.", carga.getDocumentoIdentidad());
		}

		LocalDateTime now = LocalDateTime.now();
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

		return carga;
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
