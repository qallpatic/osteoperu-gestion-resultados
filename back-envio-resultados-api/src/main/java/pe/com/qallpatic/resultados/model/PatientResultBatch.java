package pe.com.qallpatic.resultados.model;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote de resultados de un paciente. Cada lote corresponde a una carpeta
 * fisica {telefono}_{dni} y a un link unico enviado por SMS.
 */
@Entity
@Table(name = "patient_result_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResultBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(nullable = false, length = 20)
	private String dni;

	@Column(name = "folder_name", nullable = false, length = 80)
	private String folderName;

	@Column(name = "public_token", nullable = false, unique = true, length = 64)
	private String publicToken;

	@Enumerated(EnumType.STRING)
	@Column(name = "sms_status", nullable = false, length = 20)
	private SmsStatus smsStatus;

	@Column(name = "view_count", nullable = false)
	private int viewCount;

	@Column(name = "first_viewed_at")
	private LocalDateTime firstViewedAt;

	@Column(name = "last_viewed_at")
	private LocalDateTime lastViewedAt;

	@Column(name = "created_by", length = 100)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<ResultFile> files = new ArrayList<>();

	public void addFile(ResultFile file) {
		file.setBatch(this);
		this.files.add(file);
	}
}
