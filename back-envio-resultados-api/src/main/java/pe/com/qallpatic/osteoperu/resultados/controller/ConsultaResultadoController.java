package pe.com.qallpatic.osteoperu.resultados.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;
import pe.com.qallpatic.osteoperu.resultados.service.ConsultaResultadoService;

import java.time.LocalDate;

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
}
