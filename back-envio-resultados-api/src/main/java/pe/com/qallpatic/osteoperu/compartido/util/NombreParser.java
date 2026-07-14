package pe.com.qallpatic.osteoperu.compartido.util;

/**
 * Parsea/serializa un nombre en formato canonico de RENIEC:
 * <pre>"APELLIDO_P APELLIDO_M, NOMBRES"</pre>
 * <p>Se usa para que las pantallas del front siempre muestren un solo
 * input "Nombre completo", pero internamente podamos guardar (y consultar)
 * los segmentos en sus columnas dedicadas.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si el string tiene coma, la parte antes es "apellidos" (se reparten
 *       entre paterno y materno por espacio) y la parte despues es
 *       "nombres".</li>
 *   <li>Si el string NO tiene coma (caso legado: "PUMACONDOR SALVADOR LUIS DAVID"),
 *       se toma como si todo fuera el nombre completo, intentando partirlo
 *       (ultimas 1-2 palabras como nombres, el resto como apellidos).</li>
 *   <li>Si el string viene vacio o null, todos los segmentos quedan null y
 *       {@link #esVacio()} devuelve {@code true}.</li>
 * </ul>
 */
public final class NombreParser {

	private final String nombres;
	private final String apellidoPaterno;
	private final String apellidoMaterno;
	private final String nombreCompleto;

	private NombreParser(String nombres, String apellidoPaterno, String apellidoMaterno) {
		this.nombres = nombres;
		this.apellidoPaterno = apellidoPaterno;
		this.apellidoMaterno = apellidoMaterno;
		this.nombreCompleto = componerCompleto(this.nombres, this.apellidoPaterno, this.apellidoMaterno);
	}

	/** Parsea un nombre completo en sus segmentos. */
	public static NombreParser parsear(String completo) {
		if (completo == null || completo.isBlank()) {
			return new NombreParser(null, null, null);
		}
		String normalizado = completo.trim().replaceAll("\\s+", " ");

		int coma = normalizado.indexOf(',');
		if (coma >= 0) {
			String apellidos = normalizado.substring(0, coma).trim();
			String noms = normalizado.substring(coma + 1).trim();
			String[] split = partirApellidos(apellidos);
			return new NombreParser(nullToEmpty(noms), split[0], split[1]);
		}
		// Sin coma: intentar partir asumiendo que los ultimos 1-2 tokens son nombres.
		String[] tokens = normalizado.split(" ");
		if (tokens.length <= 2) {
			// "JUAN PEREZ" → paterno=PEREZ, materno=null, nombres=JUAN
			return new NombreParser(tokens[0], tokens.length > 1 ? tokens[1] : null, null);
		}
		if (tokens.length == 3) {
			// "LUIS PEREZ GOMEZ" → paterno=PEREZ, materno=GOMEZ, nombres=LUIS
			return new NombreParser(tokens[0], tokens[1], tokens[2]);
		}
		// 4+ tokens: los ultimos 2 son nombres (asumimos 2 nombres tipico)
		String ap1 = tokens[tokens.length - 4];
		String ap2 = tokens[tokens.length - 3];
		StringBuilder noms = new StringBuilder();
		for (int i = tokens.length - 2; i < tokens.length; i++) {
			if (noms.length() > 0) {
				noms.append(' ');
			}
			noms.append(tokens[i]);
		}
		return new NombreParser(noms.toString(), ap1, ap2);
	}

	/** Devuelve el nombre canonico "APELLIDO_P APELLIDO_M, NOMBRES". */
	public String completo() {
		return nombreCompleto;
	}

	public String nombres() {
		return nombres;
	}

	public String apellidoPaterno() {
		return apellidoPaterno;
	}

	public String apellidoMaterno() {
		return apellidoMaterno;
	}

	public boolean esVacio() {
		return (nombres == null || nombres.isBlank())
				&& (apellidoPaterno == null || apellidoPaterno.isBlank())
				&& (apellidoMaterno == null || apellidoMaterno.isBlank());
	}

	private static String[] partirApellidos(String apellidos) {
		if (apellidos == null || apellidos.isBlank()) {
			return new String[] { null, null };
		}
		String[] t = apellidos.split("\\s+");
		if (t.length == 1) {
			return new String[] { t[0], null };
		}
		// 2+ tokens: paterno = primer token, materno = el resto
		StringBuilder mat = new StringBuilder();
		for (int i = 1; i < t.length; i++) {
			if (mat.length() > 0) {
				mat.append(' ');
			}
			mat.append(t[i]);
		}
		return new String[] { t[0], mat.toString() };
	}

	private static String componerCompleto(String nombres, String apellidoPaterno, String apellidoMaterno) {
		String aps = ((apellidoPaterno == null ? "" : apellidoPaterno) + " "
				+ (apellidoMaterno == null ? "" : apellidoMaterno)).trim().replaceAll("\\s+", " ");
		String noms = (nombres == null ? "" : nombres).trim().replaceAll("\\s+", " ");
		if (aps.isEmpty()) {
			return noms;
		}
		if (noms.isEmpty()) {
			return aps;
		}
		return aps + ", " + noms;
	}

	private static String nullToEmpty(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}