package pe.com.qallpatic.osteoperu.integracion.reniec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Compone el cliente RENIEC real con la capa de cache local
 * ({@code RENIEC_CACHE}). Expone el bean envuelto como {@code @Primary} para
 * que el resto de la aplicacion lo consuma sin saber si hubo cache o no.
 */
@Configuration
public class ClienteReniecConfig {

	@Bean
	@org.springframework.context.annotation.Primary
	public ClienteReniec clienteReniec(
			@Qualifier("clienteReniecHttp") @Nullable ClienteReniec http,
			@Qualifier("clienteReniecMock") @Nullable ClienteReniec mock,
			ReniecCacheService cacheService,
			ReniecProperties props) {

		ClienteReniec origen = (http != null) ? http : mock;
		if (origen == null) {
			throw new IllegalStateException(
					"No hay implementacion activa de ClienteReniec "
							+ "(revisa la propiedad reniec.enabled)");
		}
		return new ClienteReniecConCache(origen, cacheService, props.getCacheDias());
	}
}