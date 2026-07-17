package pe.com.qallpatic.osteoperu.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.qallpatic.osteoperu.seguridad.entity.IntentoRecuperacion;

import java.time.LocalDateTime;

public interface IntentoRecuperacionRepository extends JpaRepository<IntentoRecuperacion, Long> {

	/** Cantidad total de intentos (exitosos o bloqueados) de la IP en la ventana. */
	@Query("SELECT COUNT(i) FROM IntentoRecuperacion i "
			+ "WHERE i.ipAddress = :ip AND i.fechaRegistro >= :desde")
	long contarPorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

	/** Cantidad de intentos asociados al DNI/CE normalizado en la ventana. */
	@Query("SELECT COUNT(i) FROM IntentoRecuperacion i "
			+ "WHERE i.documentoNorm = :documento AND i.fechaRegistro >= :desde")
	long contarPorDocumento(@Param("documento") String documento, @Param("desde") LocalDateTime desde);

	/** Cantidad de DNIs distintos solicitados por la IP en la ventana. */
	@Query("SELECT COUNT(DISTINCT i.documentoNorm) FROM IntentoRecuperacion i "
			+ "WHERE i.ipAddress = :ip AND i.fechaRegistro >= :desde")
	long contarDistintosPorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

	/** Fecha del intento mas reciente de la IP (para calcular retryAfter). */
	@Query("SELECT MAX(i.fechaRegistro) FROM IntentoRecuperacion i "
			+ "WHERE i.ipAddress = :ip AND i.fechaRegistro >= :desde")
	LocalDateTime masRecientePorIp(@Param("ip") String ip, @Param("desde") LocalDateTime desde);

	/** Purga de intentos anteriores al corte (housekeeping). */
	@Modifying
	@Query("DELETE FROM IntentoRecuperacion i WHERE i.fechaRegistro < :corte")
	int eliminarAnterioresA(@Param("corte") LocalDateTime corte);
}
