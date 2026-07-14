package pe.com.qallpatic.osteoperu.integracion.reniec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cache local de la respuesta del proveedor RENIEC por DNI.
 * Cada DNI se cobra 1 vez al proveedor; las siguientes consultas
 * dentro del TTL (configurado en {@code reniec.cache-dias}) salen
 * de esta tabla.
 */
@Entity
@Table(name = "RENIEC_CACHE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReniecCache {

	@Id
	@Column(name = "DNI", length = 8)
	private String dni;

	@Column(name = "NOMBRES", length = 120)
	private String nombres;

	@Column(name = "APELLIDO_PATERNO", length = 120)
	private String apellidoPaterno;

	@Column(name = "APELLIDO_MATERNO", length = 120)
	private String apellidoMaterno;

	@Column(name = "NOMBRE_COMPLETO", nullable = false, length = 250)
	private String nombreCompleto;

	@Column(name = "CODIGO_VERIFICACION")
	private Integer codigoVerificacion;

	@Column(name = "CONSULTAS", nullable = false)
	private int consultas;

	@Column(name = "FECHA_CONSULTA", nullable = false)
	private LocalDateTime fechaConsulta;

	@Column(name = "FECHA_EXPIRA", nullable = false)
	private LocalDateTime fechaExpira;
}