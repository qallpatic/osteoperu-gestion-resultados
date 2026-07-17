package pe.com.qallpatic.osteoperu.seguridad.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.compartido.util.ClienteIpHelper;
import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.RecuperarPasswordRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.RecuperarPasswordResponse;
import pe.com.qallpatic.osteoperu.seguridad.dto.RefreshRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.TokenResponse;
import pe.com.qallpatic.osteoperu.seguridad.service.ServicioAutenticacion;
import pe.com.qallpatic.osteoperu.seguridad.service.ServicioRecuperacionPassword;

/** Endpoints publicos de autenticacion: login, refresh, logout y recuperacion de password. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticacion", description = "Inicio de sesion y gestion de tokens JWT")
public class AutenticacionController {

	private final ServicioAutenticacion servicioAutenticacion;
	private final ServicioRecuperacionPassword servicioRecuperacion;

	@Operation(summary = "Iniciar sesion", description = "Valida credenciales y devuelve access + refresh token. "
			+ "Aplica rate-limit por IP y por usuario (config login.*).")
	@PostMapping("/login")
	public ResponseEntity<RespuestaApi<TokenResponse>> login(@Valid @RequestBody LoginRequest solicitud,
			HttpServletRequest request) {
		String ip = ClienteIpHelper.resolver(request);
		TokenResponse token = servicioAutenticacion.login(solicitud, ip);
		return ResponseEntity.ok(RespuestaApi.ok("Autenticacion exitosa.", token));
	}

	@Operation(summary = "Renovar token", description = "Emite un nuevo access token a partir del refresh token.")
	@PostMapping("/refresh")
	public ResponseEntity<RespuestaApi<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest solicitud) {
		TokenResponse token = servicioAutenticacion.refrescar(solicitud.refreshToken());
		return ResponseEntity.ok(RespuestaApi.ok("Token renovado.", token));
	}

	@Operation(summary = "Cerrar sesion", description = "Revoca el refresh token indicado.")
	@PostMapping("/logout")
	public ResponseEntity<RespuestaApi<Void>> logout(@Valid @RequestBody RefreshRequest solicitud) {
		servicioAutenticacion.logout(solicitud.refreshToken());
		return ResponseEntity.ok(RespuestaApi.mensaje("Sesion cerrada correctamente."));
	}

	@Operation(summary = "Recuperar contrasena por documento",
			description = "Recibe el DNI/CE del paciente, regenera la clave y envia un SMS al telefono "
					+ "asociado. La respuesta es siempre generica e incluye el telefono ofuscado "
					+ "(ultimos 4 digitos) para que el usuario verifique sin comprometer la informacion. "
					+ "Aplica rate-limit por IP, por documento y por cantidad de documentos distintos por IP.")
	@PostMapping("/recuperar-password")
	public ResponseEntity<RespuestaApi<RecuperarPasswordResponse>> recuperarPassword(
			@Valid @RequestBody RecuperarPasswordRequest solicitud, HttpServletRequest request) {
		String ip = ClienteIpHelper.resolver(request);
		RecuperarPasswordResponse respuesta = servicioRecuperacion.solicitar(ip, solicitud.documento());
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
		if (respuesta.retryAfterSeconds() != null && respuesta.retryAfterSeconds() > 0) {
			builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(respuesta.retryAfterSeconds()));
		}
		return builder.body(RespuestaApi.ok(respuesta.mensajeUsuario(), respuesta));
	}
}
