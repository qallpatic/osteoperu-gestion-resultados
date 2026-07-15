package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.config.RecuperacionProperties;
import pe.com.qallpatic.osteoperu.seguridad.repository.IntentoRecuperacionRepository;

import java.time.LocalDateTime;

/**
 * Purga periodica de intentos mas antiguos que la retencion configurada.
 * Mantiene la tabla en un tamano acotado sin perder la ventana operacional
 * (rate-limit) que es mucho menor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LimpiezaIntentosRecuperacionService {

	private final IntentoRecuperacionRepository intentoRepository;
	private final RecuperacionProperties propiedades;

	@Scheduled(cron = "${recuperacion.limpieza-cron:0 0 * * * *}")
	@Transactional
	public void purgar() {
		LocalDateTime corte = LocalDateTime.now().minusHours(propiedades.getRetentionHoras());
		int eliminados = intentoRepository.eliminarAnterioresA(corte);
		if (eliminados > 0) {
			log.info("[RECUPERACION] Purgados {} intentos anteriores a {}", eliminados, corte);
		}
	}
}
