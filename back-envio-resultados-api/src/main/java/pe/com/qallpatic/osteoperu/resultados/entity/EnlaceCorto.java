package pe.com.qallpatic.osteoperu.resultados.entity;

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
 * Acortador de links. El {@code codigoCorto} es lo que viaja en el SMS y
 * redirige a {@code urlOriginal} (normalmente el visor /r/{token}).
 */
@Entity
@Table(name = "ENLACE_CORTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnlaceCorto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ENLACE")
	private Long id;

	@Column(name = "CODIGO_CORTO", nullable = false, unique = true, length = 16)
	private String codigoCorto;

	@Column(name = "URL_ORIGINAL", nullable = false, columnDefinition = "TEXT")
	private String urlOriginal;

	@Column(name = "TIPO", length = 20)
	private String tipo;

	@Column(name = "ID_RESULTADO")
	private Long idResultado;

	@Column(name = "CONTADOR_CLICKS", nullable = false)
	private int contadorClicks;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "FECHA_EXPIRACION")
	private LocalDateTime fechaExpiracion;
}
