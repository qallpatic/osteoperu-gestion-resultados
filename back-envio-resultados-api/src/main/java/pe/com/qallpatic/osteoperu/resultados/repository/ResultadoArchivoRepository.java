package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;

import java.util.List;
import java.util.Optional;

public interface ResultadoArchivoRepository extends JpaRepository<ResultadoArchivo, Long> {

	Optional<ResultadoArchivo> findByIdArchivo(String idArchivo);

	List<ResultadoArchivo> findByIdGenerado(String idGenerado);
}
