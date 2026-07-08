package pe.com.qallpatic.osteoperu.paciente.dto;

import java.nio.file.Path;

/** Archivo del paciente ya validado (pertenece a un resultado suyo) y localizado en disco. */
public record ArchivoLocalizado(Path ruta, String nombreOriginal, Long idResultado) {
}
