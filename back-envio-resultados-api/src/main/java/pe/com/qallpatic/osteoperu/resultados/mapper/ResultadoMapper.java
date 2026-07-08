package pe.com.qallpatic.osteoperu.resultados.mapper;

import org.mapstruct.Mapper;

import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.entity.ArchivoResultado;
import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;

/**
 * Mapea entidades de resultados a sus DTO de respuesta. El detalle se construye
 * en el servicio porque requiere componer la URL publica del visor.
 */
@Mapper(componentModel = "spring")
public interface ResultadoMapper {

	ResultadoResumenResponse toResumen(Resultado resultado);

	ArchivoResponse toArchivo(ArchivoResultado archivo);

	LogCargaResponse toLog(LogCarga log);
}
