package pe.com.qallpatic.osteoperu.resultados.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Solicitud de edicion de archivos de una cabecera existente.
 * <p>El controller desempaca el multipart y construye este objeto antes de
 * delegar al service. Mantenerlo como record cohesivo evita firmas largas
 * y facilita la prueba del service en aislado.
 *
 * <p>Motivos por archivo: si {@code motivosPorArchivo} tiene una entrada
 * para un id de {@code idsADesactivar}, ese motivo individual se aplica
 * SOLO a ese archivo. Si no, se usa {@code motivo} como general para todos
 * los desactivados en esta operacion.
 */
public record ActualizarArchivosRequest(
		List<Long> idsADesactivar,
		List<Long> idsAReactivar,
		List<MultipartFile> archivosNuevos,
		String motivo,
		Map<Long, String> motivosPorArchivo,
		boolean reenviarSms) {
}