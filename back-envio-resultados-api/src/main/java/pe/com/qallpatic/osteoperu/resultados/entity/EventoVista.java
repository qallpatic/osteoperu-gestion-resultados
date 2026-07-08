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

/** Traza de cada click al link corto y cada vista/descarga del paciente. */
@Entity
@Table(name = "EVENTO_VISTA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoVista {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_EVENTO")
	private Long id;

	@Column(name = "ID_RESULTADO", nullable = false)
	private Long idResultado;

	@Column(name = "CODIGO_CORTO", length = 16)
	private String codigoCorto;

	@Enumerated(EnumType.STRING)
	@Column(name = "TIPO_EVENTO", nullable = false, length = 20)
	private TipoEvento tipoEvento;

	@Column(name = "IP", length = 64)
	private String ip;

	@Column(name = "USER_AGENT", length = 512)
	private String userAgent;

	@Column(name = "FECHA_CREACION", nullable = false)
	private LocalDateTime fechaCreacion;
}
