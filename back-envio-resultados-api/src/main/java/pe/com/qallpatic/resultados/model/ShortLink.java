package pe.com.qallpatic.resultados.model;

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
 * Acortador de links. El {@code shortCode} es lo que viaja en el SMS y
 * redirige a {@code originalUrl} (normalmente el visor /r/{token}).
 */
@Entity
@Table(name = "short_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "short_code", nullable = false, unique = true, length = 16)
	private String shortCode;

	@Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
	private String originalUrl;

	@Column(length = 20)
	private String type;

	@Column(name = "batch_id")
	private Long batchId;

	@Column(name = "click_count", nullable = false)
	private int clickCount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;
}
