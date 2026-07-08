package pe.com.qallpatic.osteoperu.resultados.dto;

import pe.com.qallpatic.osteoperu.resultados.entity.TipoMedio;

/** Item de archivo tal como lo consume la plantilla del visor. */
public record FileViewItem(
		Long id,
		String name,
		String url,
		String downloadUrl,
		TipoMedio mediaType,
		String sizeLabel) {

	public boolean isPdf() {
		return mediaType == TipoMedio.PDF;
	}

	public boolean isImage() {
		return mediaType == TipoMedio.IMAGEN;
	}

	public boolean isVideo() {
		return mediaType == TipoMedio.VIDEO;
	}
}
