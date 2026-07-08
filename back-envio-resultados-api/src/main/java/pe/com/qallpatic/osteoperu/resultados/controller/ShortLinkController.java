package pe.com.qallpatic.osteoperu.resultados.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import pe.com.qallpatic.osteoperu.compartido.util.UtilInfoCliente;
import pe.com.qallpatic.osteoperu.resultados.entity.EnlaceCorto;
import pe.com.qallpatic.osteoperu.resultados.service.ShortLinkService;
import pe.com.qallpatic.osteoperu.resultados.service.ViewerService;

import java.net.URI;

/** Acortador: resuelve el codigo, cuenta el click y redirige al visor. */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Acortador de links", description = "Redireccion publica de los links enviados por SMS")
public class ShortLinkController {

	private final ShortLinkService shortLinkService;
	private final ViewerService viewerService;

	@Operation(summary = "Abrir link corto", description = "Cuenta el click y redirige al destino real.")
	@GetMapping("/s/{code}")
	public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
		EnlaceCorto enlace = shortLinkService.resolveAndCount(code);

		if (enlace.getIdResultado() != null) {
			viewerService.registerClick(enlace.getIdResultado(), code,
					UtilInfoCliente.ipCliente(request), UtilInfoCliente.userAgent(request));
		}

		log.info("Redireccion link corto {} -> {}", code, enlace.getUrlOriginal());
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(enlace.getUrlOriginal()))
				.build();
	}
}
