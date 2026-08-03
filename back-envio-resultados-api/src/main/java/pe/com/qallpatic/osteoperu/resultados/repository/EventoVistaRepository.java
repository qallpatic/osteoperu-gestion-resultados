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
	 * Alimenta la curva temporal de la pestana "Vistas de pacientes".
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
	 * [2] codigoCorto
	 * [3] idResultado
	 * [4] ip
	 * [5] userAgent
	 * </pre>
	 */
	@Query("""
			SELECT e.fechaCreacion, e.tipoEvento, e.codigoCorto, e.idResultado,
			       e.ip, e.userAgent
			FROM EventoVista e
			WHERE e.fechaCreacion BETWEEN :desde AND :hasta
			ORDER BY e.fechaCreacion DESC
			""")
	List<Object[]> listarParaReporte(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);
}
