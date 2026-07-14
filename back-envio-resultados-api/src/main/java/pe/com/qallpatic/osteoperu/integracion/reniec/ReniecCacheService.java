package pe.com.qallpatic.osteoperu.integracion.reniec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.integracion.reniec.entity.ReniecCache;
import pe.com.qallpatic.osteoperu.integracion.reniec.repository.ReniecCacheRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Encapsula las operaciones de lectura/escritura sobre {@code RENIEC_CACHE}.
 * Se mantiene como servicio separado para que cada operacion corra en su
 * propia transaccion (REQUIRES_NEW) y asi no quede atrapada dentro de la
 * transaccion {@code readOnly} del servicio que consulta RENIEC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReniecCacheService {

	private final ReniecCacheRepository repo;

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public Optional<ReniecCache> buscar(String dni) {
		return repo.findById(dni);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void guardar(String dni, DatosReniec d, int cacheDias) {
		LocalDateTime ahora = LocalDateTime.now();
		ReniecCache fila = repo.findById(dni).orElseGet(() -> ReniecCache.builder()
				.dni(dni)
				.build());
		fila.setNombres(d.nombres());
		fila.setApellidoPaterno(d.apellidoPaterno());
		fila.setApellidoMaterno(d.apellidoMaterno());
		fila.setNombreCompleto(d.nombreCompleto());
		fila.setCodigoVerificacion(d.codigoVerificacion());
		fila.setConsultas(fila.getConsultas() + 1);
		fila.setFechaConsulta(ahora);
		fila.setFechaExpira(ahora.plusDays(cacheDias));
		repo.save(fila);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementarConsultas(ReniecCache c) {
		c.setConsultas(c.getConsultas() + 1);
		repo.save(c);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void eliminar(String dni) {
		repo.deleteById(dni);
	}
}