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

import java.time.LocalDateTime;

/** Archivo (pdf/imagen/video) asociado a un {@link Resultado}. */
@Entity
@Table(name = "ARCHIVO_RESULTADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoResultado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ARCHIVO")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ID_RESULTADO", nullable = false)
	private Resultado resultado;

	@Column(name = "NOMBRE_ORIGINAL", nullable = false, length = 255)
	private String nombreOriginal;

	@Column(name = "NOMBRE_ALMACENADO", nullable = false, length = 255)
	private String nombreAlmacenado;

	@Column(name = "TIPO_CONTENIDO", length = 120)
	private String tipoContenido;

	@Enumerated(EnumType.STRING)
	@Column(name = "TIPO_MEDIO", nullable = false, length = 20)
	private TipoMedio tipoMedio;

	@Column(name = "TAMANO_BYTES", nullable = false)
	private long tamanoBytes;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;
}
