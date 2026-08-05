package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.util.List;

/**
 * Detalle de la pestana "Almacenamiento usado" del Dashboard.
 *
 * <p>Incluye: distribucion por tipo de contenido (grafico de torta),
 * top 10 archivos mas pesados (tabla) y curva acumulada de MB a lo
 * largo del tiempo (grafico de linea). Como el resto del modulo, cuenta
 * TODOS los registros sin filtrar soft-delete: en OSTEOPERU los blobs
 * no se borran fisicamente.
 */
public record AlmacenamientoDetalleResponse(

		/** Rango efectivo del que provienen los datos. */
		String desde,
		String hasta,

		/** Distribucion por MIME type (torta). */
		List<DistribucionTipoContenidoResponse> distribucionPorTipo,

		/** Top 10 archivos mas pesados (tabla + barra lateral). */
		List<ArchivoPesadoResponse> archivosMasPesados,

		/** Curva acumulada de MB a lo largo del tiempo (linea). */
		List<PuntoAcumuladoMb> acumuladoMb) {
}
