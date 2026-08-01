package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDate;

/**
 * Dia con mayor cantidad de registros historicamente.
 * Si hay varios dias empatados, el backend devuelve el mas reciente
 * (definido en el ORDER BY del repositorio).
 */
public record PicoRegistrosResponse(LocalDate fecha, long total) {
}