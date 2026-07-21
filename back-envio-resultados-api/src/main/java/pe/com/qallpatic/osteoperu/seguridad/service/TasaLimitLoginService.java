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

@Service
@Slf4j
@RequiredArgsConstructor
public class TasaLimitLoginService {

	private final IntentoLoginRepository intentoRepository;
	private final LoginProperties propiedades;

	@Transactional(propagation = Propagation.REQUIRES_NEW,
			noRollbackFor = LimiteIntentosSuperadoException.class)
	public void evaluar(String ip, String usuario) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());
		String ipKey = ip == null ? "desconocida" : ip;
		String usuarioKey = usuario == null ? "" : usuario.trim();

		long intentosIpUsuario = intentoRepository.contarFallidosPorIpYUsuario(ipKey, usuarioKey, desde);
		if (intentosIpUsuario >= propiedades.getMaxIntentosIp()) {
			persistir(ipKey, usuarioKey, true);
			log.warn("[LOGIN] Bloqueado por IP y usuario. ip={}, usuario={}, intentos={}",
					ipKey, usuarioKey, intentosIpUsuario);
			throw new LimiteIntentosSuperadoException(
					"Se supero el limite de intentos para este usuario. "
							+ "Intenta nuevamente en algunos minutos.");
		}

		long intentosUsuario = intentoRepository.contarFallidosPorUsuario(usuarioKey, desde);
		if (intentosUsuario >= propiedades.getMaxIntentosUsuario()) {
			persistir(ipKey, usuarioKey, true);
			log.warn("[LOGIN] Bloqueado por usuario. usuario={}, intentos={}", usuarioKey, intentosUsuario);
			throw new LimiteIntentosSuperadoException(
					"Se supero el limite de intentos para este usuario. "
							+ "Intenta nuevamente en algunos minutos.");
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarFallo(String ip, String usuario) {
		persistir(ip == null ? "desconocida" : ip, usuario == null ? "" : usuario.trim(), false);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public long retryAfterSegundos(String ip, String usuario) {
		LocalDateTime desde = LocalDateTime.now().minusMinutes(propiedades.getVentanaMinutos());
		LocalDateTime masReciente = intentoRepository.masRecienteFallidoPorIpYUsuario(
				ip == null ? "desconocida" : ip,
				usuario == null ? "" : usuario.trim(),
				desde);
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
