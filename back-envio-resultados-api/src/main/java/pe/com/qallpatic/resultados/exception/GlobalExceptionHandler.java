package pe.com.qallpatic.resultados.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "pe.com.qallpatic.resultados.controller")
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> handleMaxSize(MaxUploadSizeExceededException ex) {
		return build(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamano maximo permitido.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
		log.error("Error no controlado", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
	}

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
		Map<String, Object> body = Map.of(
				"timestamp", LocalDateTime.now().toString(),
				"status", status.value(),
				"error", status.getReasonPhrase(),
				"message", message == null ? "" : message);
		return ResponseEntity.status(status).body(body);
	}
}
