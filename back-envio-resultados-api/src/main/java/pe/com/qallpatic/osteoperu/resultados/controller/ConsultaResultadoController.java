package pe.com.qallpatic.osteoperu.resultados.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.resultados.dto.ActualizarArchivosRequest;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;
import pe.com.qallpatic.osteoperu.resultados.service.ConsultaResultadoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Consulta de resultados cargados, detalle, actualizacion y logs de carga. */
@RestController
@RequestMapping("/api/resultados")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consulta de resultados", description = "Listado, detalle, actualizacion y bitacora de cargas")
public class ConsultaResultadoController {

	private final ConsultaResultadoService consultaService;

	@Operation(summary = "Listar resultados", description = "Filtra por DNI, nombre y rango de fechas (paginado).")
	@GetMapping
	public ResponseEntity<RespuestaApi<PaginaResponse<ResultadoResumenResponse>>> listar(
			@RequestParam(required = false) String dni,
			@RequestParam(required = false) String nombre,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "10") int tamano) {
		return ResponseEntity.ok(RespuestaApi.ok(
				consultaService.listar(dni, nombre, desde, hasta, pagina, tamano)));
	}

	@Operation(summary = "Consultar logs de carga", description = "Bitacora filtrable por DNI y rango de fechas.")
	@GetMapping("/logs")
	public ResponseEntity<RespuestaApi<PaginaResponse<LogCargaResponse>>> logs(
			@RequestParam(required = false) String dni,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "10") int tamano) {
		return ResponseEntity.ok(RespuestaApi.ok(
				consultaService.listarLogs(dni, desde, hasta, pagina, tamano)));
	}

	@Operation(summary = "Pacientes pendientes de subir resultados",
			description = "Registrados por Admision que aun no tienen resultados (para la campanita).")
	@GetMapping("/pendientes")
	public ResponseEntity<RespuestaApi<PaginaResponse<PacientePendienteResponse>>> pendientes(
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		return ResponseEntity.ok(RespuestaApi.ok(consultaService.pendientes(pagina, tamano)));
	}

	@Operation(summary = "Cantidad de pacientes pendientes de subir resultados")
	@GetMapping("/pendientes/contar")
	public ResponseEntity<RespuestaApi<Long>> contarPendientes() {
		return ResponseEntity.ok(RespuestaApi.ok(consultaService.contarPendientes()));
	}

	@Operation(summary = "Detalle de un resultado")
	@GetMapping("/{id}")
	public ResponseEntity<RespuestaApi<ResultadoDetalleResponse>> detalle(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(consultaService.detalle(id)));
	}

	@Operation(summary = "Actualizar un resultado", description = "Actualiza telefono/nombre y opcionalmente reenvia el SMS.")
	@PutMapping("/{id}")
	public ResponseEntity<RespuestaApi<ResultadoDetalleResponse>> actualizar(
			@PathVariable Long id, @Valid @RequestBody ResultadoUpdateRequest solicitud, Authentication auth) {
		String actor = auth != null ? auth.getName() : "SISTEMA";
		return ResponseEntity.ok(RespuestaApi.ok("Resultado actualizado.",
				consultaService.actualizar(id, solicitud, actor)));
	}

	@Operation(
			summary = "Editar archivos de un resultado existente",
			description = "Permite desactivar archivos equivocados, reactivarlos si el operador se "
					+ "arrepiente y subir archivos nuevos al mismo visor (/r/{token}). "
					+ "Soft-delete: los archivos desactivados no se borran fisicamente y el visor "
					+ "deja de mostrarlos. El SMS solo se reencola si reenviarSms=true. "
					+ "motivosPorArchivoJson es opcional: JSON con {idArchivo: motivoIndividual} "
					+ "para overrides del motivo general.")
	@PutMapping(value = "/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RespuestaApi<ResultadoDetalleResponse>> actualizarArchivos(
			@PathVariable Long id,
			@Parameter(description = "IDs de archivos a desactivar (soft-delete)")
			@RequestParam(value = "idsDesactivar", required = false) List<Long> idsDesactivar,
			@Parameter(description = "IDs de archivos a reactivar")
			@RequestParam(value = "idsReactivar", required = false) List<Long> idsReactivar,
			@Parameter(description = "Archivos nuevos a anadir al mismo visor", content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
					schema = @Schema(type = "array", format = "binary")))
			@RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
			@Parameter(description = "Motivo general documentado (se aplica a todos los desactivados "
					+ "que no tengan motivo individual en motivosPorArchivoJson)")
			@RequestParam(value = "motivo", required = false) String motivo,
			@Parameter(description = "JSON {idArchivo: motivoIndividual} opcional para overrides del motivo general")
			@RequestParam(value = "motivosPorArchivoJson", required = false) String motivosPorArchivoJson,
			@Parameter(description = "Si true, reencola el SMS con el mismo link")
			@RequestParam(value = "reenviarSms", defaultValue = "false") boolean reenviarSms,
			Authentication auth) {
		String actor = auth != null ? auth.getName() : "SISTEMA";
		Map<Long, String> motivosPorArchivo = parsearMotivosPorArchivo(motivosPorArchivoJson);
		ActualizarArchivosRequest solicitud = new ActualizarArchivosRequest(
				idsDesactivar, idsReactivar, archivos, motivo, motivosPorArchivo, reenviarSms);
		return ResponseEntity.ok(RespuestaApi.ok("Archivos actualizados.",
				consultaService.actualizarArchivos(id, solicitud, actor)));
	}

	private static Map<Long, String> parsearMotivosPorArchivo(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return new ObjectMapper().readValue(json, new TypeReference<Map<Long, String>>() {});
		} catch (Exception e) {
			throw new pe.com.qallpatic.osteoperu.compartido.excepcion.SolicitudInvalidaException(
					"motivosPorArchivoJson invalido: debe ser JSON {id: motivo}");
		}
	}
}
