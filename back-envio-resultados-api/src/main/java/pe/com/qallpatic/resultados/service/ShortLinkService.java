package pe.com.qallpatic.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.resultados.config.AppProperties;
import pe.com.qallpatic.resultados.exception.NotFoundException;
import pe.com.qallpatic.resultados.model.ShortLink;
import pe.com.qallpatic.resultados.repository.ShortLinkRepository;

import java.time.LocalDateTime;

/** Acortador de links: crea codigos cortos y resuelve/contabiliza clicks. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkService {

	private final ShortLinkRepository shortLinkRepository;
	private final AppProperties appProperties;

	/** Crea un link corto que apunta a {@code originalUrl}. Devuelve la entidad persistida. */
	@Transactional
	public ShortLink create(String originalUrl, String type, Long batchId) {
		String code = generateUniqueCode();

		int ttlDays = appProperties.getShortlink().getTtlDays();
		LocalDateTime expiresAt = ttlDays > 0 ? LocalDateTime.now().plusDays(ttlDays) : null;

		ShortLink link = ShortLink.builder()
				.shortCode(code)
				.originalUrl(originalUrl)
				.type(type)
				.batchId(batchId)
				.clickCount(0)
				.createdAt(LocalDateTime.now())
				.expiresAt(expiresAt)
				.build();

		return shortLinkRepository.save(link);
	}

	/** Resuelve un codigo corto, incrementa su contador de clicks y devuelve el link. */
	@Transactional
	public ShortLink resolveAndCount(String code) {
		ShortLink link = shortLinkRepository.findByShortCode(code)
				.orElseThrow(() -> new NotFoundException("Link no encontrado o vencido."));

		if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new NotFoundException("El link ha expirado.");
		}

		link.setClickCount(link.getClickCount() + 1);
		return shortLinkRepository.save(link);
	}

	private String generateUniqueCode() {
		int length = appProperties.getShortlink().getCodeLength();
		String code;
		int attempts = 0;
		do {
			code = ShortCodeGenerator.generate(length);
			attempts++;
			if (attempts > 10) {
				length++; // ante colisiones repetidas, aumentar longitud
			}
		} while (shortLinkRepository.existsByShortCode(code));
		return code;
	}
}
