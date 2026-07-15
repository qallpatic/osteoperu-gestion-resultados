package pe.com.qallpatic.osteoperu.paciente.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.compartido.util.UtilInfoCliente;
import pe.com.qallpatic.osteoperu.paciente.dto.ArchivoLocalizado;
import pe.com.qallpatic.osteoperu.paciente.service.PacientePortalService;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.service.ArchivoDescargable;
import pe.com.qallpatic.osteoperu.resultados.service.ViewerService;

import java.time.LocalDate;

/** Portal del paciente (solo lectura de sus propios resultados). */
@RestController
@RequestMapping("/api/paciente")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Portal del paciente", description = "Consulta y descarga de los resultados propios del paciente")
public class PacientePortalController {

	private final PacientePortalService portalService;
	private final ViewerService viewerService;

	@Operation(summary = "Mis resultados (cabeceras)",
			description = "Lista las transacciones (cabeceras) del paciente autenticado, paginadas.")
	@GetMapping("/resultados")
	public ResponseEntity<RespuestaApi<PaginaResponse<ResultadoResumenResponse>>> misResultados(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "10") int tamano,
			Authentication auth) {
		return ResponseEntity.ok(RespuestaApi.ok(
				portalService.misResultados(auth.getName(), esAdmin(auth), desde, hasta, pagina, tamano)));
	}

	@Operation(summary = "Detalle de una cabecera por ID")
	@GetMapping("/resultados/{id}")
	public ResponseEntity<RespuestaApi<ResultadoDetalleResponse>> detalle(
			@PathVariable Long id, Authentication auth) {
		return ResponseEntity.ok(RespuestaApi.ok(portalService.detalle(auth.getName(), esAdmin(auth), id)));
	}

	@Operation(summary = "Detalle de una cabecera por idGenerado",
			description = "Acceso publico estable: la cabecera tiene un idGenerado unico e inmutable.")
	@GetMapping("/resultados/id/{idGenerado}")
	public ResponseEntity<RespuestaApi<ResultadoDetalleResponse>> detallePorIdGenerado(
			@PathVariable String idGenerado, Authentication auth) {
		return ResponseEntity.ok(RespuestaApi.ok(
				portalService.detallePorIdGenerado(auth.getName(), esAdmin(auth), idGenerado)));
	}

	@Operation(summary = "Ver un archivo en linea")
	@GetMapping("/resultados/{id}/archivos/{archivoId}")
	public ResponseEntity<Resource> ver(@PathVariable Long id, @PathVariable Long archivoId, Authentication auth) {
		return servir(auth, id, archivoId, false, null);
	}

	@Operation(summary = "Descargar un archivo")
	@GetMapping("/resultados/{id}/archivos/{archivoId}/descargar")
	public ResponseEntity<Resource> descargar(@PathVariable Long id, @PathVariable Long archivoId,
			Authentication auth, HttpServletRequest request) {
		return servir(auth, id, archivoId, true, request);
	}

	@Operation(summary = "Soft delete de una cabecera (solo agente/admin)",
			description = "Marca la cabecera como inactiva (estado=0). El archivo permanece en S3 hasta purga.")
	@DeleteMapping("/resultados/{id}")
	public ResponseEntity<RespuestaApi<Void>> eliminar(
			@PathVariable Long id,
			@RequestParam(required = false) String motivo,
			Authentication auth) {
		portalService.softDelete(auth.getName(), esAdmin(auth), id, motivo);
		return ResponseEntity.ok(RespuestaApi.ok("Cabecera marcada como inactiva.", null));
	}

	private ResponseEntity<Resource> servir(Authentication auth, Long id, Long archivoId,
			boolean comoAdjunto, HttpServletRequest request) {
		boolean admin = esAdmin(auth);
		ArchivoLocalizado loc = portalService.localizarArchivo(auth.getName(), admin, id, archivoId);
		ArchivoDescargable descargable = portalService.descargarArchivo(auth.getName(), admin, id, archivoId);

		if (request != null && comoAdjunto) {
			viewerService.registerDownload(id,
					UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));
		}

		MediaType mediaType = descargable.contentType() == null
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(descargable.contentType());
		String disposition = (comoAdjunto ? "attachment" : "inline")
				+ "; filename=\"" + loc.nombreOriginal() + "\"";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.contentType(mediaType)
				.body(new InputStreamResource(descargable.contenido()));
	}

	private boolean esAdmin(Authentication auth) {
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}
}
