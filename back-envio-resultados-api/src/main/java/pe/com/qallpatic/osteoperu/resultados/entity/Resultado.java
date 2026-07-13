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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.com.qallpatic.osteoperu.compartido.auditoria.EntidadAuditable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote de resultados de un paciente. Cada resultado corresponde a una carpeta
 * fisica {telefono}_{dni} y a un link unico enviado por SMS.
 */
@Entity
@Table(name = "RESULTADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resultado extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_RESULTADO")
	private Long id;

	@Column(name = "TELEFONO", length = 20)
	private String telefono;

	@Column(name = "DNI", nullable = false, length = 20)
	private String dni;

	@Column(name = "NOMBRE_PACIENTE", length = 150)
	private String nombrePaciente;

	@Column(name = "MENSAJE", length = 500)
	private String mensaje;

	@Column(name = "CARPETA", nullable = false, length = 80)
	private String carpeta;

	@Column(name = "TOKEN_PUBLICO", nullable = false, unique = true, length = 64)
	private String tokenPublico;

	@Enumerated(EnumType.STRING)
	@Column(name = "ESTADO_SMS", nullable = false, length = 20)
	private EstadoSms estadoSms;

	@Column(name = "CONTADOR_VISTAS", nullable = false)
	private int contadorVistas;

	@Column(name = "FECHA_PRIMERA_VISTA")
	private LocalDateTime fechaPrimeraVista;

	@Column(name = "FECHA_ULTIMA_VISTA")
	private LocalDateTime fechaUltimaVista;

	@OneToMany(mappedBy = "resultado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<ArchivoResultado> archivos = new ArrayList<>();

	public void addArchivo(ArchivoResultado archivo) {
		archivo.setResultado(this);
		this.archivos.add(archivo);
	}
}
