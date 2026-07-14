package pe.com.qallpatic.osteoperu.integracion.reniec;

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

/**
 * Lookup de nombre por DNI contra RENIEC. Ruta propia (/api/reniec/**) con
 * seguridad independiente: la usan modulos de resultados, admin y admision
 * para autocompletar el nombre del paciente. Mantenerla fuera de
 * /api/resultados evita abrir todo ese modulo a ADMISION solo por este caso.
 */
@RestController
@RequestMapping("/api/reniec")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RENIEC", description = "Consulta de nombre por DNI (proveedor RENIEC, con cache local)")
public class ReniecController {

	private final ClienteReniec clienteReniec;

	@Operation(summary = "Consultar nombre en RENIEC por DNI",
			description = "Devuelve el nombre completo. Para DNI (8 digitos) consulta al proveedor "
					+ "con cache local; para CE (9-12) devuelve null sin gastar la llamada.")
	@GetMapping("/{dni}")
	public ResponseEntity<RespuestaApi<NombreReniecResponse>> consultar(@PathVariable String dni) {
		String d = UtilTelefono.validarDocumentoIdentidad(dni);
		String nombre = d.length() == 8
				? clienteReniec.consultar(d).map(DatosReniec::nombreCompleto).orElse(null)
				: null;
		return ResponseEntity.ok(RespuestaApi.ok(new NombreReniecResponse(d, nombre)));
	}
}