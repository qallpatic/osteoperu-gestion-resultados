package pe.com.qallpatic.osteoperu.integracion.reniec;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Implementacion mock (activa cuando {@code reniec.enabled=false} o esta ausente).
 * Incluye un DNI de demo para probar el flujo de validacion; el resto se
 * considera "no verificable" y la carga continua registrando el log.
 */
@Component
@ConditionalOnProperty(prefix = "reniec", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class ClienteReniecMock implements ClienteReniec {

	private static final Map<String, DatosReniec> DEMO = Map.of(
			"77776666", new DatosReniec("77776666", "RONEL", "GARAY", "PIMENTEL"));

	@PostConstruct
	void aviso() {
		log.warn("RENIEC en modo MOCK (reniec.enabled=false). Sin verificacion real de DNI. "
				+ "DNI de demo disponible: 77776666.");
	}

	@Override
	public Optional<DatosReniec> consultar(String dni) {
		return Optional.ofNullable(DEMO.get(dni));
	}
}
