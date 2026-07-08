package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;

/** Archivo asociado a un resultado (para el detalle). */
public record ArchivoResponse(
		Long id,
		String nombreOriginal,
		String tipoMedio,
		long tamanoBytes,
		LocalDateTime fechaCreacion) {
}
