package pe.com.qallpatic.osteoperu.resultados.dto;

import lombok.Builder;

import java.util.List;

/** Respuesta al panel tras subir (o intentar subir) los resultados de un paciente. */
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
		String smsStatus,
		/** true = el nombre del PDF no coincide con RENIEC; no se guardo nada, requiere confirmacion. */
		boolean requiereConfirmacion,
		String nombreReniec,
		String nombrePdf) {
}
