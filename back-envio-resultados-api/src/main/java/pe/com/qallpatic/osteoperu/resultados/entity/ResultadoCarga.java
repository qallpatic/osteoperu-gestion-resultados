package pe.com.qallpatic.osteoperu.resultados.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.com.qallpatic.osteoperu.compartido.auditoria.EntidadAuditable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de una transaccion de carga de resultados.
 * Cada {@code POST /api/resultados/upload} produce UNA cabecera con N detalles ({@link ResultadoArchivo}).
 *
 * <p>Business key publica: {@link #idGenerado} (UUID-like).
 * <p>URL publica del visor: {@code /r/{tokenPublico}}.
 */
@Entity
@Table(name = "RESULTADO_CARGA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoCarga extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_RESULTADO_CARGA")
	private Long idResultadoCarga;

	@Column(name = "ID_GENERADO", nullable = false, unique = true, length = 64)
	private String idGenerado;

	@Column(name = "TOKEN_PUBLICO", nullable = false, unique = true, length = 64)
	private String tokenPublico;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_PACIENTE")
	private pe.com.qallpatic.osteoperu.pacientes.entity.Paciente paciente;

	@Column(name = "DOCUMENTO_IDENTIDAD", nullable = false, length = 20)
	private String documentoIdentidad;

	@Column(name = "TELEFONO", length = 20)
	private String telefono;

	@Column(name = "NOMBRE_PACIENTE", length = 150)
	private String nombrePaciente;

	@Column(name = "MENSAJE", length = 500)
	private String mensaje;

	@Column(name = "ESTADO", nullable = false)
	private Integer estado;

	@Column(name = "FECHA_ELIMINACION")
	private LocalDateTime fechaEliminacion;

	@Column(name = "USUARIO_ELIMINACION", length = 100)
	private String usuarioEliminacion;

	@Column(name = "MOTIVO_ELIMINACION", length = 500)
	private String motivoEliminacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "ESTADO_SMS", nullable = false, length = 20)
	private EstadoSms estadoSms;

	@Column(name = "CONTADOR_VISTAS", nullable = false)
	private Integer contadorVistas;

	@Column(name = "FECHA_PRIMERA_VISTA")
	private LocalDateTime fechaPrimeraVista;

	@Column(name = "FECHA_ULTIMA_VISTA")
	private LocalDateTime fechaUltimaVista;

	@Column(name = "CANTIDAD_ARCHIVOS", nullable = false)
	private Integer cantidadArchivos;

	@Column(name = "PESO_TOTAL_MB", nullable = false, precision = 14, scale = 4)
	private BigDecimal pesoTotalMb;

	@Column(name = "FECHA_HORA_CARGA", nullable = false)
	private LocalDateTime fechaHoraCarga;

	@OneToMany(mappedBy = "resultadoCarga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<ResultadoArchivo> archivos = new ArrayList<>();

	public void addArchivo(ResultadoArchivo archivo) {
		archivo.setResultadoCarga(this);
		archivos.add(archivo);
	}
}
