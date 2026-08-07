package pe.com.qallpatic.osteoperu.dashboard.dto;

/**
 * Punto de la distribucion horaria del dashboard de vistas. Eje X = hora
 * del dia (0-23), eje Y = cantidad de interacciones (aperturas + descargas)
 * registradas en esa hora a lo largo del rango.
 *
 * @param hora hora local (0-23) ya densificada por el backend.
 * @param total cantidad de eventos VISTA + DESCARGA en esa hora.
 */
public record PuntoHorario(int hora, long total) {
}
