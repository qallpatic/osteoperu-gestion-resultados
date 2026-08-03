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
import pe.com.qallpatic.osteoperu.dashboard.dto.AlmacenamientoDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.VistasDetalleResponse;
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
		return descargaExcel(bytes, "dashboard_" + sufijoRango(desde, hasta) + ".xlsx");
	}

	@Operation(
			summary = "Detalle de la pestana Almacenamiento",
			description = "Distribucion por tipo de contenido (torta), top 10 archivos mas pesados "
					+ "(tabla) y curva acumulada de MB (linea) para el rango pedido. Considera TODOS "
					+ "los registros sin filtrar soft-delete, igual que el KPI global de almacenamiento.")
	@GetMapping("/almacenamiento/detalle")
	public ResponseEntity<RespuestaApi<AlmacenamientoDetalleResponse>> almacenamientoDetalle(
			@Parameter(description = "Fecha de inicio del rango (inclusive). Default: hoy - 29 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha de fin del rango (inclusive). Default: hoy. Maximo 365 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		AlmacenamientoDetalleResponse data = dashboardService.obtenerAlmacenamientoDetalle(desde, hasta);
		return ResponseEntity.ok(RespuestaApi.ok(data));
	}

	@Operation(
			summary = "Detalle de la pestana Vistas",
			description = "Top 10 resultados mas vistos (tabla), distribucion por buckets de "
					+ "vistas 1/2-5/6-10/11+ (barras) y curva temporal de aperturas (linea, densa) "
					+ "para el rango pedido. Las cabeceras se filtran por estado=vivo; la curva "
					+ "temporal se calcula desde la tabla EVENTO_VISTA.")
	@GetMapping("/vistas/detalle")
	public ResponseEntity<RespuestaApi<VistasDetalleResponse>> vistasDetalle(
			@Parameter(description = "Fecha de inicio del rango (inclusive). Default: hoy - 29 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha de fin del rango (inclusive). Default: hoy. Maximo 365 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		VistasDetalleResponse data = dashboardService.obtenerVistasDetalle(desde, hasta);
		return ResponseEntity.ok(RespuestaApi.ok(data));
	}

	@Operation(
			summary = "Exportar detalle de resultados cargados a Excel",
			description = "Genera un .xlsx con dos hojas (Resumen + Resultados) listando TODAS las "
					+ "cabeceras del rango, incluyendo soft-deleted con columna Estado y campos "
					+ "de eliminacion cuando aplica. Pensado para auditoria operativa del ADMIN.")
	@GetMapping("/resultados/exportar")
	public ResponseEntity<byte[]> exportarResultados(
			@Parameter(description = "Fecha de inicio del rango (inclusive). Default: hoy - 29 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha de fin del rango (inclusive). Default: hoy. Maximo 365 dias.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		byte[] bytes = dashboardService.generarExcelResultados(desde, hasta);
		return descargaExcel(bytes, "dashboard_resultados_" + sufijoRango(desde, hasta) + ".xlsx");
	}

	/**
	 * Construye la respuesta HTTP para una descarga de .xlsx. Centraliza el
	 * Content-Disposition UTF-8 y los headers (Content-Type, Content-Length).
	 */
	private ResponseEntity<byte[]> descargaExcel(byte[] bytes, String nombreArchivo) {
		ContentDisposition cd = ContentDisposition.attachment()
				.filename(nombreArchivo, StandardCharsets.UTF_8)
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(cd);
		headers.setContentType(XLSX);
		headers.setContentLength(bytes.length);

		log.info("Exportacion dashboard. nombre={}, bytes={}", nombreArchivo, bytes.length);
		return new ResponseEntity<>(bytes, headers, ResponseEntity.ok().build().getStatusCode());
	}

	/** Sufijo "_YYYY-MM-DD_YYYY-MM-DD" para nombres de archivo; vacio si ambos null. */
	private String sufijoRango(LocalDate desde, LocalDate hasta) {
		return (desde != null ? desde.toString() : "")
				+ "_"
				+ (hasta != null ? hasta.toString() : "");
	}
}
