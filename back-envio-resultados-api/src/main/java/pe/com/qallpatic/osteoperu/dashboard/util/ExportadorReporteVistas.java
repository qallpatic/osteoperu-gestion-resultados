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
import pe.com.qallpatic.osteoperu.dashboard.dto.VistasDetalleResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el .xlsx del reporte "Vistas de pacientes" a partir del detalle
 * calculado por el dashboard y una lista plana de eventos individuales.
 *
 * <p>Estructura del workbook:
 * <ol>
 *   <li><b>Resumen</b>: KPIs del dashboard para el rango (aperturas,
 *       descargas, ratio, adopcion) + desglose por tipo de evento
 *       (CLICK / VISTA / DESCARGA) contado sobre la lista plana.</li>
 *   <li><b>Eventos</b>: lista plana con 7 columnas (fecha, tipo, id
 *       resultado, DNI, paciente, IP, cliente).</li>
 * </ol>
 */
public final class ExportadorReporteVistas {

	private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private static final String[] HEADERS = {
			"Fecha", "Tipo evento", "ID Resultado",
			"DNI/CE", "Paciente", "IP", "Cliente"
	};

	private static final int[] WIDTHS = {
			5_000, 3_500, 5_500,
			4_000, 7_000, 4_000, 6_000
	};

	private ExportadorReporteVistas() {
	}

	public static byte[] generar(VistasDetalleResponse detalle, List<ReporteEventoVistaRow> filas) {
		try (XSSFWorkbook wb = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			EstilosReporte estilos = new EstilosReporte(wb);
			llenarHojaResumen(wb, estilos, detalle, filas);
			llenarHojaEventos(wb, estilos, filas);
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo serializar el Excel de vistas", e);
		}
	}

	private static void llenarHojaResumen(Workbook wb, EstilosReporte e,
			VistasDetalleResponse detalle, List<ReporteEventoVistaRow> filas) {
		Sheet sh = wb.createSheet("Resumen");
		sh.setColumnWidth(0, 10_000);
		sh.setColumnWidth(1, 16_000);

		// Desglose de la lista plana por tipo (siempre presente aunque falte detalle).
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
		fila = par(sh, e, fila, "Rango aplicado",
				"Desde " + detalle.desde() + " hasta " + detalle.hasta());

		fila++;
		fila = par(sh, e, fila, "KPIs del dashboard", "");
		fila = par(sh, e, fila, "Aperturas del visor en el rango",
				String.format("%,d", detalle.totalAperturas() == null ? 0L : detalle.totalAperturas()));
		fila = par(sh, e, fila, "Descargas de archivos en el rango",
				String.format("%,d", detalle.totalDescargas() == null ? 0L : detalle.totalDescargas()));
		fila = par(sh, e, fila, "Ratio descarga / apertura",
				String.format("%s%%",
						detalle.ratioDescargaPorApertura() == null
								? "0.0"
								: detalle.ratioDescargaPorApertura().toPlainString()));
		fila = par(sh, e, fila, "Hora pico de actividad",
				(detalle.horaPico() == null || detalle.totalHoraPico() == null)
						? "Sin actividad en el rango"
						: String.format("%02d:00 (%s interacciones)",
								detalle.horaPico(),
								String.format("%,d", detalle.totalHoraPico())));

		fila++;
		fila = par(sh, e, fila, "Eventos en el rango (lista plana)", "");
		fila = par(sh, e, fila, "Total de eventos", String.format("%,d", filas.size()));
		fila = par(sh, e, fila, "Clicks (link corto)",
				String.format("%,d", porTipo.getOrDefault("CLICK", 0L)));
		fila = par(sh, e, fila, "Vistas del visor (eventos)",
				String.format("%,d", porTipo.getOrDefault("VISTA", 0L)));
		fila = par(sh, e, fila, "Descargas de archivos (eventos)",
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
			celdaNumero(row, e.numero, 2, r.idResultado());
			celdaTexto(row, e.texto, 3, r.documentoIdentidad());
			celdaTexto(row, e.texto, 4, r.nombrePaciente());
			celdaTexto(row, e.texto, 5, r.ip());
			celdaTexto(row, e.texto, 6, r.userAgent());
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
