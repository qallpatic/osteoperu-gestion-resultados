package pe.com.qallpatic.osteoperu.resultados.dto;

import lombok.Builder;

import java.util.List;

/** Respuesta al panel tras subir (o intentar subir) los resultados de un paciente. */
@Builder
public record UploadResultadoResponse(
		Long cargaId,
		String idGenerado,
		String phone,
		String dni,
		String folderKey,
		int totalFiles,
		List<String> storedFileIds,
		String shortLink,
		String viewerUrl,
		String smsStatus,
		boolean requiereConfirmacion,
		String nombreReniec,
		String nombrePdf) {
}
