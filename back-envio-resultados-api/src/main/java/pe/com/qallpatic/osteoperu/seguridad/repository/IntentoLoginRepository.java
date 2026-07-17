package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoLogin;

import java.time.LocalDateTime;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {

	@Query("SELECT COUNT(i) FROM IntentoLogin i "
			+ "WHERE i.ipAddress = :ip AND i.usuario = :usuario "
			+ "AND i.bloqueado = false AND i.fechaRegistro >= :desde")
	long contarFallidosPorIpYUsuario(@Param("ip") String ip, @Param("usuario") String usuario,
			@Param("desde") LocalDateTime desde);

	@Query("SELECT COUNT(i) FROM IntentoLogin i "
			+ "WHERE i.usuario = :usuario AND i.bloqueado = false AND i.fechaRegistro >= :desde")
	long contarFallidosPorUsuario(@Param("usuario") String usuario, @Param("desde") LocalDateTime desde);

	@Query("SELECT MAX(i.fechaRegistro) FROM IntentoLogin i "
			+ "WHERE i.ipAddress = :ip AND i.usuario = :usuario "
			+ "AND i.bloqueado = false AND i.fechaRegistro >= :desde")
	LocalDateTime masRecienteFallidoPorIpYUsuario(@Param("ip") String ip, @Param("usuario") String usuario,
			@Param("desde") LocalDateTime desde);

	/** Purga de intentos anteriores al corte (housekeeping). */
	@Modifying
	@Query("DELETE FROM IntentoLogin i WHERE i.fechaRegistro < :corte")
	int eliminarAnterioresA(@Param("corte") LocalDateTime corte);
}
