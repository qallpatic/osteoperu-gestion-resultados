package pe.com.qallpatic.osteoperu.dashboard.service;

import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;

import java.time.Instant;

/**
 * Entrada de cache para el resumen del dashboard. Almacena el valor precomputado
 * junto con su instante de expiracion. Vive solo en memoria del backend.
 */
public record EntradaCacheDashboard(DashboardResumenResponse valor, Instant expiraEn) {

	public boolean expiro() {
		return Instant.now().isAfter(expiraEn);
	}
}