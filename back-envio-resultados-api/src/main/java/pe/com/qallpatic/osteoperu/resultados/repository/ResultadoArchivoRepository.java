package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ResultadoArchivoRepository extends JpaRepository<ResultadoArchivo, Long> {

	Optional<ResultadoArchivo> findByIdArchivo(String idArchivo);

	List<ResultadoArchivo> findByIdGenerado(String idGenerado);

	/**
	 * Suma el peso (en MB) de TODOS los archivos registrados, sin filtrar por estado.
	 * Justificacion: en OSTEOPERU los archivos NO se borran fisicamente del
	 * almacenamiento — solo se hace soft-delete (estado=0). Para metricas de uso
	 * real del almacenamiento, hay que contar todos los registros, ya que el
	 * blob sigue ocupando disco en el bucket.
	 */
	@Query("SELECT COALESCE(SUM(a.pesoMb), 0) FROM ResultadoArchivo a")
	BigDecimal sumPesoMb();
}