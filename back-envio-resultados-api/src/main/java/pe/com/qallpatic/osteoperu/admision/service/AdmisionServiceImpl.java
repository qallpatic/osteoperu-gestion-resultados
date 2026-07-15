package pe.com.qallpatic.osteoperu.admision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.admision.dto.CredencialResponse;
import pe.com.qallpatic.osteoperu.admision.dto.GenerarUsuarioRequest;
import pe.com.qallpatic.osteoperu.admision.dto.PacienteAdmisionResponse;
import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.util.GeneradorCodigos;
import pe.com.qallpatic.osteoperu.compartido.util.NombreParser;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.config.AppProperties;
import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.integracion.reniec.ClienteReniec;
import pe.com.qallpatic.osteoperu.integracion.reniec.DatosReniec;
import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;
import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.resultados.service.SmsService;
import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.seguridad.repository.RolRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;
import pe.com.qallpatic.osteoperu.usuarios.exception.UsuarioYaExisteException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AdmisionServiceImpl implements AdmisionService {

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PacienteRepository pacienteRepository;
	private final ResultadoCargaRepository resultadoCargaRepository;
	private final PasswordEncoder passwordEncoder;
	private final ClienteReniec clienteReniec;
	private final SmsService smsService;
	private final SmsProperties smsProperties;
	private final AppProperties appProperties;

	@Override
	@Transactional(readOnly = true)
	public PaginaResponse<PacienteAdmisionResponse> listarPacientes(String dni, int pagina, int tamano) {
		Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano <= 0 ? 10 : tamano);
		// Desde PACIENTE: incluye a los creados por Admision aunque aun no tengan resultados.
		Page<PacienteResumenProj> page = pacienteRepository.resumenDesdePacientes(
				(dni == null || dni.isBlank()) ? null : dni.trim(), pageable);

		List<PacienteAdmisionResponse> contenido = page.getContent().stream()
				.map(p -> new PacienteAdmisionResponse(
						p.getDni(),
						p.getNombre(),
						p.getTelefono(),
						p.getTotalResultados() == null ? 0 : p.getTotalResultados(),
						p.getUltimaFecha(),
						estadoCuenta(p.getDni())))
				.toList();

		return new PaginaResponse<>(contenido, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages(), page.isLast());
	}

	@Override
	public CredencialResponse generarUsuario(GenerarUsuarioRequest solicitud, String actor) {
		String dni = UtilTelefono.validarDocumentoIdentidad(solicitud.dni());

		Usuario existente = usuarioRepository.findByUsuario(dni).orElse(null);
		if (existente != null && existente.isEstado()) {
			throw new UsuarioYaExisteException(
					"El DNI " + dni + " ya tiene una cuenta activa. Anulela antes de regenerar.");
		}

		String nombreCompleto = (solicitud.nombre() != null && !solicitud.nombre().isBlank())
				? solicitud.nombre().trim()
				: clienteReniec.consultar(dni).map(DatosReniec::nombreCompleto)
						.orElse(existente != null ? existente.getNombre() : null);

		NombreParser parser = NombreParser.parsear(nombreCompleto);

		String clave = GeneradorCodigos.generar(6).toUpperCase();

		Usuario usuario;
		if (existente != null) {
			// Reactivar la cuenta anulada con una nueva clave.
			existente.setEstado(true);
			existente.setContrasena(passwordEncoder.encode(clave));
			if (!parser.esVacio()) {
				existente.setNombre(parser.completo());
				existente.setNombres(parser.nombres());
				existente.setApellidoPaterno(parser.apellidoPaterno());
				existente.setApellidoMaterno(parser.apellidoMaterno());
			}
			existente.setUsuarioModificacion(actor);
			usuario = usuarioRepository.save(existente);
		} else {
			Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
					.orElseThrow(() -> new IllegalStateException("Falta el rol PACIENTE (semilla Flyway)."));
			usuario = Usuario.builder()
					.usuario(dni)
					.dni(dni)
					.nombre(parser.esVacio() ? null : parser.completo())
					.nombres(parser.nombres())
					.apellidoPaterno(parser.apellidoPaterno())
					.apellidoMaterno(parser.apellidoMaterno())
					.estado(true)
					.contrasena(passwordEncoder.encode(clave))
					.roles(new HashSet<>(Set.of(rolPaciente)))
					.build();
			usuario.setUsuarioCreacion(actor);
			usuario = usuarioRepository.save(usuario);
		}

		// Pendiente de subir resultados si aun no tiene ninguno cargado.
		boolean pendiente = !resultadoCargaRepository.existsByDocumentoIdentidadAndEstado(dni, 1);
		upsertPaciente(dni, parser, solicitud.telefono(), usuario.getId(), actor, pendiente);

		String portalUrl = appProperties.getPortalUrl();
		String qrUrl = appProperties.getQr().getBaseUrl() + "?size=220x220&data="
				+ URLEncoder.encode(portalUrl == null ? "" : portalUrl, StandardCharsets.UTF_8);

		boolean smsEnviado = enviarCredencialSms(solicitud.telefono(), parser.completo(), dni, clave, portalUrl, solicitud.enviarSms());

		log.info("Credencial generada. dni={}, reactivada={}, sms={}, por={}",
				dni, existente != null, smsEnviado, actor);
		return new CredencialResponse(dni, parser.completo(), dni, clave, portalUrl, qrUrl, smsEnviado);
	}

	@Override
	public void anularUsuario(String dni, String actor) {
		String d = UtilTelefono.validarDocumentoIdentidad(dni);
		Usuario usuario = usuarioRepository.findByUsuario(d)
				.orElseThrow(() -> new RecursoNoEncontradoException("El DNI " + d + " no tiene una cuenta."));

		usuario.setEstado(false);
		usuario.setUsuarioModificacion(actor);
		usuarioRepository.save(usuario);

		pacienteRepository.findByDni(d).ifPresent(p -> {
			p.setEstado(false);
			p.setUsuarioModificacion(actor);
			pacienteRepository.save(p);
		});

		log.info("Cuenta anulada. dni={}, por={}", d, actor);
	}

	private String estadoCuenta(String dni) {
		return usuarioRepository.findByUsuario(dni)
				.map(u -> u.isEstado() ? "ACTIVO" : "ANULADO")
				.orElse("SIN_CUENTA");
	}

	private void upsertPaciente(String dni, NombreParser parser, String telefono, Long idUsuario,
			String actor, boolean pendiente) {
		Paciente paciente = pacienteRepository.findByDni(dni).orElseGet(Paciente::new);
		paciente.setDni(dni);
		if (!parser.esVacio()) {
			paciente.setNombre(parser.completo());
			paciente.setNombres(parser.nombres());
			paciente.setApellidoPaterno(parser.apellidoPaterno());
			paciente.setApellidoMaterno(parser.apellidoMaterno());
		}
		if (telefono != null && !telefono.isBlank()) {
			paciente.setTelefono(telefono.trim());
		}
		paciente.setIdUsuario(idUsuario);
		paciente.setEstado(true);
		paciente.setPendienteResultados(pendiente);
		if (paciente.getUsuarioCreacion() == null) {
			paciente.setUsuarioCreacion(actor);
		} else {
			paciente.setUsuarioModificacion(actor);
		}
		pacienteRepository.save(paciente);
	}

	private boolean enviarCredencialSms(String telefono, String nombre, String dni, String clave, String portalUrl, Boolean enviarSms) {
		if (!Boolean.TRUE.equals(enviarSms)) {
			return false;
		}
		if (telefono == null || telefono.isBlank()) {
			return false;
		}
		String plantilla = smsProperties.getMessage().getCredencialTemplate();
		if (plantilla == null || plantilla.isBlank()) {
			return false;
		}
		try {
			String phone = UtilTelefono.normalizar(telefono, appProperties.getPhone().getCountryCode());
			String mensaje = plantilla
					.replace("{nombre}", nombre == null ? "" : nombre)
					.replace("{usuario}", dni)
					.replace("{clave}", clave)
					.replace("{link}", portalUrl == null ? "" : portalUrl)
					.trim();
			smsService.enviarTexto(phone, mensaje);
			return true;
		} catch (Exception e) {
			log.warn("No se pudo enviar SMS de credencial a {}: {}", telefono, e.getMessage());
			return false;
		}
	}
}
