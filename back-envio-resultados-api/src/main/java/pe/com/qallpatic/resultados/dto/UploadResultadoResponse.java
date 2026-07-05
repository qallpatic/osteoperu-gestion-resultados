package pe.com.qallpatic.resultados.dto;

import lombok.Builder;

import java.util.List;

/** Respuesta al panel tras subir los resultados de un paciente. */
@Builder
public record UploadResultadoResponse(
		Long batchId,
		String phone,
		String dni,
		String folderName,
		int totalFiles,
		List<String> storedFiles,
		String shortLink,
		String viewerUrl,
		String smsStatus) {
}
