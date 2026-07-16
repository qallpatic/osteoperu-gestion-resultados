package pe.com.qallpatic.osteoperu.seguridad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.excepcion.LimiteIntentosSuperadoException;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.config.RecuperacionProperties;
import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.service.SmsService;
import pe.com.qallpatic.osteoperu.seguridad.dto.RecuperarPasswordResponse;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.util.List;

/**
 * Implementacion del flujo de recuperacion de password.
 *
 * Seguridad y comportamiento:
 * <ul>
 *   <li>La respuesta siempre es generica para evitar filtrar si el telefono
 *       esta registrado (anti-enumeracion).</li>
 *   <li>El rate-limit se evalua antes de tocar BD (insert + 3 counts).</li>
 *   <li>La clave se regenera y hashea con BCrypt; el hasheado previo se pierde.</li>
 *   <li>El SMS se encola en {@link SmsService} (no bloquea el hilo del request).</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioRecuperacionPasswordImpl implements ServicioRecuperacionPassword {

	private static final String MENSAJE_GENERICO =
			"Si tu numero esta registrado, recibiras un SMS con tu clave en breve.";

	private final PacienteRepository pacienteRepository;
	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final SmsService smsService;
	private final SmsProperties smsProperties;
	private final AppProperties appProperties;
	private final RecuperacionProperties recuperacionProperties;
	private final TasaLimitRecuperacionService tasaLimit;

	@Override
	@Transactional
	public RecuperarPasswordResponse solicitar(String ip, String telefono) {
		String telefonoNorm = UtilTelefono.normalizar(telefono, appProperties.getPhone().getCountryCode());

		try {
			tasaLimit.evaluar(ip, telefonoNorm);
		} catch (LimiteIntentosSuperadoException e) {
			long retry = tasaLimit.retryAfterSegundos(ip);
			return new RecuperarPasswordResponse(
					e.getMessage(),
					soporteTelefono(),
					retry);
		}

		boolean procesado = regenerarClaveSiAplica(telefonoNorm);
		log.info("[RECUPERACION] ip={} telefono=**** procesado={}", ip, procesado);
		return new RecuperarPasswordResponse(MENSAJE_GENERICO, soporteTelefono(), null);
	}

	/**
	 * Busca el paciente por telefono, deriva su USUARIO, genera una clave
	 * temporal, la hashea y encola el SMS. Devuelve true si se encolo el envio.
	 */
	private boolean regenerarClaveSiAplica(String telefonoNorm) {
		Paciente paciente = buscarPacientePorTelefono(telefonoNorm);
		if (paciente == null || paciente.getIdUsuario() == null || !paciente.isEstado()) {
			return false;
		}
		Usuario usuario = usuarioRepository.findById(paciente.getIdUsuario()).orElse(null);
		if (usuario == null || !usuario.isEstado()) {
			return false;
		}

		String clavePlana = generarClaveSinAmbiguos(recuperacionProperties.getLongitudClave());
		usuario.setContrasena(passwordEncoder.encode(clavePlana));
		usuario.setUsuarioModificacion("RECUPERACION_PASSWORD");
		usuarioRepository.save(usuario);

		String plantilla = smsProperties.getMessage().getRecuperacionTemplate();
		if (plantilla == null || plantilla.isBlank()) {
			log.warn("sms.message.recuperacion-template no configurada; no se envia SMS para {}", telefonoNorm);
			return false;
		}
		String mensaje = plantilla
				.replace("{nombre}", usuario.getNombre() == null ? "" : usuario.getNombre())
				.replace("{clave}", clavePlana)
				.replace("{link}", smsProperties.getMessage().getLoginUrl() == null
						? "" : smsProperties.getMessage().getLoginUrl())
				.trim();
		try {
			smsService.enviarTexto(telefonoNorm, mensaje);
			return true;
		} catch (Exception e) {
			log.warn("No se pudo encolar SMS de recuperacion para {}: {}", telefonoNorm, e.getMessage());
			return false;
		}
	}

	/**
	 * Genera una clave alfanumerica en mayusculas excluyendo caracteres ambiguos
	 * (0/O y 1/I) para reducir errores de transcripcion al recibir el SMS.
	 */
	private String generarClaveSinAmbiguos(int longitud) {
		String base = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
		StringBuilder sb = new StringBuilder(longitud);
		java.security.SecureRandom rnd = new java.security.SecureRandom();
		for (int i = 0; i < longitud; i++) {
			sb.append(base.charAt(rnd.nextInt(base.length())));
		}
		return sb.toString();
	}

	/**
	 * Tolerante al formato guardado: PACIENTE.TELEFONO historicamente se
	 * almaceno sin prefijo pais (ej. "980348748"), mientras que
	 * RESULTADO_CARGA.TELEFONO ya esta normalizado ("51980348748"). Probamos
	 * ambas variantes para no devolver 404 implicito en pacientes existentes.
	 *
	 * Si hay multiples pacientes con el mismo telefono (caso real, gente que
	 * comparte numero), elegimos el primero cuyo USUARIO este activo y asi
	 * regeneramos solo una cuenta; si ninguno califica, no se regenera clave.
	 */
	private Paciente buscarPacientePorTelefono(String telefonoNorm) {
		List<Paciente> conPrefijo = pacienteRepository.findByTelefono(telefonoNorm);
		Paciente candidato = pickActivo(conPrefijo);
		if (candidato != null) {
			return candidato;
		}
		String sinPrefijo = telefonoNorm.length() > 9 && telefonoNorm.startsWith("51")
				? telefonoNorm.substring(2)
				: telefonoNorm;
		List<Paciente> sinPrefijoLista = pacienteRepository.findByTelefono(sinPrefijo);
		return pickActivo(sinPrefijoLista);
	}

	private Paciente pickActivo(List<Paciente> pacientes) {
		if (pacientes == null || pacientes.isEmpty()) {
			return null;
		}
		for (Paciente p : pacientes) {
			if (p.isEstado() && p.getIdUsuario() != null) {
				return p;
			}
		}
		return pacientes.get(0);
	}

	private String soporteTelefono() {
		String t = appProperties.getSoporte().getTelefono();
		return t == null ? "" : t.trim();
	}
}
