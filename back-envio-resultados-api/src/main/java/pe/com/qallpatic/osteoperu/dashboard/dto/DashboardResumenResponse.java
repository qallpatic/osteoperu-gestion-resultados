package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta agregada del modulo Dashboard para el rol ADMIN.
 *
 * <p>Los KPIs son totales globales a la fecha. Las series son el historico
 * dentro del rango pedido (default: ultimos 30 dias) y se devuelven densas
 * (un punto por dia, con 0 si no hubo actividad) para que el front pueda
 * graficar sin re-armar el calendario.
 *
 * <p>Archivos y peso total consideran TODOS los registros (incluido soft-delete),
 * porque en OSTEOPERU los blobs no se borran fisicamente: estado=0 mantiene
 * la fila en BD y el archivo sigue ocupando espacio en el bucket.
 */
public record DashboardResumenResponse(

		/** Totales globales (sin filtro de rango). */
		long totalUsuarios,
		long totalResultados,
		long totalArchivos,
		BigDecimal pesoTotalMb,
		long totalVistas,

		/** Pico historico de usuarios registrados en un solo dia. */
		PicoRegistrosResponse picoUsuarios,

		/** Rango efectivo del que provienen las series. */
		String desde,
		String hasta,

		/** Series temporales dentro del rango. */
		List<PuntoSerieDiaria> serieResultadosPorDia,
		List<PuntoSerieDiaria> serieUsuariosPorDia) {
}