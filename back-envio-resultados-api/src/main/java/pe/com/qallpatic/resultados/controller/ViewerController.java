package pe.com.qallpatic.resultados.controller;

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

import pe.com.qallpatic.resultados.dto.FileViewItem;
import pe.com.qallpatic.resultados.exception.NotFoundException;
import pe.com.qallpatic.resultados.model.PatientResultBatch;
import pe.com.qallpatic.resultados.model.ResultFile;
import pe.com.qallpatic.resultados.service.StorageService;
import pe.com.qallpatic.resultados.service.ViewerService;
import pe.com.qallpatic.resultados.util.ClientInfoUtil;

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
		PatientResultBatch batch = viewerService.loadAndRegisterView(
				token, ClientInfoUtil.clientIp(request), ClientInfoUtil.userAgent(request));

		List<FileViewItem> items = batch.getFiles().stream()
				.map(f -> new FileViewItem(
						f.getId(),
						f.getOriginalFilename(),
						"/r/" + token + "/archivo/" + f.getId(),
						"/r/" + token + "/descargar/" + f.getId(),
						f.getMediaType(),
						humanSize(f.getSizeBytes())))
				.toList();

		model.addAttribute("dni", batch.getDni());
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
		PatientResultBatch batch = viewerService.getByToken(token);
		viewerService.registerDownload(batch.getId(),
				ClientInfoUtil.clientIp(request), ClientInfoUtil.userAgent(request));
		return serveFile(token, fileId, true);
	}

	private ResponseEntity<Resource> serveFile(String token, Long fileId, boolean asAttachment) {
		PatientResultBatch batch = viewerService.getByToken(token);
		ResultFile file = viewerService.findFile(batch, fileId);

		Path path = storageService.resolveFile(batch.getFolderName(), file.getStoredFilename());
		Resource resource = new FileSystemResource(path);

		org.springframework.http.MediaType mediaType = MediaTypeFactory.getMediaType(resource)
				.orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

		String disposition = (asAttachment ? "attachment" : "inline")
				+ "; filename=\"" + file.getOriginalFilename() + "\"";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.contentType(mediaType)
				.body(resource);
	}

	/** Muestra una pagina amigable cuando el link no existe o expiro. */
	@ExceptionHandler(NotFoundException.class)
	public String handleNotFound(NotFoundException ex, Model model) {
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
