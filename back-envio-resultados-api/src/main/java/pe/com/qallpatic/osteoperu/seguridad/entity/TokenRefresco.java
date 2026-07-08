package pe.com.qallpatic.osteoperu.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Token de refresco (opaco) para renovar el access token JWT. */
@Entity
@Table(name = "TOKEN_REFRESCO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefresco {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_TOKEN")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_USUARIO", nullable = false)
	private Usuario usuario;

	@Column(name = "TOKEN", nullable = false, unique = true, length = 255)
	private String token;

	@Column(name = "FECHA_EXPIRACION", nullable = false)
	private LocalDateTime fechaExpiracion;

	@Column(name = "REVOCADO", nullable = false)
	private boolean revocado;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;

	public boolean esValido() {
		return !revocado && fechaExpiracion.isAfter(LocalDateTime.now());
	}
}
