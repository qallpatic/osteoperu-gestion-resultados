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
}