package pe.com.qallpatic.osteoperu.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.proyeccion.ConteoPorFechaProjection;
import pe.com.qallpatic.osteoperu.compartido.proyeccion.PicoPorFechaProjection;
import pe.com.qallpatic.osteoperu.dashboard.dto.AlmacenamientoDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.ArchivoPesadoResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.BucketVistasResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DistribucionTipoContenidoResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PicoRegistrosResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoAcumuladoMb;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoSerieDiaria;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoVistaDiaria;
import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteResultadoRow;
import pe.com.qallpatic.osteoperu.dashboard.dto.ResultadoMasVistoResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.VistasDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorDashboardExcel;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorReporteResultados;
import pe.com.qallpatic.osteoperu.resultados.repository.EventoVistaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoArchivoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion del dashboard. Solo realiza lecturas agregadas sobre las
 * tablas USUARIO, RESULTADO_CARGA, RESULTADO_ARCHIVO y EVENTO_VISTA.
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
	/** Top N para listas de "mas vistos" / "mas pesados". */
	private static final int TOP_N = 10;
	/** Buckets canonicos para distribuir las vistas (orden estable). */
	private static final List<String> BUCKET_ETIQUETAS = List.of("1", "2-5", "6-10", "11+");

	private final UsuarioRepository usuarioRepository;
	private final ResultadoCargaRepository resultadoCargaRepository;
	private final ResultadoArchivoRepository resultadoArchivoRepository;
	private final EventoVistaRepository eventoVistaRepository;

	/** Cache en memoria por rango [(desde, hasta)]. ConcurrentHashMap para acceso seguro. */
	private final Map<CacheKeyDashboard, EntradaCacheDashboard<DashboardResumenResponse>> cache = new ConcurrentHashMap<>();
	/** Cache especifico para los detalles de almacenamiento (mismo TTL que resumen). */
	private final Map<CacheKeyDashboard, EntradaCacheDashboard<AlmacenamientoDetalleResponse>> cacheAlmacenamiento = new ConcurrentHashMap<>();
	/** Cache especifico para los detalles de vistas. */
	private final Map<CacheKeyDashboard, EntradaCacheDashboard<VistasDetalleResponse>> cacheVistas = new ConcurrentHashMap<>();

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
		EntradaCacheDashboard<DashboardResumenResponse> cached = cache.get(key);
		if (cached != null && !cached.expiro()) {
			log.debug("Dashboard cache HIT. rango=[{}, {}]", desdeEf, hastaEf);
			return cached.valor();
		}

		// ===== KPIs globales =====
		long totalUsuarios = usuarioRepository.count();
		long totalResultados = resultadoCargaRepository.countByEstado(ESTADO_VIVO);
		// Archivos y peso: TODOS los registros (sin filtrar estado). En OSTEOPERU
		// los archivos no se borran fisicamente — solo soft-delete, asi que el
		// espacio en el bucket corresponde al total de filas en la tabla.
		long totalArchivos = resultadoArchivoRepository.count();
		BigDecimal pesoTotalMb = resultadoArchivoRepository.sumPesoMb();
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
				totalUsuarios, totalResultados, totalArchivos, pesoTotalMb,
				totalVistas,
				picoUsuarios,
				desdeEf.toString(), hastaEf.toString(),
				serieResultados, serieUsuarios);

		cache.put(key, new EntradaCacheDashboard(respuesta,
				Instant.now().plusSeconds(CACHE_TTL_SEGUNDOS)));
		return respuesta;
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] generarExcel(LocalDate desde, LocalDate hasta) {
		// Reutilizamos obtenerResumen para beneficiarnos del cache en memoria.
		// La normalizacion del rango (defaults / inversion / cap a 365 dias)
		// ocurre una sola vez adentro, asi que el Excel sale consistente
		// con lo que el ADMIN ve en pantalla.
		DashboardResumenResponse resumen = obtenerResumen(desde, hasta);
		return ExportadorDashboardExcel.generar(resumen);
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] generarExcelResultados(LocalDate desde, LocalDate hasta) {
		// Reporte operativo: SIN cache (lista potencialmente grande + queremos
		// datos frescos siempre que el ADMIN descargue). La normalizacion del
		// rango se hace localmente para devolver al frontend el rango efectivo
		// que efectivamente usamos.
		RangoNormalizado rango = normalizar(desde, hasta);
		LocalDateTime desdeDt = rango.desde().atStartOfDay();
		LocalDateTime hastaDt = rango.hasta().atTime(LocalTime.MAX);

		List<Object[]> filas = resultadoCargaRepository.listarParaReporte(desdeDt, hastaDt);
		List<ReporteResultadoRow> reporte = new ArrayList<>(filas.size());
		for (Object[] f : filas) {
			reporte.add(new ReporteResultadoRow(
					(LocalDateTime) f[0],
					(String) f[1],
					(String) f[2],
					(String) f[3],
					((Number) f[4]).intValue(),
					(BigDecimal) f[5],
					f[6] == null ? null : f[6].toString(),
					((Number) f[7]).intValue(),
					(LocalDateTime) f[8],
					(String) f[9],
					(String) f[10],
					(String) f[11]));
		}

		log.info("Reporte resultados exportado. rango=[{}, {}], filas={}",
				rango.desde(), rango.hasta(), reporte.size());
		return ExportadorReporteResultados.generar(reporte, rango.desde().toString(), rango.hasta().toString());
	}

	@Override
	@Transactional(readOnly = true)
	public AlmacenamientoDetalleResponse obtenerAlmacenamientoDetalle(LocalDate desde, LocalDate hasta) {
		RangoNormalizado rango = normalizar(desde, hasta);
		CacheKeyDashboard key = new CacheKeyDashboard(rango.desde(), rango.hasta());

		EntradaCacheDashboard<AlmacenamientoDetalleResponse> cached = cacheAlmacenamiento.get(key);
		if (cached != null && !cached.expiro()) {
			log.debug("Almacenamiento detalle cache HIT. rango=[{}, {}]", rango.desde(), rango.hasta());
			return cached.valor();
		}

		// ===== Distribucion por tipoContenido (torta) =====
		List<DistribucionTipoContenidoResponse> distribucion = new ArrayList<>();
		for (Object[] fila : resultadoArchivoRepository.distribucionPorTipoContenido()) {
			String tipoContenido = (String) fila[0];
			Long cantidad = ((Number) fila[1]).longValue();
			BigDecimal pesoMb = (BigDecimal) fila[2];
			distribucion.add(new DistribucionTipoContenidoResponse(tipoContenido, cantidad, pesoMb));
		}

		// ===== Top N archivos mas pesados (tabla) =====
		List<ArchivoPesadoResponse> top = new ArrayList<>();
		List<Object[]> filasTop = resultadoArchivoRepository.topArchivosMasPesados(PageRequest.of(0, TOP_N));
		for (Object[] fila : filasTop) {
			top.add(new ArchivoPesadoResponse(
					(String) fila[0],
					(String) fila[1],
					(String) fila[2],
					(BigDecimal) fila[3]));
		}

		// ===== Curva acumulada de MB (linea) =====
		// Sin filtrar soft-delete. Sumamos incrementalmente en Java para
		// evitar window function nativo (dataset chico, ~178 filas).
		List<PuntoAcumuladoMb> acumulado = new ArrayList<>();
		List<Object[]> filasPorDia = resultadoArchivoRepository.mbSubidosPorDia();
		BigDecimal runningTotal = BigDecimal.ZERO;
		for (Object[] fila : filasPorDia) {
			java.sql.Date diaSql = (java.sql.Date) fila[0];
			BigDecimal mbDia = (BigDecimal) fila[1];
			runningTotal = runningTotal.add(mbDia);
			acumulado.add(new PuntoAcumuladoMb(diaSql.toString(), runningTotal));
		}

		log.debug("Almacenamiento detalle generado. rango=[{}, {}], tipos={}, top10={}, acumuladoDias={}",
				rango.desde(), rango.hasta(), distribucion.size(), top.size(), acumulado.size());

		AlmacenamientoDetalleResponse respuesta = new AlmacenamientoDetalleResponse(
				rango.desde().toString(), rango.hasta().toString(),
				distribucion, top, acumulado);

		cacheAlmacenamiento.put(key, new EntradaCacheDashboard<>(respuesta,
				Instant.now().plusSeconds(CACHE_TTL_SEGUNDOS)));
		return respuesta;
	}

	@Override
	@Transactional(readOnly = true)
	public VistasDetalleResponse obtenerVistasDetalle(LocalDate desde, LocalDate hasta) {
		RangoNormalizado rango = normalizar(desde, hasta);
		CacheKeyDashboard key = new CacheKeyDashboard(rango.desde(), rango.hasta());

		EntradaCacheDashboard<VistasDetalleResponse> cached = cacheVistas.get(key);
		if (cached != null && !cached.expiro()) {
			log.debug("Vistas detalle cache HIT. rango=[{}, {}]", rango.desde(), rango.hasta());
			return cached.valor();
		}

		LocalDateTime desdeDt = rango.desde().atStartOfDay();
		LocalDateTime hastaDt = rango.hasta().atTime(LocalTime.MAX);

		// ===== Top N cabeceras mas vistas (tabla) =====
		List<ResultadoMasVistoResponse> top = new ArrayList<>();
		List<Object[]> filasTop = resultadoCargaRepository.topCabecerasMasVistas(PageRequest.of(0, TOP_N));
		for (Object[] fila : filasTop) {
			top.add(new ResultadoMasVistoResponse(
					(String) fila[0],
					(String) fila[1],
					(String) fila[2],
					((Number) fila[3]).longValue(),
					(LocalDateTime) fila[4]));
		}

		// ===== Distribucion por buckets (1, 2-5, 6-10, 11+) =====
		List<BucketVistasResponse> buckets = calcularBuckets(
				resultadoCargaRepository.contadoresVistasCabecerasVivas());

		// ===== Curva temporal de aperturas (EVENTO_VISTA) =====
		Map<LocalDate, Long> mapaVistas = new HashMap<>();
		for (Object[] fila : eventoVistaRepository.conteoVistasPorDia(desdeDt, hastaDt)) {
			java.sql.Date diaSql = (java.sql.Date) fila[0];
			Long total = ((Number) fila[1]).longValue();
			mapaVistas.put(diaSql.toLocalDate(), total);
		}
		List<PuntoVistaDiaria> curva = new ArrayList<>();
		for (LocalDate dia = rango.desde(); !dia.isAfter(rango.hasta()); dia = dia.plusDays(1)) {
			curva.add(new PuntoVistaDiaria(dia.toString(),
					mapaVistas.getOrDefault(dia, 0L)));
		}

		log.debug("Vistas detalle generado. rango=[{}, {}], top={}, buckets={}, curvaDias={}",
				rango.desde(), rango.hasta(), top.size(), buckets.size(), curva.size());

		VistasDetalleResponse respuesta = new VistasDetalleResponse(
				rango.desde().toString(), rango.hasta().toString(),
				top, buckets, curva);

		cacheVistas.put(key, new EntradaCacheDashboard<>(respuesta,
				Instant.now().plusSeconds(CACHE_TTL_SEGUNDOS)));
		return respuesta;
	}

	/**
	 * Distribuye los contadores de vistas en buckets canonicos. Mantiene el
	 * orden estable (1, 2-5, 6-10, 11+) aunque haya ceros intermedios.
	 */
	private List<BucketVistasResponse> calcularBuckets(List<Integer> contadores) {
		Map<String, Long> acum = new LinkedHashMap<>();
		for (String et : BUCKET_ETIQUETAS) {
			acum.put(et, 0L);
		}
		for (Integer v : contadores) {
			if (v == null || v <= 0) {
				continue;
			}
			String et;
			if (v == 1) {
				et = "1";
			} else if (v <= 5) {
				et = "2-5";
			} else if (v <= 10) {
				et = "6-10";
			} else {
				et = "11+";
			}
			acum.merge(et, 1L, Long::sum);
		}
		List<BucketVistasResponse> salida = new ArrayList<>(acum.size());
		for (Map.Entry<String, Long> e : acum.entrySet()) {
			salida.add(new BucketVistasResponse(e.getKey(), e.getValue()));
		}
		return salida;
	}

	/**
	 * Normaliza un rango de fechas con defaults consistentes entre endpoints:
	 * null -> ultimos 30 dias, invertido -> se invierte, >365 -> se recorta.
	 */
	private RangoNormalizado normalizar(LocalDate desde, LocalDate hasta) {
		LocalDate hoy = LocalDate.now();
		LocalDate desdeEf = (desde != null) ? desde : hoy.minusDays(DIAS_DEFAULT - 1L);
		LocalDate hastaEf = (hasta != null) ? hasta : hoy;
		if (hastaEf.isBefore(desdeEf)) {
			LocalDate tmp = desdeEf;
			desdeEf = hastaEf;
			hastaEf = tmp;
		}
		if (ChronoUnit.DAYS.between(desdeEf, hastaEf) > DIAS_MAXIMO) {
			desdeEf = hastaEf.minusDays(DIAS_MAXIMO - 1L);
		}
		return new RangoNormalizado(desdeEf, hastaEf);
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

	/** Tupla inmutable (desde, hasta) efectiva devuelta por {@link #normalizar}. */
	private record RangoNormalizado(LocalDate desde, LocalDate hasta) {}
}
