package pe.com.qallpatic.osteoperu.integracion.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Extrae el texto de un PDF (para validar el nombre del paciente). */
@Component
@Slf4j
public class ValidadorPdf {

	public String extraerTexto(byte[] contenido) {
		try (PDDocument documento = Loader.loadPDF(contenido)) {
			return new PDFTextStripper().getText(documento);
		} catch (Exception e) {
			log.warn("No se pudo extraer texto del PDF: {}", e.getMessage());
			return "";
		}
	}
}
