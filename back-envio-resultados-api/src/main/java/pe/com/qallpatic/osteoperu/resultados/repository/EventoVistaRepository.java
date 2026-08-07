package pe.com.qallpatic.osteoperu.resultados.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.resultados.entity.EventoVista;

public interface EventoVistaRepository extends JpaRepository<EventoVista, Long> {

	/**
	 * Cuenta eventos de tipo VISTA agrupados por dia dentro de un rango.
	 * Solo aperturas del visor (no descargas). Mantenido por compat con
	 * consumidores externos; el dashboard usa {@link #conteoInteraccionesPorDia}.
	 * Devuelve filas crudas [fecha (LocalDate), total (Long)] para que el
	 * service las densifique (rellene dias sin vistas con 0).
	 */
	@Query("""
			SELECT FUNCTION('DATE', e.fechaCreacion) AS dia, COUNT(e) AS total
			FROM EventoVista e
			WHERE e.tipoEvento = pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.VISTA
			  AND e.fechaCreacion BETWEEN :desde AND :hasta
			GROUP BY FUNCTION('DATE', e.fechaCreacion)
			ORDER BY FUNCTION('DATE', e.fechaCreacion) ASC
			""")
	List<Object[]> conteoVistasPorDia(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Cuenta interacciones del paciente (aperturas del visor + descargas de
	 * archivos) agrupadas por dia. Alimenta la curva temporal de la pestana
	 * "Vistas de pacientes" del dashboard. Excluye CLICK (apertura del link
	 * corto del SMS) porque ocurre antes del engagement con los resultados.
	 * Devuelve filas crudas [fecha (LocalDate), total (Long)] para que el
	 * service las densifique.
	 */
	@Query("""
			SELECT FUNCTION('DATE', e.fechaCreacion) AS dia, COUNT(e) AS total
			FROM EventoVista e
			WHERE e.tipoEvento IN (
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.VISTA,
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA)
			  AND e.fechaCreacion BETWEEN :desde AND :hasta
			GROUP BY FUNCTION('DATE', e.fechaCreacion)
			ORDER BY FUNCTION('DATE', e.fechaCreacion) ASC
			""")
	List<Object[]> conteoInteraccionesPorDia(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Lista plana de TODOS los eventos del rango (CLICK, VISTA, DESCARGA) para
	 * el reporte Excel. Orden DESC por fechaCreacion para ver lo mas reciente
	 * primero. NO incluye paciente del cabecere: el service resuelve el join
	 * por su cuenta (EventoVista guarda idResultado como Long escalar, sin
	 * relacion JPA a ResultadoCarga) via {@code ResultadoCargaRepository.findAllById}.
	 *
	 * <p>Orden de columnas (debe coincidir con el mapeo en el service):
	 * <pre>
	 * [0] fechaCreacion
	 * [1] tipoEvento
	 * [2] idResultado
	 * [3] ip
	 * [4] userAgent
	 * </pre>
	 */
	@Query("""
			SELECT e.fechaCreacion, e.tipoEvento, e.idResultado,
			       e.ip, e.userAgent
			FROM EventoVista e
			WHERE e.fechaCreacion BETWEEN :desde AND :hasta
			ORDER BY e.fechaCreacion DESC
			""")
	List<Object[]> listarParaReporte(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Cuenta eventos DESCARGA agrupados por dia dentro de un rango. Alimenta
	 * la curva inferior del stacked bar del dashboard de vistas. Devuelve
	 * filas crudas [fecha (LocalDate), total (Long)].
	 */
	@Query("""
			SELECT FUNCTION('DATE', e.fechaCreacion) AS dia, COUNT(e) AS total
			FROM EventoVista e
			WHERE e.tipoEvento = pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA
			  AND e.fechaCreacion BETWEEN :desde AND :hasta
			GROUP BY FUNCTION('DATE', e.fechaCreacion)
			ORDER BY FUNCTION('DATE', e.fechaCreacion) ASC
			""")
	List<Object[]> conteoDescargasPorDia(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Total de eventos en el rango agrupados por tipo (CLICK / VISTA /
	 * DESCARGA). Alimenta los KPI mini del dashboard de vistas
	 * (aperturas totales, descargas totales). Devuelve filas crudas
	 * [tipoEvento, total (Long)].
	 */
	@Query("""
			SELECT e.tipoEvento, COUNT(e)
			FROM EventoVista e
			WHERE e.fechaCreacion BETWEEN :desde AND :hasta
			GROUP BY e.tipoEvento
			""")
	List<Object[]> conteoPorTipoEnRango(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Total historico de eventos agrupados por tipo (CLICK / VISTA /
	 * DESCARGA) sin filtro de fecha. Alimenta el KPI "Vistas de pacientes"
	 * del dashboard resumen, que muestra el engagement acumulado de la
	 * plataforma (numero estable, no cambia con el rango seleccionado).
	 * Devuelve filas crudas [tipoEvento, total (Long)].
	 */
	@Query("""
			SELECT e.tipoEvento, COUNT(e)
			FROM EventoVista e
			GROUP BY e.tipoEvento
			""")
	List<Object[]> conteoTotalPorTipo();

	/**
	 * Cantidad de cabeceras DISTINTAS que tuvieron al menos un evento
	 * (apertura o descarga) en el rango. Alimenta la tasa de adopcion del
	 * dashboard: cabecerasConActividad / cabecerasVivasTotales.
	 */
	@Query("""
			SELECT COUNT(DISTINCT e.idResultado)
			FROM EventoVista e
			WHERE e.tipoEvento IN (
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.VISTA,
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA)
			  AND e.fechaCreacion BETWEEN :desde AND :hasta
			""")
	long cabecerasConActividadEnRango(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/**
	 * Conteo de interacciones agrupadas por dia-de-la-semana (1-7, convencion
	 * MySQL DAYOFWEEK default: 1=Domingo, 7=Sabado) y hora (0-23). Alimenta
	 * el heatmap del dashboard. Devuelve filas crudas
	 * [diaSemana (Integer), hora (Integer), total (Long)]; el service
	 * densifica la grilla 7x24 (168 celdas) rellenando con 0 las celdas
	 * sin actividad. La hora pico y el total de la hora pico se derivan
	 * de estos datos en Java.
	 */
	@Query("""
			SELECT FUNCTION('DAYOFWEEK', e.fechaCreacion) AS diaSemana,
			       FUNCTION('HOUR', e.fechaCreacion) AS hora,
			       COUNT(e) AS total
			FROM EventoVista e
			WHERE e.tipoEvento IN (
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.VISTA,
			       pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA)
			  AND e.fechaCreacion BETWEEN :desde AND :hasta
			GROUP BY FUNCTION('DAYOFWEEK', e.fechaCreacion),
			         FUNCTION('HOUR', e.fechaCreacion)
			""")
	List<Object[]> conteoPorDiaSemanaYHoraEnRango(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);
}
