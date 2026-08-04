package pe.com.qallpatic.osteoperu.pacientes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	Optional<Paciente> findByDni(String dni);

	List<Paciente> findAllByDni(String dni);

	List<Paciente> findByTelefono(String telefono);

	/**
	 * Lista los pacientes registrados (incluye los creados por Admision aunque
	 * aun no tengan resultados) con su conteo de cabeceras. Para el modulo Admision.
	 * El orden se controla via {@link Pageable#getSort()} (no lleva ORDER BY
	 * fijo para no chocar con el orden dinamico de Spring Data).
	 */
	@Query(value = "SELECT p.dni AS dni, p.nombre AS nombre, p.telefono AS telefono, "
			+ "(SELECT COUNT(r) FROM ResultadoCarga r WHERE r.documentoIdentidad = p.dni) AS totalResultados, "
			+ "(SELECT MAX(r.fechaHoraCarga) FROM ResultadoCarga r WHERE r.documentoIdentidad = p.dni) AS ultimaFecha, "
			+ "u.estado AS estadoUsuario, u.usuarioCreacion AS usuarioCreacion, "
			+ "u.fechaCreacion AS fechaCreacionCuenta, u.usuarioModificacion AS usuarioModificacion, "
			+ "u.fechaModificacion AS fechaModificacionCuenta "
			+ "FROM Paciente p LEFT JOIN Usuario u ON u.usuario = p.dni "
			+ "WHERE (:dni IS NULL OR p.dni LIKE CONCAT('%', :dni, '%')) "
			+ "AND (:nombre IS NULL OR LOWER(CONCAT(COALESCE(p.nombre, ''), CONCAT(' ', COALESCE(p.nombres, '')))) LIKE LOWER(CONCAT('%', :nombre, '%'))) "
			+ "AND (:apellidos IS NULL OR LOWER(CONCAT(COALESCE(p.apellidoPaterno, ''), "
			+ "CONCAT(' ', COALESCE(p.apellidoMaterno, p.nombre, '')))) LIKE LOWER(CONCAT('%', :apellidos, '%'))) "
			+ "AND (:desde IS NULL OR u.fechaCreacion >= :desde) "
			+ "AND (:hasta IS NULL OR u.fechaCreacion <= :hasta)",
			countQuery = "SELECT COUNT(p) FROM Paciente p LEFT JOIN Usuario u ON u.usuario = p.dni "
					+ "WHERE (:dni IS NULL OR p.dni LIKE CONCAT('%', :dni, '%')) "
					+ "AND (:nombre IS NULL OR LOWER(CONCAT(COALESCE(p.nombre, ''), CONCAT(' ', COALESCE(p.nombres, '')))) LIKE LOWER(CONCAT('%', :nombre, '%'))) "
					+ "AND (:apellidos IS NULL OR LOWER(CONCAT(COALESCE(p.apellidoPaterno, ''), "
					+ "CONCAT(' ', COALESCE(p.apellidoMaterno, p.nombre, '')))) LIKE LOWER(CONCAT('%', :apellidos, '%'))) "
					+ "AND (:desde IS NULL OR u.fechaCreacion >= :desde) "
					+ "AND (:hasta IS NULL OR u.fechaCreacion <= :hasta)")
	Page<PacienteResumenProj> resumenDesdePacientes(
			@Param("dni") String dni,
			@Param("nombre") String nombre,
			@Param("apellidos") String apellidos,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta,
			Pageable pageable);

	/** Pacientes con resultados pendientes de subir (campanita de Resultados). */
	Page<Paciente> findByPendienteResultadosTrueOrderByFechaCreacionDesc(Pageable pageable);

	long countByPendienteResultadosTrue();
}
