package pe.com.qallpatic.resultados.model;

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
@Table(name = "view_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "batch_id", nullable = false)
	private Long batchId;

	@Column(name = "short_code", length = 16)
	private String shortCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 20)
	private EventType eventType;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	@Column(name = "user_agent", length = 512)
	private String userAgent;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
