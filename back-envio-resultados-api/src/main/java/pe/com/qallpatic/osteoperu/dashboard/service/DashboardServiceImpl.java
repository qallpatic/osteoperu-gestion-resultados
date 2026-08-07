package pe.com.qallpatic.osteoperu.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.qallpatic.osteoperu.compartido.proyeccion.ConteoPorFechaProjection;
import pe.com.qallpatic.osteoperu.compartido.proyeccion.PicoPorFechaProjection;
import pe.com.qallpatic.osteoperu.compartido.util.UserAgentParser;
import pe.com.qallpatic.osteoperu.dashboard.dto.AlmacenamientoDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.ArchivoPesadoResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.DistribucionTipoContenidoResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PicoRegistrosResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoAcumuladoMb;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoHorario;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoSerieDiaria;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoVistaDiaria;
import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteArchivoRow;
import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteEventoVistaRow;
import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteResultadoRow;
import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteUsuarioRow;
import pe.com.qallpatic.osteoperu.dashboard.dto.VistasDetalleResponse;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorDashboardExcel;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorReporteAlmacenamiento;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorReporteResultados;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorReporteUsuarios;
import pe.com.qallpatic.osteoperu.dashboard.util.ExportadorReporteVistas;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.TipoEvento;
import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;
import pe.com.qallpatic.osteoperu.resultados.repository.EventoVistaRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoArchivoRepository;
import pe.com.qallpatic.osteoperu.resultados.repository.ResultadoCargaRepository;
import pe.com.qallpatic.osteoperu.seguridad.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		// Engagement total: aperturas del visor (CONTADOR_VISTAS) + descargas
		// de archivos (eventos DESCARGA). Excluye CLICK porque ocurre antes
		// del contacto con los resultados.
		long totalVistas = resultadoCargaRepository.sumContadorVistasByEstado(ESTADO_VIVO)
				+ resultadoCargaRepository.countDescargasByEstado(ESTADO_VIVO);

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
	public byte[] generarExcelUsuarios(LocalDate desde, LocalDate hasta) {
		RangoNormalizado rango = normalizar(desde, hasta);
		LocalDateTime desdeDt = rango.desde().atStartOfDay();
		LocalDateTime hastaDt = rango.hasta().atTime(LocalTime.MAX);

		List<Usuario> usuarios = usuarioRepository.listarParaReporte(desdeDt, hastaDt);
		List<ReporteUsuarioRow> reporte = new ArrayList<>(usuarios.size());
		for (Usuario u : usuarios) {
			List<String> roles = u.getRoles().stream()
					.map(r -> r.getNombre())
					.sorted()
					.toList();
			reporte.add(new ReporteUsuarioRow(
					u.getFechaCreacion(),
					u.getUsuario(),
					u.getNombre(),
					u.getDni(),
					u.getCorreo(),
					roles,
					u.isEstado(),
					u.getFechaModificacion(),
					u.getUsuarioCreacion(),
					u.getUsuarioModificacion()));
		}

		log.info("Reporte usuarios exportado. rango=[{}, {}], filas={}",
				rango.desde(), rango.hasta(), reporte.size());
		return ExportadorReporteUsuarios.generar(reporte, rango.desde().toString(), rango.hasta().toString());
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] generarExcelAlmacenamiento(LocalDate desde, LocalDate hasta) {
		RangoNormalizado rango = normalizar(desde, hasta);
		LocalDateTime desdeDt = rango.desde().atStartOfDay();
		LocalDateTime hastaDt = rango.hasta().atTime(LocalTime.MAX);

		List<Object[]> filas = resultadoArchivoRepository.listarParaReporte(desdeDt, hastaDt);
		List<ReporteArchivoRow> reporte = new ArrayList<>(filas.size());
		for (Object[] f : filas) {
			reporte.add(new ReporteArchivoRow(
					(LocalDateTime) f[0],
					(String) f[1],
					(String) f[2],
					(String) f[3],
					(String) f[4],
					(BigDecimal) f[5],
					((Number) f[6]).intValue(),
					(LocalDateTime) f[7],
					(String) f[8],
					(String) f[9],
					(String) f[10]));
		}

		log.info("Reporte almacenamiento exportado. rango=[{}, {}], filas={}",
				rango.desde(), rango.hasta(), reporte.size());
		return ExportadorReporteAlmacenamiento.generar(reporte, rango.desde().toString(), rango.hasta().toString());
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] generarExcelVistas(LocalDate desde, LocalDate hasta) {
		RangoNormalizado rango = normalizar(desde, hasta);
		LocalDateTime desdeDt = rango.desde().atStartOfDay();
		LocalDateTime hastaDt = rango.hasta().atTime(LocalTime.MAX);

		// Reutilizamos el calculo del detalle (cacheado en memoria) para que
		// la hoja Resumen del Excel muestre los mismos KPIs accionables que
		// el front.
		VistasDetalleResponse detalle = obtenerVistasDetalle(rango.desde(), rango.hasta());

		List<Object[]> filas = eventoVistaRepository.listarParaReporte(desdeDt, hastaDt);

		// EventoVista guarda idResultado como Long escalar (sin @ManyToOne),
		// asi que resolvemos el join a ResultadoCarga en Java con un solo
		// findAllById sobre los IDs unicos del rango. Dataset chico (decenas
		// o cientos), no amerita un query N+1 ni un join declarativo.
		Set<Long> idsUnicos = new HashSet<>();
		for (Object[] f : filas) {
			if (f[2] != null) {
				idsUnicos.add(((Number) f[2]).longValue());
			}
		}
		Map<Long, ResultadoCarga> cabeceras = new HashMap<>();
		if (!idsUnicos.isEmpty()) {
			for (ResultadoCarga c : resultadoCargaRepository.findAllById(idsUnicos)) {
				cabeceras.put(c.getIdResultadoCarga(), c);
			}
		}

		List<ReporteEventoVistaRow> reporte = new ArrayList<>(filas.size());
		for (Object[] f : filas) {
			Long idRes = f[2] == null ? null : ((Number) f[2]).longValue();
			ResultadoCarga c = idRes == null ? null : cabeceras.get(idRes);
			reporte.add(new ReporteEventoVistaRow(
					(LocalDateTime) f[0],
					f[1] == null ? null : f[1].toString(),
					idRes,
					c == null ? null : c.getDocumentoIdentidad(),
					c == null ? null : c.getNombrePaciente(),
					(String) f[3],
					UserAgentParser.resumir((String) f[4])));
		}

		log.info("Reporte vistas exportado. rango=[{}, {}], eventos={}, cabecerasDistintas={}, "
				+ "aperturas={}, descargas={}, ratio={}%, horaPico={} ({})",
				rango.desde(), rango.hasta(), reporte.size(), cabeceras.size(),
				detalle.totalAperturas(), detalle.totalDescargas(),
				detalle.ratioDescargaPorApertura(),
				detalle.horaPico(), detalle.totalHoraPico());
		return ExportadorReporteVistas.generar(detalle, reporte);
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

		// ===== KPIs accionables: aperturas, descargas, ratio =====
		long totalAperturas = 0L;
		long totalDescargas = 0L;
		for (Object[] fila : eventoVistaRepository.conteoPorTipoEnRango(desdeDt, hastaDt)) {
			TipoEvento tipo = (TipoEvento) fila[0];
			long n = ((Number) fila[1]).longValue();
			if (tipo == TipoEvento.VISTA) {
				totalAperturas = n;
			} else if (tipo == TipoEvento.DESCARGA) {
				totalDescargas = n;
			}
		}
		BigDecimal ratioDescargaPorApertura = totalAperturas > 0
				? BigDecimal.valueOf(totalDescargas)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(totalAperturas), 1, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		// ===== Curva temporal densa (total + aperturas + descargas) =====
		// Reutilizamos las dos queries separadas (aperturas y descargas) y
		// sumamos en Java para el total. Asi tenemos el desglose por serie
		// sin multiplicar queries.
		Map<LocalDate, Long> mapaAperturas = new HashMap<>();
		for (Object[] fila : eventoVistaRepository.conteoVistasPorDia(desdeDt, hastaDt)) {
			mapaAperturas.put(((java.sql.Date) fila[0]).toLocalDate(),
					((Number) fila[1]).longValue());
		}
		Map<LocalDate, Long> mapaDescargas = new HashMap<>();
		for (Object[] fila : eventoVistaRepository.conteoDescargasPorDia(desdeDt, hastaDt)) {
			mapaDescargas.put(((java.sql.Date) fila[0]).toLocalDate(),
					((Number) fila[1]).longValue());
		}
		List<PuntoVistaDiaria> curva = new ArrayList<>();
		List<PuntoVistaDiaria> curvaAperturas = new ArrayList<>();
		List<PuntoVistaDiaria> curvaDescargas = new ArrayList<>();
		for (LocalDate dia = rango.desde(); !dia.isAfter(rango.hasta()); dia = dia.plusDays(1)) {
			long apertura = mapaAperturas.getOrDefault(dia, 0L);
			long descarga = mapaDescargas.getOrDefault(dia, 0L);
			curva.add(new PuntoVistaDiaria(dia.toString(), apertura + descarga));
			curvaAperturas.add(new PuntoVistaDiaria(dia.toString(), apertura));
			curvaDescargas.add(new PuntoVistaDiaria(dia.toString(), descarga));
		}

		// ===== Distribucion horaria (24 puntos densos) =====
		Map<Integer, Long> mapaPorHora = new HashMap<>();
		for (Object[] fila : eventoVistaRepository.conteoPorHoraEnRango(desdeDt, hastaDt)) {
			mapaPorHora.put(((Number) fila[0]).intValue(),
					((Number) fila[1]).longValue());
		}
		List<PuntoHorario> distribucionHoraria = new ArrayList<>(24);
		int horaPico = -1;
		long totalHoraPico = 0L;
		for (int h = 0; h < 24; h++) {
			long n = mapaPorHora.getOrDefault(h, 0L);
			distribucionHoraria.add(new PuntoHorario(h, n));
			if (n > totalHoraPico) {
				totalHoraPico = n;
				horaPico = h;
			}
		}
		Integer horaPicoNullable = totalHoraPico > 0 ? horaPico : null;
		Long totalHoraPicoNullable = totalHoraPico > 0 ? totalHoraPico : null;

		log.debug("Vistas detalle generado. rango=[{}, {}], curvaDias={}, "
				+ "aperturas={}, descargas={}, ratio={}%, horaPico={} ({})",
				rango.desde(), rango.hasta(), curva.size(),
				totalAperturas, totalDescargas, ratioDescargaPorApertura,
				horaPicoNullable, totalHoraPicoNullable);

		VistasDetalleResponse respuesta = new VistasDetalleResponse(
				rango.desde().toString(), rango.hasta().toString(),
				curva,
				curvaAperturas, curvaDescargas,
				totalAperturas, totalDescargas, ratioDescargaPorApertura,
				distribucionHoraria, horaPicoNullable, totalHoraPicoNullable);

		cacheVistas.put(key, new EntradaCacheDashboard<>(respuesta,
				Instant.now().plusSeconds(CACHE_TTL_SEGUNDOS)));
		return respuesta;
	}

	/** Tupla inmutable (desde, hasta) efectiva devuelta por {@link #normalizar}. */
	private record RangoNormalizado(LocalDate desde, LocalDate hasta) {}

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
}
