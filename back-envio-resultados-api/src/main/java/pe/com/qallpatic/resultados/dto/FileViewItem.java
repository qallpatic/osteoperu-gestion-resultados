package pe.com.qallpatic.resultados.dto;

import pe.com.qallpatic.resultados.model.MediaType;

/** Item de archivo tal como lo consume la plantilla del visor. */
public record FileViewItem(
		Long id,
		String name,
		String url,
		String downloadUrl,
		MediaType mediaType,
		String sizeLabel) {

	public boolean isPdf() {
		return mediaType == MediaType.PDF;
	}

	public boolean isImage() {
		return mediaType == MediaType.IMAGE;
	}

	public boolean isVideo() {
		return mediaType == MediaType.VIDEO;
	}
}
