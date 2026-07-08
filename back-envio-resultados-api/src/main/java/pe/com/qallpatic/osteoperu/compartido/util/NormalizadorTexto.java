package pe.com.qallpatic.osteoperu.compartido.util;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza texto para comparaciones: sin tildes, MAYUSCULAS, espacios colapsados. */
public final class NormalizadorTexto {

	private NormalizadorTexto() {
	}

	public static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sinTildes.toUpperCase(Locale.ROOT)
				.replaceAll("[^A-Z0-9 ]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
