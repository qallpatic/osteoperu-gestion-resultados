package pe.com.qallpatic.resultados.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import pe.com.qallpatic.resultados.config.SmsProperties;
import pe.com.qallpatic.resultados.dto.SmsGatewayRequest;
import pe.com.qallpatic.resultados.model.SmsStatus;
import pe.com.qallpatic.resultados.repository.PatientResultBatchRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Encola y envia los SMS al gateway Textbee en un hilo dedicado, respetando la
 * frecuencia configurada (evita saturar el gateway en envios seguidos).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

	private final SmsProperties smsProperties;
	private final WebClient.Builder webClientBuilder;
	private final PatientResultBatchRepository batchRepository;

	private final BlockingQueue<SmsTask> queue = new LinkedBlockingQueue<>();

	private record SmsTask(Long batchId, String phone, String message) {
	}

	@PostConstruct
	public void startWorker() {
		Thread worker = new Thread(this::processQueue, "sms-sender");
		worker.setDaemon(true);
		worker.start();
		log.info("SMS worker iniciado. Envio real habilitado: {}", smsProperties.isEnabled());
	}

	/** Construye el mensaje a partir de la plantilla y lo encola. */
	public void enqueueResultLink(Long batchId, String phone, String patientName, String link) {
		String message = smsProperties.getMessage().getTemplate()
				.replace("{nombre}", patientName == null ? "" : patientName)
				.replace("{link}", link)
				.trim();
		try {
			queue.put(new SmsTask(batchId, phone, message));
			updateStatus(batchId, SmsStatus.QUEUED);
			log.info("SMS encolado para batch {} -> {}", batchId, phone);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("No se pudo encolar SMS para batch {}", batchId, e);
			updateStatus(batchId, SmsStatus.FAILED);
		}
	}

	private void processQueue() {
		while (true) {
			try {
				SmsTask task = queue.take();
				send(task);
				Thread.sleep(smsProperties.getTextbee().getFrequencyMs());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("Worker de SMS interrumpido", e);
				return;
			}
		}
	}

	private void send(SmsTask task) {
		if (!smsProperties.isEnabled()) {
			log.warn("[SMS DESHABILITADO] batch {} -> {} | {}", task.batchId(), task.phone(), task.message());
			updateStatus(task.batchId(), SmsStatus.DISABLED);
			return;
		}
		try {
			SmsGatewayRequest body = new SmsGatewayRequest(List.of("+" + task.phone()), task.message());
			webClientBuilder.build().post()
					.uri(smsProperties.getTextbee().getUrl())
					.header("x-api-key", smsProperties.getTextbee().getApiKey())
					.header("Content-Type", "application/json")
					.bodyValue(body)
					.retrieve()
					.toEntity(String.class)
					.block();
			updateStatus(task.batchId(), SmsStatus.SENT);
			log.info("SMS enviado correctamente para batch {} -> {}", task.batchId(), task.phone());
		} catch (Exception e) {
			log.error("Error enviando SMS para batch {} -> {}: {}", task.batchId(), task.phone(), e.getMessage());
			updateStatus(task.batchId(), SmsStatus.FAILED);
		}
	}

	private void updateStatus(Long batchId, SmsStatus status) {
		try {
			batchRepository.findById(batchId).ifPresent(batch -> {
				batch.setSmsStatus(status);
				batch.setUpdatedAt(LocalDateTime.now());
				batchRepository.save(batch);
			});
		} catch (Exception e) {
			log.error("No se pudo actualizar el estado SMS del batch {}", batchId, e);
		}
	}
}
