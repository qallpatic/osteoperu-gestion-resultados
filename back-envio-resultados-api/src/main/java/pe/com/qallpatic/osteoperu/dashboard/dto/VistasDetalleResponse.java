package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de la pestana "Vistas de pacientes" del Dashboard.
 *
 * <p>Incluye:
 * <ul>
 *   <li>KPIs accionables: total aperturas, total descargas, ratio
 *       descargas/apertura, hora pico de actividad.</li>
 *   <li>Curva temporal densa con dos series: aperturas del visor y
 *       descargas de archivos (stacked bar).</li>
 *   <li>Distribucion horaria (24 puntos, 0-23) para identificar la
 *       ventana del dia en que los pacientes revisan sus resultados.</li>
 * </ul>
 *
 * <p>Datos: cabecera viva de {@code RESULTADO_CARGA.CONTADOR_VISTAS}
 * (combinado con descargas por cabecera); eventos individuales de
 * {@code EVENTO_VISTA} para las curvas y KPIs.
 */
public record VistasDetalleResponse(

		/** Rango efectivo del que provienen los datos. */
		String desde,
		String hasta,

		/** Curva temporal del total combinado (aperturas + descargas). */
		List<PuntoVistaDiaria> curvaTemporal,

		/** Curva temporal SOLO de aperturas del visor (base del stacked bar). */
		List<PuntoVistaDiaria> curvaTemporalAperturas,

		/** Curva temporal SOLO de descargas de archivos (segmento superior). */
		List<PuntoVistaDiaria> curvaTemporalDescargas,

		/** Total de eventos VISTA en el rango (aperturas del visor). */
		Long totalAperturas,

		/** Total de eventos DESCARGA en el rango (descargas de archivos). */
		Long totalDescargas,

		/**
		 * Ratio descargas / aperturas como porcentaje redondeado a 1
		 * decimal (ej. 22.5 = 22.5%). 0 si no hay aperturas en el rango.
		 */
		BigDecimal ratioDescargaPorApertura,

		/**
		 * Distribucion horaria densa (24 puntos, hora 0-23) con el total
		 * de interacciones por hora en todo el rango. Las horas sin
		 * actividad vienen con total=0 para que el chart tenga eje X
		 * completo.
		 */
		List<PuntoHorario> distribucionHoraria,

		/** Hora del dia con mas interacciones (0-23). null si no hubo actividad. */
		Integer horaPico,

		/** Cantidad de interacciones registradas en la hora pico. */
		Long totalHoraPico) {
}
