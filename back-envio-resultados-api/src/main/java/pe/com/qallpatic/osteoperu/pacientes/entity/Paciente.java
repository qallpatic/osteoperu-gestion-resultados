package pe.com.qallpatic.osteoperu.pacientes.entity;

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

import pe.com.qallpatic.osteoperu.compartido.auditoria.EntidadAuditable;

/** Maestro de pacientes (demografia + vinculo con su cuenta de acceso). */
@Entity
@Table(name = "PACIENTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_PACIENTE")
	private Long id;

	@Column(name = "DNI", nullable = false, length = 15)
	private String dni;

	@Column(name = "NOMBRE", length = 120)
	private String nombre;

	@Column(name = "NOMBRES", length = 120)
	private String nombres;

	@Column(name = "APELLIDO_PATERNO", length = 120)
	private String apellidoPaterno;

	@Column(name = "APELLIDO_MATERNO", length = 120)
	private String apellidoMaterno;

	@Column(name = "TELEFONO", length = 20)
	private String telefono;

	@Column(name = "CORREO", length = 150)
	private String correo;

	@Column(name = "ID_USUARIO")
	private Long idUsuario;

	@Column(name = "ESTADO", nullable = false)
	private boolean estado;

	/** true = registrado por Admision, con resultados pendientes de subir. */
	@Column(name = "PENDIENTE_RESULTADOS", nullable = false)
	private boolean pendienteResultados;
}
