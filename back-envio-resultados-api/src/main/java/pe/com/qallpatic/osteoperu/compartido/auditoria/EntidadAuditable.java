package pe.com.qallpatic.osteoperu.compartido.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Campos de auditoria comunes a las entidades, con nombres de columna en
 * espanol/MAYUSCULAS. Se aplicara a las entidades de dominio a partir de la
 * Fase 4 (esquema en espanol).
 *
 * <p>Las fechas se completan automaticamente mediante callbacks JPA; los
 * campos USUARIO_* los asigna la capa de servicio (o un {@code AuditorAware}
 * cuando exista la seguridad JWT).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class EntidadAuditable {

	@Column(name = "FECHA_CREACION", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "USUARIO_CREACION", length = 100, updatable = false)
	private String usuarioCreacion;

	@Column(name = "FECHA_MODIFICACION")
	private LocalDateTime fechaModificacion;

	@Column(name = "USUARIO_MODIFICACION", length = 100)
	private String usuarioModificacion;

	@PrePersist
	protected void alCrear() {
		this.fechaCreacion = LocalDateTime.now();
		this.fechaModificacion = this.fechaCreacion;
	}

	@PreUpdate
	protected void alActualizar() {
		this.fechaModificacion = LocalDateTime.now();
	}
}
