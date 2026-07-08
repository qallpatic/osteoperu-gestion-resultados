package pe.com.qallpatic.osteoperu.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.resultados.entity.EnlaceCorto;
import pe.com.qallpatic.osteoperu.resultados.repository.EnlaceCortoRepository;

import java.time.LocalDateTime;

/** Acortador de links: crea codigos cortos y resuelve/contabiliza clicks. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkService {

	private final EnlaceCortoRepository enlaceCortoRepository;
	private final AppProperties appProperties;

	/** Crea un link corto que apunta a {@code urlOriginal}. Devuelve la entidad persistida. */
	@Transactional
	public EnlaceCorto create(String urlOriginal, String tipo, Long idResultado) {
		String code = generateUniqueCode();

		int ttlDays = appProperties.getShortlink().getTtlDays();
		LocalDateTime expira = ttlDays > 0 ? LocalDateTime.now().plusDays(ttlDays) : null;

		EnlaceCorto enlace = EnlaceCorto.builder()
				.codigoCorto(code)
				.urlOriginal(urlOriginal)
				.tipo(tipo)
				.idResultado(idResultado)
				.contadorClicks(0)
				.fechaCreacion(LocalDateTime.now())
				.fechaExpiracion(expira)
				.build();

		return enlaceCortoRepository.save(enlace);
	}

	/** Resuelve un codigo corto, incrementa su contador de clicks y devuelve el enlace. */
	@Transactional
	public EnlaceCorto resolveAndCount(String code) {
		EnlaceCorto enlace = enlaceCortoRepository.findByCodigoCorto(code)
				.orElseThrow(() -> new RecursoNoEncontradoException("Link no encontrado o vencido."));

		if (enlace.getFechaExpiracion() != null && enlace.getFechaExpiracion().isBefore(LocalDateTime.now())) {
			throw new RecursoNoEncontradoException("El link ha expirado.");
		}

		enlace.setContadorClicks(enlace.getContadorClicks() + 1);
		return enlaceCortoRepository.save(enlace);
	}

	private String generateUniqueCode() {
		int length = appProperties.getShortlink().getCodeLength();
		String code;
		int attempts = 0;
		do {
			code = GeneradorCodigos.generar(length);
			attempts++;
			if (attempts > 10) {
				length++; // ante colisiones repetidas, aumentar longitud
			}
		} while (enlaceCortoRepository.existsByCodigoCorto(code));
		return code;
	}
}
