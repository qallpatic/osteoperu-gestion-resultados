package pe.com.qallpatic.osteoperu.dashboard.dto;

/**
 * Punto de la curva temporal de vistas, agregado desde
 * {@code EVENTO_VISTA} (no desde {@code CONTADOR_VISTAS}).
 *
 * <p>Esto refleja aperturas reales en el tiempo: cada vez que un paciente
 * entra al visor /r/{token} o descarga un archivo, queda una fila con su
 * timestamp. La curva es densa (un punto por dia, 0 si no hubo vistas).
 */
public record PuntoVistaDiaria(

		/** Fecha del dia (YYYY-MM-DD). */
		String fecha,

		/** Vistas realizadas ese dia (de todos los tokens). */
		long total) {
}
