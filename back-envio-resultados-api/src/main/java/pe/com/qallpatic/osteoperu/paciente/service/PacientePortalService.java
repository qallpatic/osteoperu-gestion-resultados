package pe.com.qallpatic.osteoperu.paciente.service;

import java.math.BigDecimal;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.paciente.dto.ArchivoLocalizado;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.service.ArchivoDescargable;

import java.time.LocalDate;

/**
 * Portal de resultados. Si {@code esAdmin} es true (rol ADMIN) se ven TODAS
 * las cabeceras; en caso contrario (PACIENTE) solo las del propio DNI y activas.
 */
public interface PacientePortalService {

	PaginaResponse<ResultadoResumenResponse> misResultados(String username, boolean esAdmin,
			LocalDate desde, LocalDate hasta, int pagina, int tamano);

	ResultadoDetalleResponse detalle(String username, boolean esAdmin, Long id);

	ResultadoDetalleResponse detallePorIdGenerado(String username, boolean esAdmin, String idGenerado);

	ArchivoLocalizado localizarArchivo(String username, boolean esAdmin, Long idResultadoCarga, Long idArchivo);

	ArchivoDescargable descargarArchivo(String username, boolean esAdmin, Long idResultadoCarga, Long idArchivo);

	void softDelete(String username, boolean esAdmin, Long idResultadoCarga, String motivo);

	BigDecimal pesoTotalCabecera(String idGenerado);
}
