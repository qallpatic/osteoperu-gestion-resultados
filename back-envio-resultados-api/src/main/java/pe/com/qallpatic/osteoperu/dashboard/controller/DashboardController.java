package pe.com.qallpatic.osteoperu.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaApi;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.service.DashboardService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Endpoints del modulo Dashboard. Acceso restringido a rol ADMIN
 * (configurado en {@link pe.com.qallpatic.osteoperu.config.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Metricas e indicadores historicos de la plataforma (solo ADMIN)")
public class DashboardController {

	private static final MediaType XLSX = MediaType.parseMediaType(
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final DashboardService dashboardService;

	@Operation(
			summary = "Resumen del dashboard",
			description = "KPIs globales (usuarios, resultados, archivos, vistas) + pico historico de registros "
					+ "+ series diarias dentro del rango pedido. Si no se envia rango, se usan los ultimos 30 dias.")
	@GetMapping("/resumen")
	public ResponseEntity<RespuestaApi<DashboardResumenResponse>> resumen(
			@Parameter(description = "Fecha de inicio del rango (inclusive). Default: hoy - 29 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha de fin del rango (inclusive). Default: hoy. Maximo 365 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		DashboardResumenResponse data = dashboardService.obtenerResumen(desde, hasta);
		return ResponseEntity.ok(RespuestaApi.ok(data));
	}

	@Operation(
			summary = "Exportar resumen del dashboard a Excel",
			description = "Genera un .xlsx con tres hojas (KPIs, Resultados por dia, Usuarios por dia) "
					+ "para el mismo rango que el endpoint de resumen. Reutiliza el cache en memoria "
					+ "del backend, por lo que es barato dispararlo varias veces con el mismo rango.")
	@GetMapping("/resumen/exportar")
	public ResponseEntity<byte[]> exportar(
			@Parameter(description = "Fecha de inicio del rango (inclusive). Default: hoy - 29 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha de fin del rango (inclusive). Default: hoy. Maximo 365 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		byte[] bytes = dashboardService.generarExcel(desde, hasta);
		String nombre = "dashboard_" + (desde != null ? desde : "") + "_" + (hasta != null ? hasta : "") + ".xlsx";

		ContentDisposition cd = ContentDisposition.attachment()
				.filename(nombre, StandardCharsets.UTF_8)
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(cd);
		headers.setContentType(XLSX);
		headers.setContentLength(bytes.length);

		log.info("Dashboard exportado. nombre={}, bytes={}", nombre, bytes.length);
		return new ResponseEntity<>(bytes, headers, ResponseEntity.ok().build().getStatusCode());
	}
}