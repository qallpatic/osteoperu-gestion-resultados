package pe.com.qallpatic.osteoperu.paciente.dto;

/**
 * Archivo del paciente ya validado (pertenece a un resultado suyo).
 * Contiene los IDs que permiten construir el object key en S3 o el path
 * en FS local sin necesidad de JOIN adicional.
 */
public record ArchivoLocalizado(
		String idGenerado,
		String idArchivo,
		String extension,
		String nombreOriginal,
		Long idResultadoCarga) {
}
