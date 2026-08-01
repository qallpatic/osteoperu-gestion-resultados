package pe.com.qallpatic.osteoperu.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.proyeccion.ConteoPorFechaProjection;
import pe.com.qallpatic.osteoperu.compartido.proyeccion.PicoPorFechaProjection;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PicoRegistrosResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoSerieDiaria;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoArchivoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion del dashboard. Solo realiza lecturas agregadas sobre las
 * tablas USUARIO, RESULTADO_CARGA y RESULTADO_ARCHIVO.
 *
 * <p>Las series temporales se devuelven densas (un punto por dia) rellenando
 * con 0 los dias sin actividad, para que el front no tenga que conocer el
 * calendario del rango pedido.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

	/** Default si el front no envia rango: ultimos 30 dias incluido hoy. */
	private static final int DIAS_DEFAULT = 30;
	/** Limite del rango para evitar consultas demasiado pesadas. */
	private static final int DIAS_MAXIMO = 365;
	/** Estado de fila "vivo" (no soft deleted). Coincide con el resto del sistema. */
	private static final Integer ESTADO_VIVO = 1;
	/**
	 * TTL del cache en memoria. Corto a proposito: el ADMIN suele navegar entre
	 * rangos conocidos (7d/30d/90d/365d) y queremos hits instantaneos en clicks
	 * repetidos sin servir datos que ya no reflejen la realidad.
	 */
	private static final long CACHE_TTL_SEGUNDOS = 30;

	private final UsuarioRepository usuarioRepository;
	private final ResultadoCargaRepository resultadoCargaRepository;
	private final ResultadoArchivoRepository resultadoArchivoRepository;

	/** Cache en memoria por rango [(desde, hasta)]. ConcurrentHashMap para acceso seguro. */
	private final Map<CacheKeyDashboard, EntradaCacheDashboard> cache = new ConcurrentHashMap<>();

	@Override
	@Transactional(readOnly = true)
	public DashboardResumenResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
		LocalDate hoy = LocalDate.now();
		LocalDate desdeEf = (desde != null) ? desde : hoy.minusDays(DIAS_DEFAULT - 1L);
		LocalDate hastaEf = (hasta != null) ? hasta : hoy;
		if (hastaEf.isBefore(desdeEf)) {
			log.warn("Rango invertido en dashboard (desde={}, hasta={}). Se invierte.", desdeEf, hastaEf);
			LocalDate tmp = desdeEf;
			desdeEf = hastaEf;
			hastaEf = tmp;
		}
		if (ChronoUnit.DAYS.between(desdeEf, hastaEf) > DIAS_MAXIMO) {
			log.warn("Rango de dashboard excede {} dias (desde={}, hasta={}). Se recorta a hoy - {}.",
					DIAS_MAXIMO, desdeEf, hastaEf, DIAS_MAXIMO);
			desdeEf = hastaEf.minusDays(DIAS_MAXIMO - 1L);
		}

		LocalDateTime desdeDt = desdeEf.atStartOfDay();
		LocalDateTime hastaDt = hastaEf.atTime(LocalTime.MAX);

		// ===== Cache: hit si ya calculamos este rango y no expiro =====
		CacheKeyDashboard key = new CacheKeyDashboard(desdeEf, hastaEf);
		EntradaCacheDashboard cached = cache.get(key);
		if (cached != null && !cached.expiro()) {
			log.debug("Dashboard cache HIT. rango=[{}, {}]", desdeEf, hastaEf);
			return cached.valor();
		}

		// ===== KPIs globales =====
		long totalUsuarios = usuarioRepository.count();
		long totalResultados = resultadoCargaRepository.countByEstado(ESTADO_VIVO);
		long totalArchivos = resultadoArchivoRepository.countByEstado(ESTADO_VIVO);
		long totalVistas = resultadoCargaRepository.sumContadorVistasByEstado(ESTADO_VIVO);

		// ===== Pico historico de usuarios (top 1) =====
		PicoRegistrosResponse picoUsuarios = null;
		List<PicoPorFechaProjection> picos = usuarioRepository.picoRegistrosUsuarios(PageRequest.of(0, 1));
		if (!picos.isEmpty()) {
			PicoPorFechaProjection p = picos.get(0);
			picoUsuarios = new PicoRegistrosResponse(p.getFecha(), p.getTotal());
		}

		// ===== Series dentro del rango =====
		List<PuntoSerieDiaria> serieResultados = densificar(
				desdeEf, hastaEf,
				resultadoCargaRepository.conteoPorFechaCarga(desdeDt, hastaDt, ESTADO_VIVO));
		List<PuntoSerieDiaria> serieUsuarios = densificar(
				desdeEf, hastaEf,
				usuarioRepository.conteoPorFechaCreacion(desdeDt, hastaDt));

		log.debug("Dashboard generado. rango=[{}, {}], resultados={}, usuarios={}",
				desdeEf, hastaEf, totalResultados, totalUsuarios);

		DashboardResumenResponse respuesta = new DashboardResumenResponse(
				totalUsuarios, totalResultados, totalArchivos, totalVistas,
				picoUsuarios,
				desdeEf.toString(), hastaEf.toString(),
				serieResultados, serieUsuarios);

		cache.put(key, new EntradaCacheDashboard(respuesta,
				Instant.now().plusSeconds(CACHE_TTL_SEGUNDOS)));
		return respuesta;
	}

	/**
	 * Convierte una lista agrupada (puede tener "huecos" en dias sin movimiento)
	 * en una serie densa de un punto por dia. Garantiza eje X completo para el
	 * grafico del front.
	 */
	private List<PuntoSerieDiaria> densificar(LocalDate desde, LocalDate hasta,
			List<ConteoPorFechaProjection> agrupado) {
		Map<LocalDate, Long> mapa = new HashMap<>();
		for (ConteoPorFechaProjection p : agrupado) {
			if (p.getFecha() != null) {
				mapa.merge(p.getFecha(), p.getTotal(), Long::sum);
			}
		}
		List<PuntoSerieDiaria> salida = new ArrayList<>();
		for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
			salida.add(new PuntoSerieDiaria(dia, mapa.getOrDefault(dia, 0L)));
		}
		return salida;
	}
}