package pe.com.qallpatic.osteoperu.resultados.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detalle: un archivo dentro de una {@link ResultadoCarga}.
 *
 * <p>Object key S3 = {@code <keyPrefix><idGenerado>_<idArchivo>.<extension>}
 * (la combinacion de los dos IDs denormalizados construye la ruta completa).
 */
@Entity
@Table(name = "RESULTADO_ARCHIVO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoArchivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_RESULTADO_ARCHIVO")
	private Long idResultadoArchivo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_RESULTADO_CARGA", nullable = false)
	private ResultadoCarga resultadoCarga;

	@Column(name = "ID_GENERADO", nullable = false, length = 64)
	private String idGenerado;

	@Column(name = "ID_ARCHIVO", nullable = false, unique = true, length = 64)
	private String idArchivo;

	@Column(name = "NOMBRE_ORIGINAL", nullable = false, length = 255)
	private String nombreOriginal;

	@Column(name = "TIPO_CONTENIDO", nullable = false, length = 120)
	private String tipoContenido;

	@Enumerated(EnumType.STRING)
	@Column(name = "TIPO_MEDIO", nullable = false, length = 20)
	private TipoMedio tipoMedio;

	@Column(name = "EXTENSION", length = 10)
	private String extension;

	@Column(name = "PESO_MB", nullable = false, precision = 12, scale = 4)
	private BigDecimal pesoMb;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;
}
