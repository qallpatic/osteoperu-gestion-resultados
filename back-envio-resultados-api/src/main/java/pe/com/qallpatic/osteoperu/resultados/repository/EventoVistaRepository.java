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
}
