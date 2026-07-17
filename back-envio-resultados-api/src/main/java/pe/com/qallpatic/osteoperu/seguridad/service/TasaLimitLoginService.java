package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.LimiteIntentosSuperadoException;
import pe.com.qallpatic.osteoperu.config.LoginProperties;
import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoLogin;
import pe.com.qallpatic.osteoperu.seguridad.repository.IntentoLoginRepository;

import java.time.LocalDateTime;

/**
 * Rate-limit del endpoint publico /api/auth/login. Aplica reglas por IP y
 * por usuario, persistidas en BD para que el limite sea compartido entre
 * reinicios y replicas.
 *
 * La verificacion se hace ANTES de invocar el AuthenticationManager: si la
 * dupla ip+usuario esta bloqueada, devolvemos 429 sin delegar a Spring
 * Security (que devolveria 401 con el mismo delay para todos y permitiria
 * fuerza bruta con respuesta de tiempo constante).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TasaLimitLoginService {

	private final IntentoLoginRepository intentoRepository;
	private final LoginProperties propiedades;

	/**
	 * Inserta el intento y verifica si la dupla ip+usuario esta dentro de los
	 * limites. Si se supera algun umbral, lanza 429; en caso contrario el
	 * intento queda registrado como "no bloqueado".
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void evaluar(String ip, String usuario) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());
		String ipKey = ip == null ? "desconocida" : ip;
		String usuarioKey = usuario == null ? "" : usuario.trim();

		long intentosIp = intentoRepository.contarPorIp(ipKey, desde);
		if (intentosIp >= propiedades.getMaxIntentosIp()) {
			persistir(ipKey, usuarioKey, true);
			log.warn("[LOGIN] Bloqueado por IP. ip={}, intentos={}", ipKey, intentosIp);
			throw new LimiteIntentosSuperadoException(
					"Se supero el limite de intentos desde esta IP. "
							+ "Intenta nuevamente en algunos minutos.");
		}

		long intentosUsuario = intentoRepository.contarPorUsuario(usuarioKey, desde);
		if (intentosUsuario >= propiedades.getMaxIntentosUsuario()) {
			persistir(ipKey, usuarioKey, true);
			log.warn("[LOGIN] Bloqueado por usuario. usuario={}, intentos={}", usuarioKey, intentosUsuario);
			throw new LimiteIntentosSuperadoException(
					"Se supero el limite de intentos para este usuario. "
							+ "Intenta nuevamente en algunos minutos.");
		}

		persistir(ipKey, usuarioKey, false);
	}

	/**
	 * Segundos a esperar para que la ventana deslizante libere al menos un
	 * cupo, en base al intento mas reciente de la IP.
	 */
	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public long retryAfterSegundos(String ip) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());
		LocalDateTime masReciente = intentoRepository.masRecientePorIp(
				ip == null ? "desconocida" : ip, desde);
		if (masReciente == null) {
			return 0L;
		}
		long segundos = java.time.Duration.between(masReciente, LocalDateTime.now()).getSeconds();
		long ventanaSeg = propiedades.getVentanaMinutos() * 60L;
		return Math.max(0L, ventanaSeg - segundos);
	}

	private void persistir(String ip, String usuario, boolean bloqueado) {
		IntentoLogin intento = IntentoLogin.builder()
				.ipAddress(ip)
				.usuario(usuario)
				.bloqueado(bloqueado)
				.fechaRegistro(LocalDateTime.now())
				.build();
		intentoRepository.save(intento);
	}
}
