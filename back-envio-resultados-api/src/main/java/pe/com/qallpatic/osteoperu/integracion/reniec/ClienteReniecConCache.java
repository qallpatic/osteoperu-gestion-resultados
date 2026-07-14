package pe.com.qallpatic.osteoperu.integracion.reniec;

import lombok.extern.slf4j.Slf4j;
import pe.com.qallpatic.osteoperu.integracion.reniec.entity.ReniecCache;
import pe.com.qallpatic.osteoperu.integracion.reniec.repository.ReniecCacheRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Wrapper de {@link ClienteReniec} que persiste la respuesta del proveedor en
 * la tabla {@code RENIEC_CACHE} para evitar cobrar la consulta cada vez.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Busca en cache por DNI. Si la fila existe, NO esta expirada y tiene
 *       los segmentos del nombre completos (no son null), la devuelve e
 *       incrementa el contador de hits.</li>
 *   <li>Si no existe, esta expirada, o tiene segmentos null (dato incompleto
 *       guardado por un lookup anterior fallido), consulta al proveedor.</li>
 *   <li>Si el proveedor responde con datos, persiste/actualiza la fila con
 *       TTL = hoy + cacheDias.</li>
 *   <li>Si el proveedor responde vacio, NO se persiste nada (la fila se
 *       elimina si existia) para no envenenar el cache con nulls.</li>
 *   <li>Si el proveedor falla con excepcion y existe una fila expirada con
 *       datos, la devuelve como fallback de resiliencia (sin cobrar).</li>
 * </ol>
 */
@Slf4j
public class ClienteReniecConCache implements ClienteReniec {

	private final ClienteReniec origen;
	private final ReniecCacheService cacheService;
	private final int cacheDias;

	public ClienteReniecConCache(ClienteReniec origen, ReniecCacheService cacheService, int cacheDias) {
		this.origen = origen;
		this.cacheService = cacheService;
		this.cacheDias = cacheDias;
	}

	@Override
	public Optional<DatosReniec> consultar(String dni) {
		Optional<ReniecCache> cached = cacheService.buscar(dni);

		// 1. Cache vigente y con datos completos: devolver sin consultar al proveedor
		if (cached.isPresent() && vigenteYCompleta(cached.get())) {
			ReniecCache c = cached.get();
			cacheService.incrementarConsultas(c);
			log.debug("RENIEC cache HIT dni={}", dni);
			return Optional.of(aDatos(c));
		}

		// 2. Cache miss / expirado / incompleto: consultar al proveedor
		Optional<DatosReniec> datos;
		try {
			datos = origen.consultar(dni);
		} catch (Exception ex) {
			// Fallo del proveedor: si tenemos cache expirada con datos, devolverla
			if (cached.isPresent()) {
				log.warn("RENIEC API fallo dni={}: {}; devolviendo cache previa (consultas={})",
						dni, ex.getMessage(), cached.get().getConsultas());
				return Optional.of(aDatos(cached.get()));
			}
			throw ex;
		}

		if (datos.isPresent()) {
			cacheService.guardar(dni, datos.get(), cacheDias);
			log.debug("RENIEC cache MISS dni={} (consultado al proveedor)", dni);
			return datos;
		}

		// 3. Proveedor respondio vacio: si habia una fila (expirada o con nulls), limpiarla
		//    para que el proximo intento vuelva a pegarle a la API.
		if (cached.isPresent()) {
			log.info("RENIEC API devolvio vacio para dni={}; limpiando fila de cache previa", dni);
			cacheService.eliminar(dni);
		}
		return Optional.empty();
	}

	/** Una fila es "util" cuando NO esta expirada y tiene al menos un nombre o apellido. */
	private boolean vigenteYCompleta(ReniecCache c) {
		if (c.getFechaExpira() == null || !c.getFechaExpira().isAfter(LocalDateTime.now())) {
			return false;
		}
		boolean tieneNombre = notBlank(c.getNombres());
		boolean tieneApellido = notBlank(c.getApellidoPaterno()) || notBlank(c.getApellidoMaterno());
		return tieneNombre || tieneApellido;
	}

	private static boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}

	private DatosReniec aDatos(ReniecCache c) {
		return new DatosReniec(c.getDni(), c.getNombres(), c.getApellidoPaterno(),
				c.getApellidoMaterno(), c.getCodigoVerificacion());
	}
}