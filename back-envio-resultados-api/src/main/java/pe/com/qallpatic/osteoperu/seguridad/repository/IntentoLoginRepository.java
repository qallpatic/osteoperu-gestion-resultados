package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoLogin;

import java.time.LocalDateTime;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {

	/** Cantidad de intentos (exitosos o bloqueados) de la IP en la ventana. */
	@Query("SELECT COUNT(i) FROM IntentoLogin i "
			+ "WHERE i.ipAddress = :ip AND i.fechaRegistro >= :desde")
	long contarPorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

	/** Cantidad de intentos del usuario en la ventana. */
	@Query("SELECT COUNT(i) FROM IntentoLogin i "
			+ "WHERE i.usuario = :usuario AND i.fechaRegistro >= :desde")
	long contarPorUsuario(@Param("usuario") String usuario, @Param("desde") LocalDateTime desde);

	/** Fecha del intento mas reciente de la IP (para calcular retryAfter). */
	@Query("SELECT MAX(i.fechaRegistro) FROM IntentoLogin i "
			+ "WHERE i.ipAddress = :ip AND i.fechaRegistro >= :desde")
	LocalDateTime masRecientePorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

	/** Purga de intentos anteriores al corte (housekeeping). */
	@Modifying
	@Query("DELETE FROM IntentoLogin i WHERE i.fechaRegistro < :corte")
	int eliminarAnterioresA(@Param("corte") LocalDateTime corte);
}
