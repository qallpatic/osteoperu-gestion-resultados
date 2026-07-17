package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.config.LoginProperties;
import pe.com.qallpatic.osteoperu.seguridad.repository.IntentoLoginRepository;

import java.time.LocalDateTime;

/**
 * Purga periodica de intentos de login mas antiguos que la retencion
 * configurada. Mantiene la tabla en un tamano acotado sin perder la ventana
 * operacional (rate-limit) que es mucho menor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LimpiezaIntentosLoginService {

	private final IntentoLoginRepository intentoRepository;
	private final LoginProperties propiedades;

	@Scheduled(cron = "${login.limpieza-cron:0 0 * * * *}")
	@Transactional
	public void purgar() {
		LocalDateTime corte = LocalDateTime.now().minusHours(propiedades.getRetentionHoras());
		int eliminados = intentoRepository.eliminarAnterioresA(corte);
		if (eliminados > 0) {
			log.info("[LOGIN] Purgados {} intentos anteriores a {}", eliminados, corte);
		}
	}
}
