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
 *
 * <p>Proveedor actual: <b>apiperu.dev</b> (POST con JSON).
 * <p>Peticion:
 * <pre>
 *   POST {reniec.url}
 *   Authorization: Bearer {reniec.token}
 *   Content-Type: application/json
 *
 *   { "dni": "42834028" }
 * </pre>
 *
 * <p>Respuesta (envelope):
 * <pre>
 *   {
 *     "success": true,
 *     "data": {
 *       "numero": "42834028",
 *       "nombre_completo": "LABAN GARCIA, CLAUDINA",
 *       "nombres": "CLAUDINA",
 *       "apellido_paterno": "LABAN",
 *       "apellido_materno": "GARCIA",
 *       "codigo_verificacion": 6,
 *       "direccion": "",
 *       "ubigeo": [ null, null, null ]
 *     },
 *     "time": 0.127
 *   }
 * </pre>
 *
 * <p>Normalizamos al formato canonico del proyecto:
 * {@code nombres} = data.nombres, {@code apellidoPaterno} = data.apellido_paterno,
 * {@code apellidoMaterno} = data.apellido_materno. El codigo de verificacion se
 * persiste cuando viene.
 *
 * <p><b>Nota historica</b>: hasta 2026-08-10 el cliente apuntaba a decolecta
 * (GET con query param {@code ?numero=}). El proveedor dejo de responder con
 * ese token y se migro a apiperu.dev. Si vuelve a cambiar, ver estructura de
 * {@code RespuestaApiperu} abajo y adaptar.
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
			RespuestaApiperu envelope = webClient.post()
					.uri(propiedades.getUrl())
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.getToken())
					.header(HttpHeaders.CONTENT_TYPE, "application/json")
					.header(HttpHeaders.ACCEPT, "application/json")
					.bodyValue(new SolicitudDni(dni))
					.retrieve()
					.bodyToMono(RespuestaApiperu.class)
					.timeout(Duration.ofMillis(propiedades.getTimeoutMs()))
					.block();

			if (envelope == null) {
				log.warn("RENIEC (apiperu.dev) respondio cuerpo vacio para dni={}", dni);
				return Optional.empty();
			}
			if (envelope.success() == null || !envelope.success() || envelope.data() == null) {
				log.warn("RENIEC (apiperu.dev) respondio success=false o sin data para dni={}", dni);
				return Optional.empty();
			}

			DatosApiperu d = envelope.data();
			String nombres = blankToNull(d.nombres());
			String apPat = blankToNull(d.apellidoPaterno());
			String apMat = blankToNull(d.apellidoMaterno());

			if (nombres == null && apPat == null && apMat == null) {
				log.warn("RENIEC (apiperu.dev) devolvio data sin nombres/apellidos para dni={}", dni);
				return Optional.empty();
			}
			return Optional.of(new DatosReniec(dni, nombres, apPat, apMat, d.codigoVerificacion()));
		} catch (Exception e) {
			log.warn("RENIEC (apiperu.dev) fallo para dni={}: {}", dni, e.toString());
			return Optional.empty();
		}
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	/** Body de la peticion a apiperu.dev. */
	private record SolicitudDni(@JsonProperty("dni") String dni) {}

	/** Envelope de apiperu.dev POST /api/dni. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RespuestaApiperu(
			@JsonProperty("success") Boolean success,
			@JsonProperty("data") DatosApiperu data) {}

	/** Bloque "data" de la respuesta de apiperu.dev. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DatosApiperu(
			@JsonProperty("numero") String numero,
			@JsonProperty("nombre_completo") String nombreCompleto,
			@JsonProperty("nombres") String nombres,
			@JsonProperty("apellido_paterno") String apellidoPaterno,
			@JsonProperty("apellido_materno") String apellidoMaterno,
			@JsonProperty("codigo_verificacion") Integer codigoVerificacion) {}
}
