package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.seguridad.entity.TokenRefresco;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;

import java.util.Optional;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Long> {

	Optional<TokenRefresco> findByToken(String token);

	@Modifying
	@Query("update TokenRefresco t set t.revocado = true where t.usuario = :usuario and t.revocado = false")
	void revocarTodosPorUsuario(@Param("usuario") Usuario usuario);
}
