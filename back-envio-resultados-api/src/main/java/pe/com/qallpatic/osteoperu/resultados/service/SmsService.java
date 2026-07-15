package pe.com.qallpatic.osteoperu.resultados.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import pe.com.qallpatic.osteoperu.config.SmsProperties;
import pe.com.qallpatic.osteoperu.resultados.dto.SmsGatewayRequest;
import pe.com.qallpatic.osteoperu.resultados.entity.EstadoSms;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;

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
	private final ResultadoCargaRepository resultadoCargaRepository;

	private final BlockingQueue<SmsTask> queue = new LinkedBlockingQueue<>();

	private record SmsTask(Long idResultado, String phone, String message) {
	}

	@PostConstruct
	public void startWorker() {
		Thread worker = new Thread(this::processQueue, "sms-sender");
		worker.setDaemon(true);
		worker.start();
		log.info("SMS worker iniciado. Envio real habilitado: {}", smsProperties.isEnabled());
	}

	/** Encola un SMS de texto libre (no asociado a un resultado). */
	public void enviarTexto(String phone, String message) {
		try {
			queue.put(new SmsTask(null, phone, message));
			log.info("SMS de texto encolado -> {}", phone);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("No se pudo encolar SMS de texto para {}", phone, e);
		}
	}

	/** Construye el mensaje a partir de la plantilla y lo encola. */
	public void enqueueResultLink(Long idResultado, String phone, String patientName, String link) {
		String message = smsProperties.getMessage().getTemplate()
				.replace("{nombre}", patientName == null ? "" : patientName)
				.replace("{link}", link)
				.trim();
		try {
			queue.put(new SmsTask(idResultado, phone, message));
			updateStatus(idResultado, EstadoSms.EN_COLA);
			log.info("SMS encolado para resultado {} -> {}", idResultado, phone);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("No se pudo encolar SMS para resultado {}", idResultado, e);
			updateStatus(idResultado, EstadoSms.FALLIDO);
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
			log.warn("[SMS DESHABILITADO] resultado {} -> {} | {}", task.idResultado(), task.phone(), task.message());
			updateStatus(task.idResultado(), EstadoSms.DESHABILITADO);
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
			updateStatus(task.idResultado(), EstadoSms.ENVIADO);
			log.info("SMS enviado correctamente para resultado {} -> {}", task.idResultado(), task.phone());
		} catch (Exception e) {
			log.error("Error enviando SMS para resultado {} -> {}: {}",
					task.idResultado(), task.phone(), e.getMessage());
			updateStatus(task.idResultado(), EstadoSms.FALLIDO);
		}
	}

	private void updateStatus(Long idResultado, EstadoSms estado) {
		if (idResultado == null) {
			return;
		}
		try {
			resultadoCargaRepository.findById(idResultado).ifPresent(carga -> {
				carga.setEstadoSms(estado);
				resultadoCargaRepository.save(carga);
			});
		} catch (Exception e) {
			log.error("No se pudo actualizar el estado SMS de la carga {}", idResultado, e);
		}
	}
}
