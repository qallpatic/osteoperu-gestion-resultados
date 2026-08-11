package pe.com.qallpatic.osteoperu.dashboard.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import pe.com.qallpatic.osteoperu.dashboard.dto.DashboardResumenResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PicoRegistrosResponse;
import pe.com.qallpatic.osteoperu.dashboard.dto.PuntoSerieDiaria;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Genera el archivo .xlsx del dashboard a partir de un
 * {@link DashboardResumenResponse}. Mantiene la logica de export fuera del
 * service para que este siga siendo un orquestador de queries.
 *
 * <p>Estructura del workbook:
 * <ol>
 *   <li><b>KPIs</b>: metricas globales + rango efectivo + pico historico.</li>
 *   <li><b>Resultados por dia</b>: serie densa de cargas dentro del rango.</li>
 *   <li><b>Usuarios por dia</b>: serie densa de registros dentro del rango.</li>
 * </ol>
 */
public final class ExportadorDashboardExcel {

	private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private ExportadorDashboardExcel() {
	}

	/**
	 * Construye el workbook en memoria y lo serializa a bytes. El caller se
	 * encarga de devolverlo como descarga HTTP.
	 */
	public static byte[] generar(DashboardResumenResponse r) {
		try (XSSFWorkbook wb = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Estilos estilos = new Estilos(wb);
			llenarHojaKpis(wb, estilos, r);
			llenarHojaSerie(wb, estilos, "Resultados por dia",
					"Resultados cargados dentro del rango", r.serieResultadosPorDia());
			llenarHojaSerie(wb, estilos, "Usuarios por dia",
					"Usuarios registrados dentro del rango", r.serieUsuariosPorDia());
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo serializar el Excel del dashboard", e);
		}
	}

	private static void llenarHojaKpis(Workbook wb, Estilos e, DashboardResumenResponse r) {
		Sheet sh = wb.createSheet("KPIs");
		sh.setColumnWidth(0, 9000);
		sh.setColumnWidth(1, 14_000);

		int fila = 0;
		titulo(sh, e, "Resumen del dashboard", fila++);
		fila++;

		fila = par(sh, e, fila, "Total usuarios", formatearNumero(r.totalUsuarios()));
		fila = par(sh, e, fila, "Total resultados cargados", formatearNumero(r.totalResultados()));
		fila = par(sh, e, fila, "Total archivos registrados", formatearNumero(r.totalArchivos()));
		fila = par(sh, e, fila, "Peso total acumulado", formatearPesoMb(r.pesoTotalMb()));
		fila = par(sh, e, fila, "Interacciones acumuladas (aperturas + descargas, historico)",
				formatearNumero(r.totalVistas()));
		fila++;

		PicoRegistrosResponse pico = r.picoUsuarios();
		String picoTxt = (pico == null)
				? "Sin registros suficientes para calcular un pico"
				: formatearNumero(pico.total()) + " el " + pico.fecha().format(FMT_FECHA);
		fila = par(sh, e, fila, "Pico historico de usuarios", picoTxt);
		fila++;

		fila = par(sh, e, fila, "Rango aplicado",
				"Desde " + r.desde() + " hasta " + r.hasta());
	}

