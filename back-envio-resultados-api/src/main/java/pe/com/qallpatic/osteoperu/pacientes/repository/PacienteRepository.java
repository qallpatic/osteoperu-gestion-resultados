package pe.com.qallpatic.osteoperu.pacientes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.pacientes.entity.Paciente;
import pe.com.qallpatic.osteoperu.resultados.dto.PacienteResumenProj;

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
			+ "(SELECT MAX(r.fechaHoraCarga) FROM ResultadoCarga r WHERE r.documentoIdentidad = p.dni) AS ultimaFecha "
			+ "FROM Paciente p WHERE (:dni IS NULL OR p.dni LIKE CONCAT('%', :dni, '%'))",
			countQuery = "SELECT COUNT(p) FROM Paciente p "
					+ "WHERE (:dni IS NULL OR p.dni LIKE CONCAT('%', :dni, '%'))")
	Page<PacienteResumenProj> resumenDesdePacientes(@Param("dni") String dni, Pageable pageable);

	/** Pacientes con resultados pendientes de subir (campanita de Resultados). */
	Page<Paciente> findByPendienteResultadosTrueOrderByFechaCreacionDesc(Pageable pageable);

	long countByPendienteResultadosTrue();
}
