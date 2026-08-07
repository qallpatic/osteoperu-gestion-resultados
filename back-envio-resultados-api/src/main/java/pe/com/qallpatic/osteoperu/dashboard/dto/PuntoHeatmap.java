package pe.com.qallpatic.osteoperu.dashboard.dto;

/**
 * Celda del heatmap dia-de-la-semana x hora-del-dia del dashboard de
 * vistas. Combina dos dimensiones temporales en una sola mirada: revela
 * patrones circadianos Y semanales (ej: "martes a las 14hs siempre hay
 * pico"), util para optimizar la ventana de envio de SMS.
 *
 * @param diaSemana 1=Domingo, 2=Lunes, ..., 7=Sabado (convencion de
 *                  MySQL DAYOFWEEK default). El front se encarga de
 *                  reordenar para mostrar L M M J V S D.
 * @param hora hora local 0-23.
 * @param total cantidad de interacciones (aperturas + descargas) en esa
 *             celda a lo largo del rango.
 */
public record PuntoHeatmap(int diaSemana, int hora, long total) {
}
