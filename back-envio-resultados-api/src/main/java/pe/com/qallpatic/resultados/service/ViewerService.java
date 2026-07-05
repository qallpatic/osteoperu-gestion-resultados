package pe.com.qallpatic.resultados.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.resultados.exception.NotFoundException;
import pe.com.qallpatic.resultados.model.EventType;
import pe.com.qallpatic.resultados.model.PatientResultBatch;
import pe.com.qallpatic.resultados.model.ResultFile;
import pe.com.qallpatic.resultados.model.ViewEvent;
import pe.com.qallpatic.resultados.repository.PatientResultBatchRepository;
import pe.com.qallpatic.resultados.repository.ViewEventRepository;

import java.time.LocalDateTime;

/** Logica del visor de resultados: registra vistas/clicks y marca "visto". */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewerService {

	private final PatientResultBatchRepository batchRepository;
	private final ViewEventRepository viewEventRepository;

	/** Registra un CLICK (apertura del link corto) asociado al lote. */
	@Transactional
	public void registerClick(Long batchId, String shortCode, String ip, String userAgent) {
		viewEventRepository.save(ViewEvent.builder()
				.batchId(batchId)
				.shortCode(shortCode)
				.eventType(EventType.CLICK)
				.ipAddress(ip)
				.userAgent(userAgent)
				.createdAt(LocalDateTime.now())
				.build());
	}

	/**
	 * Carga el lote por token, registra la VISTA, incrementa el contador y
	 * marca la primera vez que el paciente vio sus resultados.
	 */
	@Transactional
	public PatientResultBatch loadAndRegisterView(String token, String ip, String userAgent) {
		PatientResultBatch batch = batchRepository.findByPublicToken(token)
				.orElseThrow(() -> new NotFoundException("Resultados no encontrados."));

		LocalDateTime now = LocalDateTime.now();
		batch.setViewCount(batch.getViewCount() + 1);
		if (batch.getFirstViewedAt() == null) {
			batch.setFirstViewedAt(now);
			log.info("Paciente vio sus resultados por primera vez. batch={}", batch.getId());
		}
		batch.setLastViewedAt(now);
		batch.setUpdatedAt(now);
		batchRepository.save(batch);

		viewEventRepository.save(ViewEvent.builder()
				.batchId(batch.getId())
				.eventType(EventType.VIEW)
				.ipAddress(ip)
				.userAgent(userAgent)
				.createdAt(now)
				.build());

		return batch;
	}

	/** Devuelve el lote por token sin registrar vista (para servir archivos). */
	@Transactional(readOnly = true)
	public PatientResultBatch getByToken(String token) {
		return batchRepository.findByPublicToken(token)
				.orElseThrow(() -> new NotFoundException("Resultados no encontrados."));
	}

	@Transactional
	public void registerDownload(Long batchId, String ip, String userAgent) {
		viewEventRepository.save(ViewEvent.builder()
				.batchId(batchId)
				.eventType(EventType.DOWNLOAD)
				.ipAddress(ip)
				.userAgent(userAgent)
				.createdAt(LocalDateTime.now())
				.build());
	}

	public ResultFile findFile(PatientResultBatch batch, Long fileId) {
		return batch.getFiles().stream()
				.filter(f -> f.getId().equals(fileId))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("Archivo no encontrado."));
	}
}
