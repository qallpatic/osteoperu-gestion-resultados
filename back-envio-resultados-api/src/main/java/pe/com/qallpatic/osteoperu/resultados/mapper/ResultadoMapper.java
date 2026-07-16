package pe.com.qallpatic.osteoperu.resultados.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.com.qallpatic.osteoperu.resultados.dto.ArchivoResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.LogCargaResponse;
import pe.com.qallpatic.osteoperu.resultados.dto.ResultadoResumenResponse;
import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoArchivo;
import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;

@Mapper(componentModel = "spring")
public interface ResultadoMapper {

	@Mapping(source = "idResultadoCarga", target = "id")
	@Mapping(source = "documentoIdentidad", target = "dni")
	ResultadoResumenResponse toResumen(ResultadoCarga carga);

	@Mapping(source = "idResultadoArchivo", target = "id")
	@Mapping(target = "activo", expression = "java(archivo.getEstado() != null && archivo.getEstado() == 1)")
	ArchivoResponse toArchivo(ResultadoArchivo archivo);

	LogCargaResponse toLog(LogCarga log);
}
