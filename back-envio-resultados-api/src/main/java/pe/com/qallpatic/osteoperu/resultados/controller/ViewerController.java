package pe.com.qallpatic.osteoperu.resultados.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import pe.com.qallpatic.osteoperu.compartido.excepcion.RecursoNoEncontradoException;
import pe.com.qallpatic.osteoperu.compartido.util.UtilInfoCliente;
import pe.com.qallpatic.osteoperu.resultados.dto.FileViewItem;
import pe.com.qallpatic.osteoperu.resultados.entity.ArchivoResultado;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;
import pe.com.qallpatic.osteoperu.resultados.service.StorageService;
import pe.com.qallpatic.osteoperu.resultados.service.ViewerService;

import java.nio.file.Path;
import java.util.List;

/** Visor web publico de resultados del paciente. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ViewerController {

	private final ViewerService viewerService;
	private final StorageService storageService;

	/** Pagina de resultados: se abre desde el link del SMS. */
	@GetMapping("/r/{token}")
	public String viewResults(@PathVariable String token, HttpServletRequest request, Model model) {
		Resultado resultado = viewerService.loadAndRegisterView(
				token, UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));

		List<FileViewItem> items = resultado.getArchivos().stream()
				.map(a -> new FileViewItem(
						a.getId(),
						a.getNombreOriginal(),
						"/r/" + token + "/archivo/" + a.getId(),
						"/r/" + token + "/descargar/" + a.getId(),
						a.getTipoMedio(),
						humanSize(a.getTamanoBytes())))
				.toList();

		model.addAttribute("dni", resultado.getDni());
		model.addAttribute("files", items);
		return "resultados";
	}

	/** Sirve un archivo en linea (inline) para visualizarlo en el navegador. */
	@GetMapping("/r/{token}/archivo/{fileId}")
	@ResponseBody
	public ResponseEntity<Resource> viewFile(@PathVariable String token, @PathVariable Long fileId) {
		return serveFile(token, fileId, false);
	}

	/** Fuerza la descarga del archivo. */
	@GetMapping("/r/{token}/descargar/{fileId}")
	@ResponseBody
	public ResponseEntity<Resource> downloadFile(@PathVariable String token, @PathVariable Long fileId,
			HttpServletRequest request) {
		Resultado resultado = viewerService.getByToken(token);
		viewerService.registerDownload(resultado.getId(),
				UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));
		return serveFile(token, fileId, true);
	}

	private ResponseEntity<Resource> serveFile(String token, Long fileId, boolean asAttachment) {
		Resultado resultado = viewerService.getByToken(token);
		ArchivoResultado archivo = viewerService.findFile(resultado, fileId);

		Path path = storageService.resolveFile(resultado.getCarpeta(), archivo.getNombreAlmacenado());
		Resource resource = new FileSystemResource(path);

		org.springframework.http.MediaType mediaType = MediaTypeFactory.getMediaType(resource)
				.orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

		String disposition = (asAttachment ? "attachment" : "inline")
				+ "; filename=\"" + archivo.getNombreOriginal() + "\"";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.contentType(mediaType)
				.body(resource);
	}

	/** Muestra una pagina amigable cuando el link no existe o expiro. */
	@ExceptionHandler(RecursoNoEncontradoException.class)
	public String handleNotFound(RecursoNoEncontradoException ex, Model model) {
		model.addAttribute("mensaje", ex.getMessage());
		return "error";
	}

	private static String humanSize(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		String[] units = {"KB", "MB", "GB"};
		double size = bytes;
		int i = -1;
		do {
			size /= 1024.0;
			i++;
		} while (size >= 1024 && i < units.length - 1);
		return String.format("%.1f %s", size, units[i]);
	}
}
