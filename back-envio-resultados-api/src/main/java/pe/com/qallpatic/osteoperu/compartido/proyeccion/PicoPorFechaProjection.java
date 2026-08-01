package pe.com.qallpatic.osteoperu.compartido.proyeccion;

import java.time.LocalDate;

/**
 * Proyeccion JPQL: pico de registros en un dia concreto. Usado por el Dashboard
 * para resaltar el dia con mas registros de usuarios.
 */
public interface PicoPorFechaProjection {

	LocalDate getFecha();

	long getTotal();
}