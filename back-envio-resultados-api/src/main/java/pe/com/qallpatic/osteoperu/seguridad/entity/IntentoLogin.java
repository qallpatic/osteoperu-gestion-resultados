package pe.com.qallpatic.osteoperu.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Auditoria de intentos de login fallidos admitidos o bloqueados por rate-limit.
 * Aplica a /api/auth/login. La limpieza la hace
 * {@code LimpiezaIntentosLoginService}.
 */
@Entity
@Table(name = "INTENTO_LOGIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoLogin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "IP_ADDRESS", nullable = false, length = 45)
	private String ipAddress;

	/** Login intentado. Se persiste tal cual llega del request (trim). */
	@Column(name = "USUARIO", nullable = false, length = 100)
	private String usuario;

	@Column(name = "BLOQUEADO", nullable = false)
	private boolean bloqueado;

	@Column(name = "FECHA_REGISTRO", nullable = false)
	private LocalDateTime fechaRegistro;
}
