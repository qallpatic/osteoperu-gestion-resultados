package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.resultados.entity.ArchivoResultado;
import pe.com.qallpatic.osteoperu.resultados.entity.EventoVista;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento;
import pe.com.qallpatic.osteoperu.resultados.repository.EventoVistaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoRepository;

import java.time.LocalDateTime;

/** Logica del visor de resultados: registra vistas/clicks y marca "visto". */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewerService {

	private final ResultadoRepository resultadoRepository;
	private final EventoVistaRepository eventoVistaRepository;

	/** Registra un CLICK (apertura del link corto) asociado al resultado. */
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

	/**
	 * Carga el resultado por token, registra la VISTA, incrementa el contador y
	 * marca la primera vez que el paciente vio sus resultados.
	 */
	@Transactional
	public Resultado loadAndRegisterView(String token, String ip, String userAgent) {
		Resultado resultado = resultadoRepository.findByTokenPublico(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultados no encontrados."));

		LocalDateTime now = LocalDateTime.now();
		resultado.setContadorVistas(resultado.getContadorVistas() + 1);
		if (resultado.getFechaPrimeraVista() == null) {
			resultado.setFechaPrimeraVista(now);
			log.info("Paciente vio sus resultados por primera vez. resultado={}", resultado.getId());
		}
		resultado.setFechaUltimaVista(now);
		resultadoRepository.save(resultado);

		eventoVistaRepository.save(EventoVista.builder()
				.idResultado(resultado.getId())
				.tipoEvento(TipoEvento.VISTA)
				.ip(ip)
				.userAgent(userAgent)
				.fechaCreacion(now)
				.build());

		return resultado;
	}

	/** Devuelve el resultado por token sin registrar vista (para servir archivos). */
	@Transactional(readOnly = true)
	public Resultado getByToken(String token) {
		return resultadoRepository.findByTokenPublico(token)
				.orElseThrow(() -> new RecursoNoEncontradoException("Resultados no encontrados."));
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

	public ArchivoResultado findFile(Resultado resultado, Long fileId) {
		return resultado.getArchivos().stream()
				.filter(a -> a.getId().equals(fileId))
				.findFirst()
				.orElseThrow(() -> new RecursoNoEncontradoException("Archivo no encontrado."));
	}
}
