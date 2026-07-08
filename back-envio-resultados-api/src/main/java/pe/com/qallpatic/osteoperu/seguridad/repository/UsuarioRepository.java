package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

	Optional<Usuario> findByUsuario(String usuario);

	boolean existsByUsuario(String usuario);

	boolean existsByDni(String dni);
}