	private static void llenarHojaSerie(Workbook wb, Estilos e, String tituloHoja,
			String subtitulo, java.util.List<PuntoSerieDiaria> puntos) {
		Sheet sh = wb.createSheet(tituloHoja);
		sh.setColumnWidth(0, 14_000);
		sh.setColumnWidth(1, 14_000);

		int fila = 0;
		titulo(sh, e, tituloHoja, fila++);
		celda(sh, e.texto, fila, 0, subtitulo).getCellStyle();
		sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, 1));
		fila++;
		fila++;

		encabezado(sh, e, fila, "Fecha", "Total");
		fila++;

		for (PuntoSerieDiaria p : puntos) {
			Row row = sh.createRow(fila++);
			celda(row, e.fecha, 0, p.fecha() == null ? "" : p.fecha().format(FMT_FECHA));
			celda(row, e.numero, 1, formatearNumero(p.total()));
		}
	}

	// ===== helpers =====

	private static void titulo(Sheet sh, Estilos e, String texto, int fila) {
		Row row = sh.createRow(fila);
		row.setHeightInPoints(20);
		Cell c = row.createCell(0);
		c.setCellValue(texto);
		c.setCellStyle(e.titulo);
		sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, 1));
	}

	private static int par(Sheet sh, Estilos e, int fila, String label, String valor) {
		Row row = sh.createRow(fila);
		celda(row, e.etiqueta, 0, label);
		celda(row, e.texto, 1, valor);
		return fila + 1;
	}

	private static void encabezado(Sheet sh, Estilos e, int fila, String... textos) {
		Row row = sh.createRow(fila);
		for (int i = 0; i < textos.length; i++) {
			celda(row, e.encabezado, i, textos[i]);
		}
	}

	private static Cell celda(Sheet sh, CellStyle estilo, int fila, int col, String valor) {
		return celda(sh.createRow(fila), estilo, col, valor);
	}

	private static Cell celda(Row row, CellStyle estilo, int col, String valor) {
		Cell c = row.createCell(col);
		c.setCellValue(valor);
		c.setCellStyle(estilo);
		return c;
	}

	private static String formatearNumero(long n) {
		return String.format("%,d", n);
	}

	private static String formatearPesoMb(BigDecimal mb) {
		if (mb == null) {
			return "0.00 MB";
		}
		BigDecimal v = mb.setScale(2, RoundingMode.HALF_UP);
		if (v.compareTo(new BigDecimal("1024")) >= 0) {
			return v.divide(new BigDecimal("1024"), 2, RoundingMode.HALF_UP) + " GB";
		}
		return v + " MB";
	}

	/**
	 * Conjunto de estilos reutilizados por todas las hojas. Se construyen una
	 * sola vez por workbook para no recrear fuentes/estilos celda por celda.
	 */
	private static final class Estilos {
		final CellStyle titulo;
		final CellStyle encabezado;
		final CellStyle etiqueta;
		final CellStyle texto;
		final CellStyle numero;
		final CellStyle fecha;

		Estilos(Workbook wb) {
			Font fTitulo = wb.createFont();
			fTitulo.setBold(true);
			fTitulo.setFontHeightInPoints((short) 14);
			fTitulo.setColor(IndexedColors.WHITE.getIndex());

			Font fEnc = wb.createFont();
			fEnc.setBold(true);
			fEnc.setColor(IndexedColors.WHITE.getIndex());

			Font fLabel = wb.createFont();
			fLabel.setBold(true);
			fLabel.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

			titulo = wb.createCellStyle();
			titulo.setFont(fTitulo);
			titulo.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
			titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			titulo.setAlignment(HorizontalAlignment.LEFT);

			encabezado = wb.createCellStyle();
			encabezado.setFont(fEnc);
			encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			encabezado.setAlignment(HorizontalAlignment.LEFT);
			conBordes(encabezado);

			etiqueta = wb.createCellStyle();
			etiqueta.setFont(fLabel);
			etiqueta.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			etiqueta.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			etiqueta.setAlignment(HorizontalAlignment.LEFT);
			conBordes(etiqueta);

			texto = wb.createCellStyle();
			texto.setAlignment(HorizontalAlignment.LEFT);
			conBordes(texto);

			numero = wb.createCellStyle();
			numero.setAlignment(HorizontalAlignment.RIGHT);
			numero.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
			conBordes(numero);

			fecha = wb.createCellStyle();
			fecha.setAlignment(HorizontalAlignment.CENTER);
			conBordes(fecha);
		}

		private static void conBordes(CellStyle s) {
			s.setBorderBottom(BorderStyle.THIN);
			s.setBorderTop(BorderStyle.THIN);
			s.setBorderLeft(BorderStyle.THIN);
			s.setBorderRight(BorderStyle.THIN);
		}
	}
}
