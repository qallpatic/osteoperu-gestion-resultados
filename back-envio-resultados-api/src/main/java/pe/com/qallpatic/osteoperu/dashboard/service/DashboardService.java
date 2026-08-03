package pe.com.qallpatic.osteoperu.dashboard.service;

import pe.com.qallpatic.osteoperu.dashboard.dto.AlmacenamientoDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.VistasDetalleResponse;

import java.time.LocalDate;

/**
 * Casos de uso del modulo Dashboard (solo ADMIN).
 */
public interface DashboardService {

	/**
	 * Devuelve el resumen para el rango pedido (inclusive).
	 * Si el rango es nulo o invalido, se usan los ultimos 30 dias como default.
	 */
	DashboardResumenResponse obtenerResumen(LocalDate desde, LocalDate hasta);

	/**
	 * Genera el archivo .xlsx (KPIs + series) para el mismo rango que
	 * {@link #obtenerResumen(LocalDate, LocalDate)}. Reutiliza el calculo
	 * (y por lo tanto el cache en memoria) del resumen.
	 *
	 * @return bytes del workbook serializado; listo para devolver como
	 *         application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.
	 */
	byte[] generarExcel(LocalDate desde, LocalDate hasta);

	/**
	 * Detalle de la pestana Almacenamiento: distribucion por tipoContenido,
	 * top archivos mas pesados y curva acumulada de MB. Cuenta TODOS los
	 * registros sin filtrar soft-delete (mismo criterio que el KPI global).
	 */
	AlmacenamientoDetalleResponse obtenerAlmacenamientoDetalle(LocalDate desde, LocalDate hasta);

	/**
	 * Detalle de la pestana Vistas: top resultados mas vistos, distribucion
	 * por buckets (1, 2-5, 6-10, 11+) y curva temporal de aperturas desde
	 * EVENTO_VISTA.
	 */
	VistasDetalleResponse obtenerVistasDetalle(LocalDate desde, LocalDate hasta);

	/**
	 * Genera el .xlsx con el detalle plano de "Resultados cargados" del rango.
	 * Incluye TODOS los registros (vivos + soft-deleted) con columna Estado
	 * y campos de eliminacion cuando aplica.
	 */
	byte[] generarExcelResultados(LocalDate desde, LocalDate hasta);

	/**
	 * Genera el .xlsx con el detalle plano de "Usuarios registrados" del rango.
	 * Una fila por usuario con sus roles (CSV), datos de contacto y campos
	 * de auditoria (creado por / modificado por).
	 */
	byte[] generarExcelUsuarios(LocalDate desde, LocalDate hasta);
}
