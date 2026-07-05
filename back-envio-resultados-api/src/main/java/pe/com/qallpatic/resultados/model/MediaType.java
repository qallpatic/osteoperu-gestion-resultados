package pe.com.qallpatic.resultados.model;

/** Clasificacion simple del archivo para decidir como mostrarlo en el visor. */
public enum MediaType {
	PDF,
	IMAGE,
	VIDEO,
	OTHER;

	public static MediaType fromContentType(String contentType) {
		if (contentType == null) {
			return OTHER;
		}
		String ct = contentType.toLowerCase();
		if (ct.equals("application/pdf")) {
			return PDF;
		}
		if (ct.startsWith("image/")) {
			return IMAGE;
		}
		if (ct.startsWith("video/")) {
			return VIDEO;
		}
		return OTHER;
	}
}
