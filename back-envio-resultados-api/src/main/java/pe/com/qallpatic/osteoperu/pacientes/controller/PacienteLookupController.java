package pe.com.qallpatic.osteoperu.pacientes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.compartido.util.UtilTelefono;
import pe.com.qallpatic.osteoperu.pacientes.dto.PacienteLookupResponse;
import pe.com.qallpatic.osteoperu.pacientes.repository.PacienteRepository;

/**
 * Lookup publico dentro del modulo de pacientes. Pensado para que el modulo
 * de Resultados autocomplete nombre y telefono al tipear el DNI en la carga,
 * usando lo que el operador ya guardo via Admision/Gestion de usuarios.
 * Coexiste con /api/reniec/{dni} (que sigue siendo fallback cuando el
 * paciente no esta registrado localmente).
 */
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pacientes", description = "Lookup basico de paciente por DNI para autocompletar formularios")
public class PacienteLookupController {

	private final PacienteRepository pacienteRepository;

	@Operation(summary = "Buscar paciente por DNI",
			description = "Devuelve {dni, nombre, telefono} si el DNI esta registrado en PACIENTE, "
					+ "incluyendo cuentas anuladas (el operador puede estar reactivandolas). "
					+ "Si no existe, devuelve 200 con nombre=null y telefono=null para que el front "
					+ "caiga al fallback de RENIEC.")
	@GetMapping("/buscar/{dni}")
	public ResponseEntity<RespuestaApi<PacienteLookupResponse>> buscar(@PathVariable String dni) {
		String d = UtilTelefono.validarDocumentoIdentidad(dni);
		PacienteLookupResponse body = pacienteRepository.findByDni(d)
				.map(p -> new PacienteLookupResponse(p.getDni(), p.getNombre(), p.getTelefono()))
				.orElseGet(() -> new PacienteLookupResponse(d, null, null));
		return ResponseEntity.ok(RespuestaApi.ok(body));
	}
}