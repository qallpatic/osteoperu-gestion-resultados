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
 * Mapea el formato de decolecta (first_name / first_last_name / second_last_name).
 */
@Component
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
			if (r.firstName() == null && r.firstLastName() == null) {
				// Solo llego el nombre completo
				return (r.fullName() != null && !r.fullName().isBlank())
						? Optional.of(new DatosReniec(dni, r.fullName(), null, null))
						: Optional.empty();
			}
			return Optional.of(new DatosReniec(dni, r.firstName(), r.firstLastName(), r.secondLastName()));
		} catch (Exception e) {
			log.error("Error consultando RENIEC para dni {}: {}", dni, e.getMessage());
			return Optional.empty();
		}
	}

	/** Respuesta de decolecta (/v1/reniec/dni). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RespuestaReniec(
			@JsonProperty("first_name") String firstName,
			@JsonProperty("first_last_name") String firstLastName,
			@JsonProperty("second_last_name") String secondLastName,
			@JsonProperty("full_name") String fullName) {
	}
}
