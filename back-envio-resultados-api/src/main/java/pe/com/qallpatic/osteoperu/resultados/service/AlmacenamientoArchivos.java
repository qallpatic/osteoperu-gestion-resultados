package pe.com.qallpatic.osteoperu.resultados.service;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Abstraccion de almacenamiento de archivos. Permite alternar entre FS local
 * (desarrollo) y S3-compatible (produccion / MinIO) sin acoplar al resto de la app.
 *
 * <p>Estructura del object key (S3) o ruta relativa (local):
 * <pre>{@code <documentoIdentidad>/<YYYY-MM-DD>/<idGenerado>/<idArchivo>.<extension>}</pre>
 *
 * <p>Las dos primeras partes (DNI + fecha ISO) son derivables desde
 * {@code ResultadoCarga} (cabecera) y permiten:
 * <ul>
 *   <li>Agrupar visualmente los archivos de un paciente por fecha en el bucket.</li>
 *   <li>Aplicar lifecycle rules por prefix temporal.</li>
 *   <li>Listar nativamente con {@code LIST bucket/&lt;DNI&gt;/} sin filtrar.</li>
 *   <li>Politicas IAM granulares por paciente en el futuro.</li>
 * </ul>
 */
public interface AlmacenamientoArchivos {

	/**
	 * Sube el contenido y devuelve el {@code idArchivo} generado (sufijo unico dentro
	 * de la carpeta de la transaccion).
	 *
	 * @param contenido   stream con los bytes del archivo
	 * @param sizeBytes   tamano en bytes
	 * @param contentType MIME type (ej: application/pdf)
	 * @param extension   extension sin punto (ej: pdf, png)
	 * @param idGenerado  business key de la cabecera (transaccion), ya validado
	 * @param documentoIdentidad DNI o CE normalizado, ya validado (solo digitos)
	 * @param fechaHoraCarga   fecha/hora de la carga (de la cabecera)
	 * @return idArchivo generado (alfanumerico, unico dentro de la carpeta)
	 */
	String guardar(InputStream contenido, long sizeBytes, String contentType, String extension,
			String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga);

	/**
	 * Recupera el contenido de un archivo para servirlo al cliente.
	 *
	 * @param idGenerado        business key de la cabecera
	 * @param idArchivo         id del archivo (sufijo del object key)
	 * @param extension         extension sin punto
	 * @param documentoIdentidad DNI/CE de la cabecera (para construir el prefix)
	 * @param fechaHoraCarga    fecha/hora de la cabecera (para construir el prefix)
	 */
	ArchivoDescargable descargar(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga);

	/**
	 * Health-check: verifica que el backend puede comunicarse con el storage.
	 */
	boolean disponible();

	/**
	 * Construye el object key / ruta relativa completa para un archivo.
	 */
	String construirKey(String idGenerado, String idArchivo, String extension,
			String documentoIdentidad, LocalDateTime fechaHoraCarga);

	/**
	 * Construye el prefijo "carpeta" de una transaccion (sin el idArchivo final).
	 * Util para respuestas y para hacer LIST por transaccion.
	 */
	String construirCarpeta(String idGenerado, String documentoIdentidad, LocalDateTime fechaHoraCarga);
}
