package pe.com.qallpatic.osteoperu.paciente.service;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.paciente.dto.ArchivoLocalizado;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;

import java.time.LocalDate;

/**
 * Portal de resultados. Si {@code esAdmin} es true (rol ADMIN) se ven TODOS los
 * resultados; en caso contrario (PACIENTE) solo los del propio DNI.
 */
public interface PacientePortalService {

	PaginaResponse<ResultadoResumenResponse> misResultados(String username, boolean esAdmin,
			LocalDate desde, LocalDate hasta, int pagina, int tamano);

	ResultadoDetalleResponse detalle(String username, boolean esAdmin, Long id);

	ArchivoLocalizado localizarArchivo(String username, boolean esAdmin, Long idResultado, Long idArchivo);
}
