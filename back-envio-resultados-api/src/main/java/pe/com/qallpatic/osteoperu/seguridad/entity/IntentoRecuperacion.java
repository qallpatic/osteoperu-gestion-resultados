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
 * Auditoria de los intentos de recuperacion de password. Permite aplicar las
 * reglas de rate-limit con ventana deslizante sin depender de un store externo.
 * La limpieza la hace {@code LimpiezaIntentosRecuperacionService}.
 */
@Entity
@Table(name = "INTENTO_RECUPERACION_PASSWORD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoRecuperacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "IP_ADDRESS", nullable = false, length = 45)
	private String ipAddress;

	@Column(name = "TELEFONO_NORM", nullable = false, length = 20)
	private String telefonoNorm;

	@Column(name = "BLOQUEADO", nullable = false)
	private boolean bloqueado;

	@Column(name = "FECHA_REGISTRO", nullable = false)
	private LocalDateTime fechaRegistro;
}
