package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.compartido.proyeccion.ConteoPorFechaProjection;
import pe.com.qallpatic.osteoperu.compartido.proyeccion.PicoPorFechaProjection;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

	Optional<Usuario> findByUsuario(String usuario);

	boolean existsByUsuario(String usuario);

	boolean existsByDni(String dni);

	/** Conteo de usuarios creados dentro de un rango (incluye extremos). */
	@Query("SELECT COUNT(u) FROM Usuario u WHERE u.fechaCreacion BETWEEN :desde AND :hasta")
	long countByFechaCreacionBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

	/** Serie diaria de usuarios creados en un rango. Una fila por dia con su total. */
	@Query("SELECT FUNCTION('DATE', u.fechaCreacion) AS fecha, COUNT(u) AS total "
			+ "FROM Usuario u "
			+ "WHERE u.fechaCreacion BETWEEN :desde AND :hasta "
			+ "GROUP BY FUNCTION('DATE', u.fechaCreacion) "
			+ "ORDER BY FUNCTION('DATE', u.fechaCreacion) ASC")
	List<ConteoPorFechaProjection> conteoPorFechaCreacion(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);

	/** Dia(s) con mayor cantidad de registros historicamente. Pageable para tomar top N. */
	@Query("SELECT FUNCTION('DATE', u.fechaCreacion) AS fecha, COUNT(u) AS total "
			+ "FROM Usuario u "
			+ "GROUP BY FUNCTION('DATE', u.fechaCreacion) "
			+ "ORDER BY COUNT(u) DESC, FUNCTION('DATE', u.fechaCreacion) DESC")
	List<PicoPorFechaProjection> picoRegistrosUsuarios(Pageable pageable);

	/**
	 * Lista plana para el reporte Excel de "Usuarios registrados".
	 * Devuelve TODOS los usuarios del rango con sus roles eager-loaded
	 * (un usuario sin roles devuelve una lista vacia, no falla).
	 * El service mapea las entidades a
	 * {@link pe.com.qallpatic.osteoperu.dashboard.dto.ReporteUsuarioRow}.
	 */
	@EntityGraph(attributePaths = "roles")
	@Query("SELECT u FROM Usuario u "
			+ "WHERE u.fechaCreacion BETWEEN :desde AND :hasta "
			+ "ORDER BY u.fechaCreacion DESC")
	List<Usuario> listarParaReporte(@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta);
}