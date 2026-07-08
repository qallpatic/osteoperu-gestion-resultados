package pe.com.qallpatic.osteoperu.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import pe.com.qallpatic.osteoperu.compartido.auditoria.EntidadAuditable;

import java.util.HashSet;
import java.util.Set;

/**
 * Usuario de acceso al sistema. Para los pacientes, el login ({@code usuario})
 * es su DNI. La contrasena se almacena como hash BCrypt.
 */
@Entity
@Table(name = "USUARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_USUARIO")
	private Long id;

	@Column(name = "USUARIO", nullable = false, unique = true, length = 100)
	private String usuario;

	@Column(name = "NOMBRE", length = 120)
	private String nombre;

	@Column(name = "APELLIDO", length = 120)
	private String apellido;

	@Column(name = "CORREO", length = 150)
	private String correo;

	@Column(name = "CONTRASENA", nullable = false, length = 100)
	private String contrasena;

	@Column(name = "DNI", length = 15)
	private String dni;

	@Column(name = "ESTADO", nullable = false)
	private boolean estado;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "USUARIO_ROL",
			joinColumns = @JoinColumn(name = "ID_USUARIO"),
			inverseJoinColumns = @JoinColumn(name = "ID_ROL"))
	@Builder.Default
	private Set<Rol> roles = new HashSet<>();

	public void agregarRol(Rol rol) {
		this.roles.add(rol);
	}
}
