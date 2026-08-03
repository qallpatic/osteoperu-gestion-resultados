package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;

/**
 * Punto de la curva "MB acumulados a lo largo del tiempo".
 *
 * <p>Cada fila representa cuantos MB llevaba el sistema al cierre del dia,
 * calculado en Java sumando incrementalmente la suma diaria (porque la tabla
 * tiene solo 178 registros en prod y un window function nativo no aporta).
 */
public record PuntoAcumuladoMb(

		/** Fecha del cierre (YYYY-MM-DD). */
		String fecha,

		/** MB acumulados al cierre de esa fecha. */
		BigDecimal mbAcumulado) {
}
