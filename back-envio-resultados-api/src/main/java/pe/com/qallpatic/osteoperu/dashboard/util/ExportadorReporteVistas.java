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

import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteEventoVistaRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el .xlsx del reporte "Vistas de pacientes" a partir de una lista
 * plana de {@link ReporteEventoVistaRow}. Una fila por evento (CLICK,
 * VISTA o DESCARGA) con metadatos de la peticion (IP, user agent).
 *
 * <p>Estructura del workbook:
 * <ol>
 *   <li><b>Resumen</b>: rango aplicado, total de eventos, distribucion por
 *       tipo de evento (CLICK / VISTA / DESCARGA).</li>
 *   <li><b>Eventos</b>: lista plana con 8 columnas.</li>
 * </ol>
 */
public final class ExportadorReporteVistas {

	private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private static final String[] HEADERS = {
			"Fecha", "Tipo evento", "Codigo corto", "ID Resultado",
			"DNI/CE", "Paciente", "IP", "User agent"
	};

	private static final int[] WIDTHS = {
			5_000, 3_500, 5_000, 5_500,
			4_000, 7_000, 4_000, 9_000
	};

	private ExportadorReporteVistas() {
	}

	public static byte[] generar(List<ReporteEventoVistaRow> filas, String desde, String hasta) {
		try (XSSFWorkbook wb = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			EstilosReporte estilos = new EstilosReporte(wb);
			llenarHojaResumen(wb, estilos, filas, desde, hasta);
			llenarHojaEventos(wb, estilos, filas);
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo serializar el Excel de vistas", e);
		}
	}

	private static void llenarHojaResumen(Workbook wb, EstilosReporte e,
			List<ReporteEventoVistaRow> filas, String desde, String hasta) {
		Sheet sh = wb.createSheet("Resumen");
		sh.setColumnWidth(0, 9_000);
		sh.setColumnWidth(1, 14_000);

		Map<String, Long> porTipo = new HashMap<>();
		for (String tipo : List.of("CLICK", "VISTA", "DESCARGA")) {
			porTipo.put(tipo, 0L);
		}
		for (ReporteEventoVistaRow r : filas) {
			String tipo = r.tipoEvento();
			if (tipo != null) {
				porTipo.merge(tipo, 1L, Long::sum);
			}
		}

		int fila = 0;
		titulo(sh, e, "Reporte de vistas de pacientes", fila++);
		fila++;
		fila = par(sh, e, fila, "Rango aplicado", "Desde " + desde + " hasta " + hasta);
		fila = par(sh, e, fila, "Total de eventos", String.format("%,d", filas.size()));
		fila = par(sh, e, fila, "Clicks (link corto)", String.format("%,d", porTipo.getOrDefault("CLICK", 0L)));
		fila = par(sh, e, fila, "Vistas del visor", String.format("%,d", porTipo.getOrDefault("VISTA", 0L)));
		fila = par(sh, e, fila, "Descargas de archivos",
				String.format("%,d", porTipo.getOrDefault("DESCARGA", 0L)));
		fila++;
		fila = par(sh, e, fila, "Detalle",
				"Lista plana de eventos en la hoja 'Eventos'.");
	}

	private static void llenarHojaEventos(Workbook wb, EstilosReporte e,
			List<ReporteEventoVistaRow> filas) {
		Sheet sh = wb.createSheet("Eventos");
		for (int i = 0; i < WIDTHS.length; i++) {
			sh.setColumnWidth(i, WIDTHS[i]);
		}

		int fila = 0;
		Row header = sh.createRow(fila++);
		header.setHeightInPoints(20);
		for (int i = 0; i < HEADERS.length; i++) {
			Cell c = header.createCell(i);
			c.setCellValue(HEADERS[i]);
			c.setCellStyle(e.encabezado);
		}

		for (ReporteEventoVistaRow r : filas) {
			Row row = sh.createRow(fila++);
			celdaFechaHora(row, e.fechaHora, 0, r.fechaCreacion());
			celdaTexto(row, e.texto, 1, r.tipoEvento());
			celdaTexto(row, e.texto, 2, r.codigoCorto());
			celdaNumero(row, e.numero, 3, r.idResultado());
			celdaTexto(row, e.texto, 4, r.documentoIdentidad());
			celdaTexto(row, e.texto, 5, r.nombrePaciente());
			celdaTexto(row, e.texto, 6, r.ip());
			celdaTexto(row, e.texto, 7, r.userAgent());
		}
	}

	// ===== helpers =====

	private static void titulo(Sheet sh, EstilosReporte e, String texto, int fila) {
		Row row = sh.createRow(fila);
		row.setHeightInPoints(20);
		Cell c = row.createCell(0);
		c.setCellValue(texto);
		c.setCellStyle(e.titulo);
		sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, 1));
	}

	private static int par(Sheet sh, EstilosReporte e, int fila, String label, String valor) {
		Row row = sh.createRow(fila);
		celda(row, e.etiqueta, 0, label);
		celda(row, e.texto, 1, valor);
		return fila + 1;
	}

	private static void celdaTexto(Row row, CellStyle estilo, int col, String valor) {
		celda(row, estilo, col, valor == null ? "" : valor);
	}

	private static void celdaNumero(Row row, CellStyle estilo, int col, Long valor) {
		Cell c = row.createCell(col);
		if (valor != null) {
			c.setCellValue(valor.doubleValue());
		}
		c.setCellStyle(estilo);
	}

	private static void celdaFechaHora(Row row, CellStyle estilo, int col,
			java.time.LocalDateTime valor) {
		Cell c = row.createCell(col);
		if (valor != null) {
			c.setCellValue(valor.format(FMT_FECHA_HORA));
		}
		c.setCellStyle(estilo);
	}

	private static void celda(Row row, CellStyle estilo, int col, String valor) {
		Cell c = row.createCell(col);
		c.setCellValue(valor);
		c.setCellStyle(estilo);
	}

	private static final class EstilosReporte {
		final CellStyle titulo;
		final CellStyle encabezado;
		final CellStyle etiqueta;
		final CellStyle texto;
		final CellStyle numero;
		final CellStyle fechaHora;

		EstilosReporte(Workbook wb) {
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
			encabezado.setWrapText(true);
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

			fechaHora = wb.createCellStyle();
			fechaHora.setAlignment(HorizontalAlignment.CENTER);
			conBordes(fechaHora);
		}

		private static void conBordes(CellStyle s) {
			s.setBorderBottom(BorderStyle.THIN);
			s.setBorderTop(BorderStyle.THIN);
			s.setBorderLeft(BorderStyle.THIN);
			s.setBorderRight(BorderStyle.THIN);
		}
	}
}
