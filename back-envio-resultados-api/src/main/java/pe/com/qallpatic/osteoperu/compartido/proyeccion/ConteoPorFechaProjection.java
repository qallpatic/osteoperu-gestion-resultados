package pe.com.qallpatic.osteoperu.compartido.proyeccion;

import java.time.LocalDate;

/**
 * Proyeccion JPQL: conteo agrupado por dia. Usado por el modulo Dashboard
 * para series temporales (resultados cargados por dia, usuarios por dia, etc.).
 */
public interface ConteoPorFechaProjection {

	LocalDate getFecha();

	long getTotal();
}