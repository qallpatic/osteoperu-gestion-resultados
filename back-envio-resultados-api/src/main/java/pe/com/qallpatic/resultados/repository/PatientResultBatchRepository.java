package pe.com.qallpatic.resultados.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.resultados.model.PatientResultBatch;

import java.util.Optional;

public interface PatientResultBatchRepository extends JpaRepository<PatientResultBatch, Long> {

	@EntityGraph(attributePaths = "files")
	Optional<PatientResultBatch> findByPublicToken(String publicToken);
}
