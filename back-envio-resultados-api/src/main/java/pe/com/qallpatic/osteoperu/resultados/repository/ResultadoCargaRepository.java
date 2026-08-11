package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.compartido.proyeccion.ConteoPorFechaProjection;
import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResultadoCargaRepository
		extends JpaRepository<ResultadoCarga, Long>, JpaSpecificationExecutor<ResultadoCarga> {

	@EntityGraph(attributePaths = "archivos")
	Optional<ResultadoCarga> findByTokenPublico(String tokenPublico);

	Optional<ResultadoCarga> findByIdGenerado(String idGenerado);

	/**
	 * Carga la cabecera junto con sus archivos en una sola query (evita N+1
	 * y resuelve el problema del LAZY: despues de persistir archivos nuevos
	 * por separado, un {@code findById} simple no los refleja en la lista).
	 */
	@EntityGraph(attributePaths = "archivos")
	@Query("SELECT c FROM ResultadoCarga c WHERE c.idResultadoCarga = :id")
	Optional<ResultadoCarga> findByIdConArchivos(@Param("id") Long id);

	boolean existsByDocumentoIdentidadAndEstado(String documentoIdentidad, Integer estado);

	long countByDocumentoIdentidadAndEstado(String documentoIdentidad, Integer estado);

	/**
	 * Resumen por DNI (totales y ultima fecha) derivado de las cabeceras.
	 * Usado por el modulo de Admision.
	 */
	@Query(value = "SELECT r.documentoIdentidad AS dni, MAX(r.nombrePaciente) AS nombre, "
			+ "COUNT(r) AS totalResultados, MAX(r.fechaHoraCarga) AS ultimaFecha "
			+ "FROM ResultadoCarga r WHERE (:dni IS NULL OR r.documentoIdentidad LIKE CONCAT('%', :dni, '%')) "
			+ "GROUP BY r.documentoIdentidad ORDER BY MAX(r.fechaHoraCarga) DESC",
			countQuery = "SELECT COUNT(DISTINCT r.documentoIdentidad) FROM ResultadoCarga r "
					+ "WHERE (:dni IS NULL OR r.documentoIdentidad LIKE CONCAT('%', :dni, '%'))")
	Page<PacienteResumenProj> resumenPacientes(@Param("dni") String dni, Pageable pageable);

	/** Total de cabeceras por estado (1 = vivas, 0 = eliminadas por soft delete). */
	long countByEstado(Integer estado);

	/**
	 * Total de cabeceras con estado=vivo. Usado como denominador de la
	 * "tasa de adopcion" del dashboard de vistas (cabezeras engagemented
	 * sobre total de cabeceras vivas).
	 */
	@Query("SELECT COUNT(r) FROM ResultadoCarga r WHERE r.estado = 1")
	long countVivas();

	/** Total de cabeceras vivas creadas dentro de un rango. */
	@Query("SELECT COUNT(r) FROM ResultadoCarga r "
			+ "WHERE r.estado = :estado AND r.fechaHoraCarga BETWEEN :desde AND :hasta")
	long countByFechaHoraCargaBetween(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta, @Param("estado") Integer estado);

	/** Serie diaria de cabeceras vivas cargadas en un rango. */
	@Query("SELECT FUNCTION('DATE', r.fechaHoraCarga) AS fecha, COUNT(r) AS total "
			+ "FROM ResultadoCarga r "
			+ "WHERE r.estado = :estado AND r.fechaHoraCarga BETWEEN :desde AND :hasta "
			+ "GROUP BY FUNCTION('DATE', r.fechaHoraCarga) "
			+ "ORDER BY FUNCTION('DATE', r.fechaHoraCarga) ASC")
	List<ConteoPorFechaProjection> conteoPorFechaCarga(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta, @Param("estado") Integer estado);

	/** Suma de vistas (CONTADOR_VISTAS) de cabeceras vivas. Solo aperturas del visor. */
	@Query("SELECT COALESCE(SUM(r.contadorVistas), 0) FROM ResultadoCarga r WHERE r.estado = :estado")
	long sumContadorVistasByEstado(@Param("estado") Integer estado);

	/**
	 * Total de eventos DESCARGA asociados a cabeceras con el estado pedido.
	 * Se suma en Java al {@link #sumContadorVistasByEstado(Integer)} para
	 * obtener el engagement total del paciente (aperturas + descargas).
	 */
	@Query("""
			SELECT COUNT(e)
			FROM EventoVista e
			WHERE e.tipoEvento = pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA
			  AND e.idResultado IN (SELECT r.idResultadoCarga FROM ResultadoCarga r WHERE r.estado = :estado)
			""")
	long countDescargasByEstado(@Param("estado") Integer estado);

	/**
	 * Cabeceras vivas con algun tipo de actividad (apertura del visor o
	 * descarga), con su {@code CONTADOR_VISTAS} sin sumar descargas. El
	 * service mergea contra {@link #descargasPorCabecera()} y ordena por
	 * engagement total (vistas + descargas) antes de truncar al top N.
	 * Dataset chico (~50 cabeceras), por eso no hace falta paginar.
	 *
	 * <p>Orden de columnas (debe coincidir con el mapeo en el service):
	 * <pre>
	 * [0] idResultadoCarga
	 * [1] idGenerado
	 * [2] documentoIdentidad
	 * [3] nombrePaciente
	 * [4] contadorVistas
	 * [5] fechaHoraCarga
	 * </pre>
	 */
	@Query("""
			SELECT r.idResultadoCarga, r.idGenerado, r.documentoIdentidad, r.nombrePaciente,
			       r.contadorVistas, r.fechaHoraCarga
			FROM ResultadoCarga r
			WHERE r.estado = 1
			  AND (r.contadorVistas > 0
			       OR EXISTS (SELECT 1 FROM EventoVista e
			                  WHERE e.tipoEvento = pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA
			                    AND e.idResultado = r.idResultadoCarga))
			""")
	List<Object[]> cabecerasVivasConActividad();

	/**
	 * Descargas agrupadas por cabecera (idResultado + count de eventos
	 * DESCARGA). Alimenta la suma engagement total en el dashboard. Una
	 * cabecera sin descargas no aparece en el resultado.
	 *
	 * <p>Orden de columnas:
	 * <pre>
	 * [0] idResultado (Long)
	 * [1] total descargas (Long)
	 * </pre>
	 */
	@Query("""
			SELECT e.idResultado, COUNT(e)
			FROM EventoVista e
			WHERE e.tipoEvento = pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento.DESCARGA
			GROUP BY e.idResultado
			""")
	List<Object[]> descargasPorCabecera();

	/**
	 * Todas las cabeceras vivas con su contador de aperturas, para que el
	 * service arme los buckets (1, 2-5, 6-10, 11+) en Java sumando las
	 * descargas obtenidas por {@link #descargasPorCabecera()}. El dataset
	 * es chico (~50 cabeceras), por eso el merge se hace en memoria.
	 */
	@Query("SELECT r.idResultadoCarga, r.contadorVistas FROM ResultadoCarga r WHERE r.estado = 1")
	List<Object[]> cabecerasVivasConContador();

	/**
	 * Lista plana para el reporte Excel de "Resultados cargados". Devuelve
	 * TODAS las cabeceras del rango (vivas + soft-deleted) ordenadas por
	 * fechaHoraCarga descendente para que el ADMIN vea primero lo mas
	 * reciente. El service mapea los {@code Object[]} a
	 * {@link pe.com.qallpatic.osteoperu.dashboard.dto.ReporteResultadoRow}.
	 *
	 * <p>Orden de columnas (debe coincidir con el mapeo en el service):
	 * <pre>
	 * [0]  fechaHoraCarga
	 * [1]  documentoIdentidad
	 * [2]  nombrePaciente
	 * [3]  telefono
	 * [4]  cantidadArchivos
	 * [5]  pesoTotalMb
	 * [6]  estadoSms
	 * [7]  estado
	 * [8]  fechaEliminacion
	 * [9]  usuarioEliminacion
	 * [10] motivoEliminacion
	 * [11] usuarioCreacion
	 * </pre>
	 */
	@Query("""
			SELECT r.fechaHoraCarga, r.documentoIdentidad, r.nombrePaciente, r.telefono,
			       r.cantidadArchivos, r.pesoTotalMb, r.estadoSms, r.estado,
			       r.fechaEliminacion, r.usuarioEliminacion, r.motivoEliminacion,
			       r.usuarioCreacion
			FROM ResultadoCarga r
			WHERE r.fechaHoraCarga BETWEEN :desde AND :hasta
			ORDER BY r.fechaHoraCarga DESC
			""")
	List<Object[]> listarParaReporte(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);
}