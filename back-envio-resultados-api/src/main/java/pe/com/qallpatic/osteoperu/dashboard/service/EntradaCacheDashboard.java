package pe.com.qallpatic.osteoperu.dashboard.service;

import java.time.Instant;

/**
 * Entrada de cache para cualquier respuesta del dashboard. Almacena el valor
 * precomputado junto con su instante de expiracion. Vive solo en memoria
 * del backend. El tipo {@code T} se infiere del caller: el resumen se
 * cachea como {@code EntradaCacheDashboard<DashboardResumenResponse>}, los
 * detalles como {@code EntradaCacheDashboard<AlmacenamientoDetalleResponse>},
 * etc.
 */
public record EntradaCacheDashboard<T>(T valor, Instant expiraEn) {

	public boolean expiro() {
		return Instant.now().isAfter(expiraEn);
	}
}
