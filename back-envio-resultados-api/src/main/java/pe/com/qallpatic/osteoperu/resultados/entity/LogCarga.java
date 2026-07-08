package pe.com.qallpatic.osteoperu.resultados.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Bitacora de cada intento de carga (incluye validacion RENIEC/PDF). */
@Entity
@Table(name = "LOG_CARGA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogCarga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_LOG")
	private Long id;

	@Column(name = "ID_RESULTADO")
	private Long idResultado;

	@Column(name = "DNI", length = 20)
	private String dni;

	@Column(name = "USUARIO_CARGA", length = 100)
	private String usuarioCarga;

	@Enumerated(EnumType.STRING)
	@Column(name = "ESTADO", nullable = false, length = 20)
	private EstadoCarga estado;

	@Column(name = "MENSAJE", length = 500)
	private String mensaje;

	@Column(name = "NOMBRE_RENIEC", length = 200)
	private String nombreReniec;

	@Column(name = "NOMBRE_PDF", length = 200)
	private String nombrePdf;

	@Column(name = "COINCIDE_NOMBRE")
	private Boolean coincideNombre;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;
}
