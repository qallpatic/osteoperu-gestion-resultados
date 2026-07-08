package pe.com.qallpatic.osteoperu.compartido.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaError;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Manejo centralizado de excepciones para los controladores REST ({@code @RestController}).
 * No intercepta los controladores MVC/Thymeleaf (por ejemplo el visor de resultados),
 * que gestionan sus propias vistas de error.
 */
@Slf4j
@ControllerAdvice(annotations = RestController.class)
public class ManejadorGlobalExcepciones {

	@ExceptionHandler(ExcepcionNegocio.class)
	public ResponseEntity<RespuestaError> manejarNegocio(ExcepcionNegocio ex, HttpServletRequest req) {
		return construir(ex.getEstado(), ex.getMessage(), req);
	}

	@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
	public ResponseEntity<RespuestaError> manejarViolacion(
			jakarta.validation.ConstraintViolationException ex, HttpServletRequest req) {
		return construir(HttpStatus.BAD_REQUEST,
				ex.getMessage() == null ? "Datos invalidos." : ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex,
			HttpServletRequest req) {
		String detalle = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return construir(HttpStatus.BAD_REQUEST, detalle.isBlank() ? "Datos invalidos." : detalle, req);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<RespuestaError> manejarAutenticacion(AuthenticationException ex, HttpServletRequest req) {
		return construir(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", req);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<RespuestaError> manejarTamano(MaxUploadSizeExceededException ex, HttpServletRequest req) {
		return construir(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamano maximo permitido.", req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<RespuestaError> manejarGenerico(Exception ex, HttpServletRequest req) {
		log.error("Error no controlado", ex);
		return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.", req);
	}

	private ResponseEntity<RespuestaError> construir(HttpStatus estado, String mensaje, HttpServletRequest req) {
		RespuestaError cuerpo = new RespuestaError(
				Instant.now(),
				estado.value(),
				estado.getReasonPhrase(),
				mensaje == null ? "" : mensaje,
				req == null ? null : req.getRequestURI());
		return ResponseEntity.status(estado).body(cuerpo);
	}
}
