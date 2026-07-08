package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;

import java.util.Optional;

public interface ResultadoRepository
		extends JpaRepository<Resultado, Long>, JpaSpecificationExecutor<Resultado> {

	@EntityGraph(attributePaths = "archivos")
	Optional<Resultado> findByTokenPublico(String tokenPublico);

	boolean existsByDni(String dni);

	/** Pacientes (DNI) con resultados cargados, agrupados, para el modulo de Admision. */
	@Query(value = "SELECT r.dni AS dni, MAX(r.nombrePaciente) AS nombre, "
			+ "COUNT(r) AS totalResultados, MAX(r.fechaCreacion) AS ultimaFecha "
			+ "FROM Resultado r WHERE (:dni IS NULL OR r.dni LIKE CONCAT('%', :dni, '%')) "
			+ "GROUP BY r.dni ORDER BY MAX(r.fechaCreacion) DESC",
			countQuery = "SELECT COUNT(DISTINCT r.dni) FROM Resultado r "
					+ "WHERE (:dni IS NULL OR r.dni LIKE CONCAT('%', :dni, '%'))")
	Page<PacienteResumenProj> resumenPacientes(@Param("dni") String dni, Pageable pageable);
}
