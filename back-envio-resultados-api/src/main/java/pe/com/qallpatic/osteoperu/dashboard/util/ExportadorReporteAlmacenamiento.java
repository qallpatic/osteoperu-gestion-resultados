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

import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteArchivoRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el .xlsx del reporte "Almacenamiento usado" a partir de una lista
 * plana de {@link ReporteArchivoRow}.
 *
 * <p>Estructura del workbook:
 * <ol>
 *   <li><b>Resumen</b>: rango aplicado, total de archivos, peso total,
 *       activos vs anulados.</li>
 *   <li><b>Archivos</b>: lista plana con 13 columnas (metadatos del archivo
 *       + datos del paciente del cabecere + auditoria).</li>
 * </ol>
 */
public final class ExportadorReporteAlmacenamiento {

	private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private static final String[] HEADERS = {
			"Fecha carga", "DNI/CE", "Paciente", "Nombre original",
			"Tipo (MIME)", "Extension", "Peso (MB)", "Tipo medio",
			"Estado", "Fecha eliminacion", "Usuario eliminacion", "Motivo eliminacion",
			"Cargado por"
	};

	private static final int[] WIDTHS = {
			5_000, 4_000, 7_000, 8_000,
			5_500, 2_500, 3_500, 3_500,
			3_500, 5_500, 5_500, 9_000, 5_000
	};

	private ExportadorReporteAlmacenamiento() {
	}

	public static byte[] generar(List<ReporteArchivoRow> filas, String desde, String hasta) {
		try (XSSFWorkbook wb = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			EstilosReporte estilos = new EstilosReporte(wb);
			llenarHojaResumen(wb, estilos, filas, desde, hasta);
			llenarHojaArchivos(wb, estilos, filas);
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo serializar el Excel de almacenamiento", e);
		}
	}

	private static void llenarHojaResumen(Workbook wb, EstilosReporte e,
			List<ReporteArchivoRow> filas, String desde, String hasta) {
		Sheet sh = wb.createSheet("Resumen");
		sh.setColumnWidth(0, 9_000);
		sh.setColumnWidth(1, 14_000);

		long activos = filas.stream().filter(f -> f.estado() != null && f.estado() == 1).count();
		long anulados = filas.size() - activos;
		BigDecimal pesoTotal = filas.stream()
				.map(ReporteArchivoRow::pesoMb)
				.filter(p -> p != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		int fila = 0;
		titulo(sh, e, "Reporte de almacenamiento usado", fila++);
		fila++;
		fila = par(sh, e, fila, "Rango aplicado", "Desde " + desde + " hasta " + hasta);
		fila = par(sh, e, fila, "Total de archivos", String.format("%,d", filas.size()));
		fila = par(sh, e, fila, "Peso total acumulado", formatearPesoMb(pesoTotal));
		fila = par(sh, e, fila, "Activos", String.format("%,d", activos));
		fila = par(sh, e, fila, "Anulados (soft-delete)", String.format("%,d", anulados));
		fila++;
		fila = par(sh, e, fila, "Detalle",
				"Lista plana de archivos en la hoja 'Archivos'.");
	}

	private static void llenarHojaArchivos(Workbook wb, EstilosReporte e,
			List<ReporteArchivoRow> filas) {
		Sheet sh = wb.createSheet("Archivos");
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

		for (ReporteArchivoRow r : filas) {
			Row row = sh.createRow(fila++);
			celdaFechaHora(row, e.fechaHora, 0, r.fechaCreacion());
			celdaTexto(row, e.texto, 1, r.documentoIdentidad());
			celdaTexto(row, e.texto, 2, r.nombrePaciente());
			celdaTexto(row, e.texto, 3, r.nombreOriginal());
			celdaTexto(row, e.texto, 4, r.tipoContenido());
			celdaTexto(row, e.texto, 5, r.extension());
			celdaDecimal(row, e.decimal, 6, r.pesoMb());
			celdaTexto(row, e.texto, 7, r.tipoMedio());
			celdaTexto(row, e.texto, 8, estadoLegible(r.estado()));
			celdaFechaHora(row, e.fechaHora, 9, r.fechaEliminacion());
			celdaTexto(row, e.texto, 10, r.usuarioEliminacion());
			celdaTexto(row, e.texto, 11, r.motivoEliminacion());
			celdaTexto(row, e.texto, 12, r.usuarioCreacion());
		}
	}

	private static String estadoLegible(Integer estado) {
		if (estado == null) {
			return "";
		}
		return estado == 1 ? "Activo" : "Anulado";
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

	private static void celdaDecimal(Row row, CellStyle estilo, int col, BigDecimal valor) {
		Cell c = row.createCell(col);
		if (valor != null) {
			c.setCellValue(valor.setScale(2, RoundingMode.HALF_UP).doubleValue());
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
		final CellStyle decimal;
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

			decimal = wb.createCellStyle();
			decimal.setAlignment(HorizontalAlignment.RIGHT);
			decimal.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
			conBordes(decimal);

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
