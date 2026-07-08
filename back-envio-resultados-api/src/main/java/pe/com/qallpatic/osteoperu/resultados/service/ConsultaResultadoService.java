package pe.com.qallpatic.osteoperu.resultados.service;

import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.NombreReniecResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.PacientePendienteResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoDetalleResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoUpdateRequest;

import java.time.LocalDate;

/** Consulta de resultados cargados y de la bitacora de cargas. */
public interface ConsultaResultadoService {

	/** Consulta el nombre en RENIEC por DNI (para autocompletar la carga). */
	NombreReniecResponse consultarReniec(String dni);

	/** Pacientes registrados por Admision pendientes de subir resultados (campanita). */
	PaginaResponse<PacientePendienteResponse> pendientes(int pagina, int tamano);

	/** Cantidad de pacientes pendientes de subir resultados. */
	long contarPendientes();

	PaginaResponse<ResultadoResumenResponse> listar(String dni, String nombre,
			LocalDate desde, LocalDate hasta, int pagina, int tamano);

	ResultadoDetalleResponse detalle(Long id);

	ResultadoDetalleResponse actualizar(Long id, ResultadoUpdateRequest solicitud, String actor);

	PaginaResponse<LogCargaResponse> listarLogs(String dni, LocalDate desde, LocalDate hasta,
			int pagina, int tamano);
}
