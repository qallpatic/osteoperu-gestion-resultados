package pe.com.qallpatic.osteoperu.resultados.entity;

/** Clasificacion simple del archivo para decidir como mostrarlo en el visor. */
public enum TipoMedio {
	PDF,
	IMAGEN,
	VIDEO,
	OTRO;

	public static TipoMedio desdeTipoContenido(String tipoContenido) {
		if (tipoContenido == null) {
			return OTRO;
		}
		String ct = tipoContenido.toLowerCase();
		if (ct.equals("application/pdf")) {
			return PDF;
		}
		if (ct.startsWith("image/")) {
			return IMAGEN;
		}
		if (ct.startsWith("video/")) {
			return VIDEO;
		}
		return OTRO;
	}
}
