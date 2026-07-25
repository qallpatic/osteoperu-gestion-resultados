package pe.com.qallpatic.osteoperu.resultados.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Solicitud de edicion de archivos de una cabecera existente.
 * <p>El motivo es OBLIGATORIO para cada archivo desactivado: se captura en el
 * dialog de confirmacion del frontend y se envia en {@code motivosPorArchivo}
 * como map {idArchivo: motivo}. No hay motivo general: cada archivo
 * desactivado tiene su propio texto en la bitacora.
 */
public record ActualizarArchivosRequest(
		List<Long> idsADesactivar,
		List<Long> idsAReactivar,
		List<MultipartFile> archivosNuevos,
		Map<Long, String> motivosPorArchivo,
		boolean reenviarSms) {
}