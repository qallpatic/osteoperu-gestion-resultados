package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.math.BigDecimal;

/**
 * Archivo mas pesado del bucket de almacenamiento (top N).
 * Pensado para alimentar una tabla con los archivos mas grandes
 * del sistema.
 */
public record ArchivoPesadoResponse(

		/** ID inmutable del archivo (estable, sin filtrar soft-delete). */
		String idGenerado,

		/** Nombre original con el que se subio (ej.: radiografia_2026_07.pdf). */
		String nombreOriginal,

		/** MIME type (application/pdf, image/jpeg, ...). */
		String tipoContenido,

		/** Tamano del archivo en MB. */
		BigDecimal pesoMb) {
}
