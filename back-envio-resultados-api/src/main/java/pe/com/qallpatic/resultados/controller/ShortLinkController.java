package pe.com.qallpatic.resultados.controller;

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

import pe.com.qallpatic.resultados.model.ShortLink;
import pe.com.qallpatic.resultados.service.ShortLinkService;
import pe.com.qallpatic.resultados.service.ViewerService;
import pe.com.qallpatic.resultados.util.ClientInfoUtil;

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
		ShortLink link = shortLinkService.resolveAndCount(code);

		if (link.getBatchId() != null) {
			viewerService.registerClick(link.getBatchId(), code,
					ClientInfoUtil.clientIp(request), ClientInfoUtil.userAgent(request));
		}

		log.info("Redireccion link corto {} -> {}", code, link.getOriginalUrl());
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(link.getOriginalUrl()))
				.build();
	}
}
