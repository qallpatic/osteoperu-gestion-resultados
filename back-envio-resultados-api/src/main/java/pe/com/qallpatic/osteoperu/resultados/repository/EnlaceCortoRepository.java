package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.osteoperu.resultados.entity.EnlaceCorto;

import java.util.Optional;

public interface EnlaceCortoRepository extends JpaRepository<EnlaceCorto, Long> {

	Optional<EnlaceCorto> findByCodigoCorto(String codigoCorto);

	boolean existsByCodigoCorto(String codigoCorto);

	Optional<EnlaceCorto> findFirstByIdResultadoOrderByIdDesc(Long idResultado);
}
