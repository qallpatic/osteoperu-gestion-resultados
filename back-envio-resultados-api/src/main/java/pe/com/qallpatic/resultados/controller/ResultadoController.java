package pe.com.qallpatic.resultados.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.com.qallpatic.resultados.dto.UploadResultadoResponse;
import pe.com.qallpatic.resultados.service.ResultadoService;

import java.util.List;

@RestController
@RequestMapping("/api/resultados")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resultados", description = "Carga de resultados de pacientes y envio del link por SMS")
public class ResultadoController {

	private final ResultadoService resultadoService;

	@Operation(
			summary = "Subir resultados de un paciente",
			description = "Crea la carpeta {telefono}_{dni}, guarda los archivos (PDF/imagen/video), "
					+ "genera el link corto y encola el SMS con el enlace al visor de resultados.",
			security = @SecurityRequirement(name = "basicAuth"))
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UploadResultadoResponse> upload(
			@Parameter(description = "Numero destino (9 digitos o con prefijo 51)")
			@RequestParam("phone") String phone,
			@Parameter(description = "DNI del paciente (8 digitos)")
			@RequestParam("dni") String dni,
			@Parameter(description = "Nombre del paciente (opcional, se usa en el SMS)")
			@RequestParam(value = "patientName", required = false) String patientName,
			@Parameter(description = "Archivos de resultados",
					content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
							schema = @Schema(type = "array", format = "binary")))
			@RequestParam("files") List<MultipartFile> files,
			Authentication authentication) {

		String createdBy = authentication != null ? authentication.getName() : "anonymous";
		log.info("INICIO - Carga de resultados. phone={}, dni={}, archivos={}, user={}",
				phone, dni, files == null ? 0 : files.size(), createdBy);

		UploadResultadoResponse response =
				resultadoService.uploadResultados(phone, dni, patientName, files, createdBy);

		log.info("FIN - Carga de resultados. batch={}", response.batchId());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
