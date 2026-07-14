package pe.com.qallpatic.osteoperu.integracion.reniec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Implementacion HTTP real (activa cuando {@code reniec.enabled=true}). Consulta
 * el proveedor configurado en {@code reniec.url} con {@code reniec.token}.
 * <p>JSON real que devuelve decolecta:
 * <pre>
 * {
 *   "first_name": "REYLY MABELY",
 *   "first_last_name": "JULCA",
 *   "second_last_name": "MELENDRES",
 *   "full_name": "JULCA MELENDRES REYLY MABELY",
 *   "document_number": "75700133"
 * }
 * </pre>
 * <p>Normalizamos los campos a la representacion canonica del proyecto:
 * {@code nombres} = first_name, {@code apellidoPaterno} = first_last_name,
 * {@code apellidoMaterno} = second_last_name.
 */
@Component("clienteReniecHttp")
@ConditionalOnProperty(prefix = "reniec", name = "enabled", havingValue = "true")
@Slf4j
public class ClienteReniecHttp implements ClienteReniec {

	private final ReniecProperties propiedades;
	private final WebClient webClient;

	public ClienteReniecHttp(ReniecProperties propiedades, WebClient.Builder builder) {
		this.propiedades = propiedades;
		this.webClient = builder.build();
	}

	@Override
	public Optional<DatosReniec> consultar(String dni) {
		try {
			RespuestaReniec r = webClient.get()
					.uri(propiedades.getUrl() + "?numero=" + dni)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.getToken())
					.header(HttpHeaders.ACCEPT, "application/json")
					.retrieve()
					.bodyToMono(RespuestaReniec.class)
					.timeout(Duration.ofMillis(propiedades.getTimeoutMs()))
					.block();

			if (r == null) {
				return Optional.empty();
			}

			String nombres = blankToNull(r.firstName());
			String apPat = blankToNull(r.firstLastName());
			String apMat = blankToNull(r.secondLastName());

			// Si no llega NADA, no devolvemos DatosReniec vacio: queremos que el
			// cache NO persista un registro inutil.
			if (nombres == null && apPat == null && apMat == null) {
				return Optional.empty();
			}
			return Optional.of(new DatosReniec(dni, nombres, apPat, apMat));
		} catch (Exception e) {
			log.error("Error consultando RENIEC para dni {}: {}", dni, e.getMessage());
			return Optional.empty();
		}
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	/** Respuesta de decolecta (/v1/reniec/dni). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RespuestaReniec(
			@JsonProperty("first_name") String firstName,
			@JsonProperty("first_last_name") String firstLastName,
			@JsonProperty("second_last_name") String secondLastName,
			@JsonProperty("full_name") String fullName,
			@JsonProperty("document_number") String documentNumber) {
	}
}