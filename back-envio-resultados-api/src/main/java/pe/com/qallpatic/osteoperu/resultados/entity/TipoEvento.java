package pe.com.qallpatic.osteoperu.resultados.entity;

/** Tipo de interaccion registrada del paciente con sus resultados. */
public enum TipoEvento {
	/** El paciente abrio el link corto del SMS. */
	CLICK,
	/** Se renderizo la pagina de resultados. */
	VISTA,
	/** El paciente descargo/abrio un archivo. */
	DESCARGA
}
