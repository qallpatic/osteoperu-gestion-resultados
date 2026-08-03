package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.domain.Pageable;
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

	/**
	 * Distribucion por tipoContenido (MIME type). Sin filtrar soft-delete:
	 * el blob sigue ocupando espacio en el bucket aunque este "anulado".
	 * Devuelve filas crudas (Object[]) para evitar proyecciones intermedias:
	 * [tipoContenido, cantidad, pesoMb].
	 */
	@Query("""
			SELECT a.tipoContenido, COUNT(a), COALESCE(SUM(a.pesoMb), 0)
			FROM ResultadoArchivo a
			GROUP BY a.tipoContenido
			ORDER BY COUNT(a) DESC
			""")
	List<Object[]> distribucionPorTipoContenido();

	/**
	 * Top N archivos mas pesados del sistema (para alimentar tabla y barras
	 * en la pestana Almacenamiento). Sin filtrar soft-delete — el peso
	 * fisico en el bucket es independiente del "estado" logico.
	 */
	@Query("""
			SELECT a.idGenerado, a.nombreOriginal, a.tipoContenido, a.pesoMb
			FROM ResultadoArchivo a
			ORDER BY a.pesoMb DESC
			""")
	List<Object[]> topArchivosMasPesados(Pageable pageable);

	/**
	 * MB subidos por dia (suma del peso de los archivos creados ese dia).
	 * Sin filtrar soft-delete. La curva "acumulada" se calcula en servicio
	 * recorriendo la lista y sumando incrementalmente (dataset chico, no
	 * amerita un window function nativo).
	 */
	@Query("""
			SELECT FUNCTION('DATE', a.fechaCreacion) AS dia, COALESCE(SUM(a.pesoMb), 0)
			FROM ResultadoArchivo a
			GROUP BY FUNCTION('DATE', a.fechaCreacion)
			ORDER BY FUNCTION('DATE', a.fechaCreacion) ASC
			""")
	List<Object[]> mbSubidosPorDia();
}
