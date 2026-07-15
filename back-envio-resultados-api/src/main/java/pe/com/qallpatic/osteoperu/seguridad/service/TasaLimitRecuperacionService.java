package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.LimiteIntentosSuperadoException;
import pe.com.qallpatic.osteoperu.config.RecuperacionProperties;
import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoRecuperacion;
import pe.com.qallpatic.osteoperu.seguridad.repository.IntentoRecuperacionRepository;

import java.time.LocalDateTime;

/**
 * Aplica las reglas de rate-limit al endpoint publico de recuperacion de
 * password. La persistencia en BD (no en memoria) garantiza que el limite
 * es compartido entre reinicios y posibles replicas del backend.
 *
 * Algoritmo: se inserta primero el intento actual y luego se cuentan los
 * intentos previos en la ventana. Si alguno de los umbrales se excede, el
 * intento actual se marca como bloqueado y se lanza 429 con el tiempo
 * restante hasta el intento mas antiguo (retryAfter).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TasaLimitRecuperacionService {

	private final IntentoRecuperacionRepository intentoRepository;
	private final RecuperacionProperties propiedades;

	/**
	 * Evalua si la dupla ip+telefono puede continuar. Inserta siempre un
	 * registro (exitoso o bloqueado) para mantener la auditoria consistente.
	 *
	 * @return segundos a esperar si fue bloqueado; 0 si pasa.
	 * @throws LimiteIntentosSuperadoException si se excede algun umbral.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public long evaluar(String ip, String telefonoNorm) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());

		long intentosIp = intentoRepository.contarPorIp(ip, desde);
		if (intentosIp >= propiedades.getMaxIntentosIp()) {
			registrarBloqueado(ip, telefonoNorm, "ip");
			throw limite("Se supero el limite de intentos desde esta IP. "
					+ "Intenta nuevamente en algunos minutos.");
		}

		long intentosTel = intentoRepository.contarPorTelefono(telefonoNorm, desde);
		if (intentosTel >= propiedades.getMaxIntentosTelefono()) {
			registrarBloqueado(ip, telefonoNorm, "telefono");
			throw limite("Se supero el limite de intentos para este telefono. "
					+ "Intenta nuevamente en algunos minutos.");
		}

		long distintosIp = intentoRepository.contarDistintosPorIp(ip, desde);
		if (distintosIp >= propiedades.getMaxDistintosPorIp()) {
			registrarBloqueado(ip, telefonoNorm, "distintos");
			throw limite("Se supero el limite de telefonos consultados desde esta IP. "
					+ "Intenta nuevamente en algunos minutos.");
		}

		registrarExitoso(ip, telefonoNorm);
		return 0L;
	}

	private void registrarExitoso(String ip, String telefonoNorm) {
		persistir(ip, telefonoNorm, false);
	}

	private void registrarBloqueado(String ip, String telefonoNorm, String motivo) {
		log.warn("[RECUPERACION] Bloqueado por '{}'. ip={}", motivo, ip);
		persistir(ip, telefonoNorm, true);
	}

	private void persistir(String ip, String telefonoNorm, boolean bloqueado) {
		IntentoRecuperacion intento = IntentoRecuperacion.builder()
				.ipAddress(ip == null ? "desconocida" : ip)
				.telefonoNorm(telefonoNorm)
				.bloqueado(bloqueado)
				.fechaRegistro(LocalDateTime.now())
				.build();
		intentoRepository.save(intento);
	}

	/**
	 * Calcula los segundos a esperar para que la ventana deslizante libere al
	 * menos un cupo, basandose en el intento mas reciente que entro a la
	 * cuenta. Los controllers usan este numero en la respuesta 429.
	 */
	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public long retryAfterSegundos(String ip) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());
		LocalDateTime masReciente = intentoRepository.masRecientePorIp(ip, desde);
		if (masReciente == null) {
			return 0L;
		}
		long segundos = java.time.Duration.between(masReciente, LocalDateTime.now()).getSeconds();
		long ventanaSeg = propiedades.getVentanaMinutos() * 60L;
		return Math.max(0L, ventanaSeg - segundos);
	}

	private LimiteIntentosSuperadoException limite(String mensaje) {
		return new LimiteIntentosSuperadoException(mensaje);
	}
}
