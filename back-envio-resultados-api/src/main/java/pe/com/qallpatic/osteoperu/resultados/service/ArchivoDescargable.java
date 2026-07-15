package pe.com.qallpatic.osteoperu.resultados.service;

import java.io.InputStream;

/**
 * Resultado de descargar un archivo del storage. Se usa como cuerpo de
 * {@code ResponseEntity<Resource>} en los endpoints que sirven archivos.
 */
public record ArchivoDescargable(
		InputStream contenido,
		long sizeBytes,
		String contentType,
		String nombreOriginal) {
}
