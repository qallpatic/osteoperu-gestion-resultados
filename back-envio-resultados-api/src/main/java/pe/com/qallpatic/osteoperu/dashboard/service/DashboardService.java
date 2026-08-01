package pe.com.qallpatic.osteoperu.dashboard.service;

import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;

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
}