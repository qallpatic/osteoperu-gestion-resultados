package pe.com.qallpatic.osteoperu.resultados.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.util.UtilInfoCliente;
import pe.com.qallpatic.osteoperu.resultados.dto.FileViewItem;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;
import pe.com.qallpatic.osteoperu.resultados.service.AlmacenamientoArchivos;
import pe.com.qallpatic.osteoperu.resultados.service.ArchivoDescargable;
import pe.com.qallpatic.osteoperu.resultados.service.ViewerService;

import java.util.List;

/** Visor web publico de resultados del paciente. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ViewerController {

	private final ViewerService viewerService;
	private final AlmacenamientoArchivos almacenamiento;

	@GetMapping("/r/{token}")
	public String viewResults(@PathVariable String token, HttpServletRequest request, Model model) {
		ResultadoCarga carga = viewerService.loadAndRegisterView(
				token, UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));

		List<FileViewItem> items = carga.getArchivos().stream()
				.filter(a -> a.getEstado() == null || a.getEstado() == 1)
				.map(a -> new FileViewItem(
						a.getIdResultadoArchivo(),
						a.getNombreOriginal(),
						token + "/archivo/" + a.getIdResultadoArchivo(),
						token + "/descargar/" + a.getIdResultadoArchivo(),
						a.getTipoMedio(),
						humanSizeMb(a.getPesoMb())))
				.toList();

		model.addAttribute("dni", carga.getDocumentoIdentidad());
		model.addAttribute("files", items);
		return "resultados";
	}

	@GetMapping("/r/{token}/archivo/{fileId}")
	@ResponseBody
	public ResponseEntity<Resource> viewFile(@PathVariable String token, @PathVariable Long fileId) {
		return serveFile(token, fileId, false);
	}

	@GetMapping("/r/{token}/descargar/{fileId}")
	@ResponseBody
	public ResponseEntity<Resource> downloadFile(@PathVariable String token, @PathVariable Long fileId,
			HttpServletRequest request) {
		ResultadoCarga carga = viewerService.getByToken(token);
		viewerService.registerDownload(carga.getIdResultadoCarga(),
				UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));
		return serveFile(token, fileId, true);
	}

	private ResponseEntity<Resource> serveFile(String token, Long fileId, boolean asAttachment) {
		ResultadoCarga carga = viewerService.getByToken(token);
		ResultadoArchivo archivo = viewerService.findFile(carga, fileId);

		if (archivo.getEstado() != null && archivo.getEstado() == 0) {
			throw new RecursoNoEncontradoException("Archivo no encontrado.");
		}

		ArchivoDescargable descargable = almacenamiento.descargar(
				archivo.getIdGenerado(), archivo.getIdArchivo(), archivo.getExtension(),
				carga.getDocumentoIdentidad(), carga.getFechaHoraCarga());

		MediaType mediaType = descargable.contentType() == null
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(descargable.contentType());

		String disposition = (asAttachment ? "attachment" : "inline")
				+ "; filename=\"" + archivo.getNombreOriginal() + "\"";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.contentType(mediaType)
				.body(new InputStreamResource(descargable.contenido()));
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(RecursoNoEncontradoException ex, Model model) {
		model.addAttribute("mensaje", ex.getMessage());
		return "error";
	}

	private static String humanSizeMb(java.math.BigDecimal pesoMb) {
		if (pesoMb == null) {
			return "0 B";
		}
		double mb = pesoMb.doubleValue();
		if (mb < 1.0) {
			long kb = Math.round(mb * 1024);
			return kb + " KB";
		}
		if (mb < 1024.0) {
			return String.format("%.1f MB", mb);
		}
		return String.format("%.2f GB", mb / 1024.0);
	}
}
