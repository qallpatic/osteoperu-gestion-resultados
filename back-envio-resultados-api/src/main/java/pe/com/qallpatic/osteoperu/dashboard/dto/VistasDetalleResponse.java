package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.util.List;

/**
 * Detalle de la pestana "Vistas de pacientes" del Dashboard.
 *
 * <p>Incluye: top 10 resultados mas vistos, distribucion por buckets
 * (1, 2-5, 6-10, 11+) y curva temporal de aperturas (grafico de linea).
 *
 * <p>Datos: cabecera viva de {@code RESULTADO_CARGA.CONTADOR_VISTAS}
 * para top/buckets; eventos individuales de {@code EVENTO_VISTA} para
 * la curva temporal.
 */
public record VistasDetalleResponse(

		/** Rango efectivo del que provienen los datos. */
		String desde,
		String hasta,

		/** Top 10 resultados mas vistos (tabla + barras). */
		List<ResultadoMasVistoResponse> resultadosMasVistos,

		/** Distribucion de vistas por buckets (grafico de barras). */
		List<BucketVistasResponse> distribucionBuckets,

		/** Curva temporal de aperturas (grafico de linea, denso). */
		List<PuntoVistaDiaria> curvaTemporal) {
}
