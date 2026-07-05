package pe.com.qallpatic.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.resultados.model.ShortLink;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

	Optional<ShortLink> findByShortCode(String shortCode);

	boolean existsByShortCode(String shortCode);
}
