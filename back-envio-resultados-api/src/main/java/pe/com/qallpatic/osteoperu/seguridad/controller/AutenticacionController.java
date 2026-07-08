package pe.com.qallpatic.osteoperu.seguridad.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.seguridad.dto.LoginRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.RefreshRequest;
import pe.com.qallpatic.osteoperu.seguridad.dto.TokenResponse;
import pe.com.qallpatic.osteoperu.seguridad.service.ServicioAutenticacion;

/** Endpoints publicos de autenticacion: login, refresh y logout. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticacion", description = "Inicio de sesion y gestion de tokens JWT")
public class AutenticacionController {

	private final ServicioAutenticacion servicioAutenticacion;

	@Operation(summary = "Iniciar sesion", description = "Valida credenciales y devuelve access + refresh token.")
	@PostMapping("/login")
	public ResponseEntity<RespuestaApi<TokenResponse>> login(@Valid @RequestBody LoginRequest solicitud) {
		TokenResponse token = servicioAutenticacion.login(solicitud);
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
}
