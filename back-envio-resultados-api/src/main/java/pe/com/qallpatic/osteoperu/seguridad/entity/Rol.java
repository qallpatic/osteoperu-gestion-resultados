package pe.com.qallpatic.osteoperu.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.com.qallpatic.osteoperu.compartido.auditoria.EntidadAuditable;

/** Rol del sistema: ADMIN, RESULTADOS, ADMISION, PACIENTE, CALLCENTER. */
@Entity
@Table(name = "ROL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class Rol extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_ROL")
	private Long id;

	@Column(name = "NOMBRE", nullable = false, unique = true, length = 30)
	private String nombre;

	@Column(name = "DESCRIPCION", length = 150)
	private String descripcion;

	@Column(name = "ESTADO", nullable = false)
	private boolean estado;
}
