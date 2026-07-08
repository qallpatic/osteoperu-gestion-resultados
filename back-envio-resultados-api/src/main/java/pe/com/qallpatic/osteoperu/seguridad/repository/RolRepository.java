package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.osteoperu.seguridad.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

	Optional<Rol> findByNombre(String nombre);
}
