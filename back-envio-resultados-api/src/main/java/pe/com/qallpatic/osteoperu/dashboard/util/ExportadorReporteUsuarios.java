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

import pe.com.qallpatic.osteoperu.dashboard.dto.ReporteUsuarioRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Genera el .xlsx del reporte "Usuarios registrados" a partir de una lista
 * plana de {@link ReporteUsuarioRow}. Mantiene la logica de export fuera
 * del service.
 *
 * <p>Estructura del workbook:
 * <ol>
 *   <li><b>Resumen</b>: rango aplicado, total de usuarios, activos vs inactivos.</li>
 *   <li><b>Usuarios</b>: lista plana con 10 columnas (incluye roles CSV y
 *       campos de auditoria para 'creado por' / 'modificado por').</li>
 * </ol>
 */
public final class ExportadorReporteUsuarios {

	private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private static final String[] HEADERS = {
			"Fecha creacion", "Login", "Nombre", "DNI/CE", "Correo",
			"Roles", "Estado", "Fecha modificacion", "Creado por", "Modificado por"
	};

	private static final int[] WIDTHS = {
			5_500, 5_000, 7_000, 4_000, 7_000,
			6_000, 3_500, 5_500, 5_000, 5_000
	};

	private ExportadorReporteUsuarios() {
	}

	public static byte[] generar(List<ReporteUsuarioRow> filas, String desde, String hasta) {
		try (XSSFWorkbook wb = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			EstilosReporte estilos = new EstilosReporte(wb);
			llenarHojaResumen(wb, estilos, filas, desde, hasta);
			llenarHojaUsuarios(wb, estilos, filas);
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo serializar el Excel de usuarios", e);
		}
	}

	private static void llenarHojaResumen(Workbook wb, EstilosReporte e,
			List<ReporteUsuarioRow> filas, String desde, String hasta) {
		Sheet sh = wb.createSheet("Resumen");
		sh.setColumnWidth(0, 9_000);
		sh.setColumnWidth(1, 14_000);

		long activos = filas.stream().filter(f -> Boolean.TRUE.equals(f.estado())).count();
		long inactivos = filas.size() - activos;

		int fila = 0;
		titulo(sh, e, "Reporte de usuarios registrados", fila++);
		fila++;
		fila = par(sh, e, fila, "Rango aplicado", "Desde " + desde + " hasta " + hasta);
		fila = par(sh, e, fila, "Total de usuarios", String.format("%,d", filas.size()));
		fila = par(sh, e, fila, "Activos", String.format("%,d", activos));
		fila = par(sh, e, fila, "Inactivos", String.format("%,d", inactivos));
		fila++;
		fila = par(sh, e, fila, "Detalle",
				"Lista plana de usuarios en la hoja 'Usuarios'.");
	}

	private static void llenarHojaUsuarios(Workbook wb, EstilosReporte e,
			List<ReporteUsuarioRow> filas) {
		Sheet sh = wb.createSheet("Usuarios");
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

		for (ReporteUsuarioRow r : filas) {
			Row row = sh.createRow(fila++);
			celdaFechaHora(row, e.fechaHora, 0, r.fechaCreacion());
			celdaTexto(row, e.texto, 1, r.usuario());
			celdaTexto(row, e.texto, 2, r.nombreCompleto());
			celdaTexto(row, e.texto, 3, r.dni());
			celdaTexto(row, e.texto, 4, r.correo());
			celdaTexto(row, e.texto, 5, rolesComoCsv(r.roles()));
			celdaTexto(row, e.texto, 6, estadoLegible(r.estado()));
			celdaFechaHora(row, e.fechaHora, 7, r.fechaModificacion());
			celdaTexto(row, e.texto, 8, r.usuarioCreacion());
			celdaTexto(row, e.texto, 9, r.usuarioModificacion());
		}
	}

	private static String rolesComoCsv(List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return "";
		}
		return roles.stream().collect(Collectors.joining(", "));
	}

	private static String estadoLegible(Boolean estado) {
		if (estado == null) {
			return "";
		}
		return estado ? "Activo" : "Inactivo";
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

	/**
	 * Conjunto de estilos reutilizados por todas las hojas.
	 */
	private static final class EstilosReporte {
		final CellStyle titulo;
		final CellStyle encabezado;
		final CellStyle etiqueta;
		final CellStyle texto;
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
