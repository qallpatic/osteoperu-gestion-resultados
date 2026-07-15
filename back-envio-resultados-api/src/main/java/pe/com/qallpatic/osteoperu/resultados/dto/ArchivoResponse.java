package pe.com.qallpatic.osteoperu.resultados.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Archivo (detalle) asociado a una cabecera de resultado. */
public record ArchivoResponse(
		Long id,
		String idGenerado,
		String idArchivo,
		String nombreOriginal,
		String tipoContenido,
		String tipoMedio,
		String extension,
		BigDecimal pesoMb,
		LocalDateTime fechaCreacion) {
}
