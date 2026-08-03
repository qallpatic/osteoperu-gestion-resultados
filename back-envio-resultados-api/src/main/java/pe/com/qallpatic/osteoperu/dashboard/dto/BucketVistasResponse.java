package pe.com.qallpatic.osteoperu.dashboard.dto;

/**
 * Bucket de distribucion de vistas: cuantos resultados tienen
 * exactamente N vistas (1, 2-5, 6-10, 11+).
 *
 * <p>El {@code etiqueta} es la leyenda corta que se muestra al usuario
 * (ej.: "2-5") y {@code cantidad} es cuantos resultados caen en ese
 * rango. Se computa en Java desde la lista de {@code CONTADOR_VISTAS}
 * (no requiere CASE WHEN en SQL — el dataset es chico).
 */
public record BucketVistasResponse(

		/** Etiqueta corta para el grafico: "1", "2-5", "6-10", "11+". */
		String etiqueta,

		/** Cantidad de resultados en ese rango. */
		long cantidad) {
}
