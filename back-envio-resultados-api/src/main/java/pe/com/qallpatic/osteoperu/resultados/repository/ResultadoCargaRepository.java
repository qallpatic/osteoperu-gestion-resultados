package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;

import java.util.Optional;

public interface ResultadoCargaRepository
		extends JpaRepository<ResultadoCarga, Long>, JpaSpecificationExecutor<ResultadoCarga> {

	@EntityGraph(attributePaths = "archivos")
	Optional<ResultadoCarga> findByTokenPublico(String tokenPublico);

	Optional<ResultadoCarga> findByIdGenerado(String idGenerado);

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
}
