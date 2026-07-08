package pe.com.qallpatic.osteoperu.seguridad.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import pe.com.qallpatic.osteoperu.compartido.respuesta.RespuestaError;

import java.io.IOException;
import java.time.Instant;

/** Responde 401 en JSON uniforme cuando falta o es invalido el token. */
@Component
@RequiredArgsConstructor
public class PuntoEntradaNoAutorizado implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		RespuestaError cuerpo = new RespuestaError(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"No autenticado. Debe iniciar sesion.",
				request.getRequestURI());
		objectMapper.writeValue(response.getWriter(), cuerpo);
	}
}
