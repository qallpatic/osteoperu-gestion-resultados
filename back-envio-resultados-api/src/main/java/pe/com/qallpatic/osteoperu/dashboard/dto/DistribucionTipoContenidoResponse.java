package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;

/**
 * Distribucion de archivos agrupados por {@code tipoContenido} (MIME type).
 *
 * <p>Cada fila representa un bucket: cuantos registros hay con ese MIME
 * y cuantos MB suman en total.
 */
public record DistribucionTipoContenidoResponse(

		/** MIME type, ej.: application/pdf, image/jpeg, video/mp4. */
		String tipoContenido,

		/** Cantidad de archivos (sin filtrar soft-delete). */
		long cantidad,

		/** Suma de peso en MB del bucket. */
		BigDecimal pesoMb) {
}
