package pe.com.qallpatic.osteoperu.integracion.captcha;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;
import pe.com.qallpatic.osteoperu.compartido.excepcion.ExcepcionCaptchaInvalidoException;
import pe.com.qallpatic.osteoperu.config.RecaptchaProperties;

/**
 * Implementacion HTTP real contra Google siteverify. Se activa cuando
 * {@code recaptcha.enabled=true}.
 * <p>
 * Siteverify responde:
 * <pre>
 * {
 *   "success": true|false,
 *   "score": 0.0..1.0,
 *   "action": "login",
 *   "challenge_ts": "...",
 *   "hostname": "...",
 *   "error-codes": ["..."]
 * }
 * </pre>
 * <p>
 * Se considera valido solo si success=true Y score &gt;= minScore configurado.
 */
@Component("verificadorCaptchaHttp")
@ConditionalOnProperty(prefix = "recaptcha", name = "enabled", havingValue = "true")
@Slf4j
public class VerificadorCaptchaHttp implements VerificadorCaptcha {

	private final RecaptchaProperties propiedades;
	private final WebClient webClient;

	public VerificadorCaptchaHttp(RecaptchaProperties propiedades, WebClient.Builder builder) {
		this.propiedades = propiedades;
		this.webClient = builder.build();
	}

	@Override
	public void verificar(String token, String ipRemota) {
		if (token == null || token.isBlank()) {
			throw new ExcepcionCaptchaInvalidoException("Falta el token de verificacion.");
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", propiedades.getSecretKey());
		form.add("response", token);
		if (ipRemota != null && !ipRemotaEsInvalida(ipRemota)) {
			form.add("remoteip", ipRemota);
		}

		RespuestaSitioVerify r;
		try {
			r = webClient.post()
					.uri(propiedades.getVerifyUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.bodyValue(form)
					.retrieve()
					.bodyToMono(RespuestaSitioVerify.class)
					.timeout(Duration.ofMillis(propiedades.getTimeoutMs()))
					.block();
		} catch (WebClientResponseException e) {
			log.warn("[CAPTCHA] siteverify respondio HTTP {}.", e.getStatusCode().value());
			throw new ExcepcionCaptchaInvalidoException(
					"No se pudo validar la verificacion de seguridad. Intenta nuevamente.");
		} catch (Exception e) {
			log.error("[CAPTCHA] Error consultando siteverify: {}", e.getMessage());
			throw new ExcepcionCaptchaInvalidoException(
					"No se pudo validar la verificacion de seguridad. Intenta nuevamente.");
		}

		if (r == null) {
			throw new ExcepcionCaptchaInvalidoException(
					"Respuesta vacia del servicio de verificacion de seguridad.");
		}

		List<String> errores = r.errorCodes() == null ? Collections.emptyList() : r.errorCodes();
		if (!Boolean.TRUE.equals(r.success())) {
			log.warn("[CAPTCHA] Verificacion rechazada por Google. errores={}", errores);
			throw ExcepcionCaptchaInvalidoException.porScore(0.0, propiedades.getMinScore(), errores);
		}

		double score = r.score() == null ? 0.0 : r.score();
		if (score < propiedades.getMinScore()) {
			log.warn("[CAPTCHA] Score {} por debajo del umbral {}.", score, propiedades.getMinScore());
			throw ExcepcionCaptchaInvalidoException.porScore(score, propiedades.getMinScore(), errores);
		}

		log.info("[CAPTCHA] Verificacion OK. score={}", score);
	}

	private static boolean ipRemotaEsInvalida(String ip) {
		return "desconocida".equals(ip) || ip.isBlank();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RespuestaSitioVerify(
			Boolean success,
			Double score,
			@JsonProperty("action") String action,
			@JsonProperty("hostname") String hostname,
			@JsonProperty("error-codes") List<String> errorCodes,
			@JsonProperty("challenge_ts") String challengeTs) {
	}
}
