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
import java.util.Optional;

/**
 * Implementacion del flujo de recuperacion de password.
 *
 * Flujo:
 * <ol>
 *   <li>El front envia el DNI/CE del paciente.</li>
 *   <li>El back busca el paciente por DNI, recupera su telefono.</li>
 *   <li>Regenera una clave temporal, la hashea con BCrypt y envia SMS al
 *       telefono del paciente.</li>
 *   <li>Devuelve un mensaje generico + el telefono ofuscado (ultimos 4
 *       digitos) para que el usuario verifique sin comprometer la info.</li>
 * </ol>
 *
 * Seguridad:
 * <ul>
 *   <li>La respuesta es siempre generica (anti-enumeracion).</li>
 *   <li>El rate-limit se evalua antes de tocar BD (insert + 3 counts).</li>
 *   <li>El telefono se ofusca SIEMPRE: si no hay paciente, se ofusca el DNI
 *       tipeado (mismo formato) para no filtrar la longitud/estructura.</li>
 *   <li>El SMS se encola en {@link SmsService} (no bloquea el request).</li>
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
	public RecuperarPasswordResponse solicitar(String ip, String documento) {
		String documentoNorm = UtilTelefono.validarDocumentoIdentidad(documento);
		String telefonoOfuscado = ofuscar(documentoNorm);

		try {
			tasaLimit.evaluar(ip, documentoNorm);
		} catch (LimiteIntentosSuperadoException e) {
			long retry = tasaLimit.retryAfterSegundos(ip);
			return new RecuperarPasswordResponse(
					e.getMessage(),
					soporteTelefono(),
					telefonoOfuscado,
					retry);
		}

		Optional<String> telefonoParaSms = regenerarClaveSiAplica(documentoNorm);
		if (telefonoParaSms.isPresent()) {
			telefonoOfuscado = ofuscar(telefonoParaSms.get());
		}
		log.info("[RECUPERACION] ip={} documento=**** procesado={}", ip, telefonoParaSms.isPresent());
		return new RecuperarPasswordResponse(MENSAJE_GENERICO, soporteTelefono(), telefonoOfuscado, null);
	}

	/**
	 * Busca el paciente por DNI, deriva su USUARIO, genera una clave
	 * temporal, la hashea y encola el SMS. Devuelve el telefono normalizado
	 * (con prefijo 51) si el SMS fue encolado, o vacio si no.
	 */
	private Optional<String> regenerarClaveSiAplica(String documentoNorm) {
		List<Paciente> pacientes = pacienteRepository.findAllByDni(documentoNorm);
		if (pacientes.isEmpty()) {
			return Optional.empty();
		}
		Paciente paciente = pickActivo(pacientes);
		if (paciente == null || paciente.getIdUsuario() == null || !paciente.isEstado()) {
			return Optional.empty();
		}
		Usuario usuario = usuarioRepository.findById(paciente.getIdUsuario()).orElse(null);
		if (usuario == null || !usuario.isEstado()) {
			return Optional.empty();
		}
		if (paciente.getTelefono() == null || paciente.getTelefono().isBlank()) {
			log.info("[RECUPERACION] paciente sin telefono (dni={}); no se envia SMS", documentoNorm);
			return Optional.empty();
		}

		String clavePlana = generarClaveSinAmbiguos(recuperacionProperties.getLongitudClave());
		usuario.setContrasena(passwordEncoder.encode(clavePlana));
		usuario.setUsuarioModificacion("RECUPERACION_PASSWORD");
		usuarioRepository.save(usuario);

		String plantilla = smsProperties.getMessage().getRecuperacionTemplate();
		if (plantilla == null || plantilla.isBlank()) {
			log.warn("sms.message.recuperacion-template no configurada; no se envia SMS para {}", documentoNorm);
			return Optional.empty();
		}
		String telefonoNorm = UtilTelefono.normalizar(paciente.getTelefono(),
				appProperties.getPhone().getCountryCode());
		String mensaje = plantilla
				.replace("{nombre}", usuario.getNombre() == null ? "" : usuario.getNombre())
				.replace("{dni}", usuario.getDni() == null ? documentoNorm : usuario.getDni())
				.replace("{clave}", clavePlana)
				.replace("{link}", smsProperties.getMessage().getLoginUrl() == null
						? "" : smsProperties.getMessage().getLoginUrl())
				.trim();
		try {
			smsService.enviarTexto(telefonoNorm, mensaje);
			return Optional.of(telefonoNorm);
		} catch (Exception e) {
			log.warn("No se pudo encolar SMS de recuperacion para {}: {}", telefonoNorm, e.getMessage());
			return Optional.empty();
		}
	}

	private Paciente pickActivo(List<Paciente> pacientes) {
		for (Paciente p : pacientes) {
			if (p.isEstado() && p.getIdUsuario() != null) {
				return p;
			}
		}
		return pacientes.get(0);
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
	 * Devuelve los ultimos 4 digitos enmascarados con 5 bullets. Acepta
	 * documentos de 4+ digitos; si son menos, devuelve solo bullets.
	 */
	static String ofuscar(String doc) {
		if (doc == null) {
			return "•••••";
		}
		String limpio = doc.replaceAll("\\D", "");
		if (limpio.length() < 4) {
			return "•••••" + limpio;
		}
		return "•••••" + limpio.substring(limpio.length() - 4);
	}

	private String soporteTelefono() {
		String t = appProperties.getSoporte().getTelefono();
		return t == null ? "" : t.trim();
	}
}
