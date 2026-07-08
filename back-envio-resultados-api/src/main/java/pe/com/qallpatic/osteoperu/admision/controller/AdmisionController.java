package pe.com.qallpatic.osteoperu.admision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.admision.dto.CredencialResponse;
import pe.com.qallpatic.osteoperu.admision.dto.GenerarUsuarioRequest;
import pe.com.qallpatic.osteoperu.admision.dto.PacienteAdmisionResponse;
import pe.com.qallpatic.osteoperu.admision.service.AdmisionService;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;

/** Admision: alta y baja de la cuenta de acceso del paciente. Roles: ADMIN, ADMISION. */
@RestController
@RequestMapping("/api/admision")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admision", description = "Generar y anular la cuenta del paciente (credencial + QR)")
public class AdmisionController {

	private final AdmisionService admisionService;

	@Operation(summary = "Listar pacientes con resultados",
			description = "Muestra los pacientes (por DNI) y el estado de su cuenta de acceso.")
	@GetMapping("/pacientes")
	public ResponseEntity<RespuestaApi<PaginaResponse<PacienteAdmisionResponse>>> listarPacientes(
			@RequestParam(required = false) String dni,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "10") int tamano) {
		return ResponseEntity.ok(RespuestaApi.ok(admisionService.listarPacientes(dni, pagina, tamano)));
	}

	@Operation(summary = "Generar usuario del paciente",
			description = "Crea la cuenta (rol PACIENTE, usuario=DNI, clave aleatoria) y devuelve la credencial con QR.")
	@PostMapping("/usuarios/generar")
	public ResponseEntity<RespuestaApi<CredencialResponse>> generar(
			@Valid @RequestBody GenerarUsuarioRequest solicitud, Authentication auth) {
		CredencialResponse credencial = admisionService.generarUsuario(solicitud, actor(auth));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaApi.ok("Cuenta generada correctamente.", credencial));
	}

	@Operation(summary = "Anular usuario del paciente", description = "Desactiva el acceso del paciente por su DNI.")
	@PostMapping("/usuarios/{dni}/anular")
	public ResponseEntity<RespuestaApi<Void>> anular(@PathVariable String dni, Authentication auth) {
		admisionService.anularUsuario(dni, actor(auth));
		return ResponseEntity.ok(RespuestaApi.mensaje("Acceso del paciente anulado."));
	}

	private String actor(Authentication auth) {
		return auth != null ? auth.getName() : "SISTEMA";
	}
}
