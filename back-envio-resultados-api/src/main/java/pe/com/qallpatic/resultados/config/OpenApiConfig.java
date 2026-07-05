package pe.com.qallpatic.resultados.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
		title = "API Envio de Resultados - OSTEOPERU",
		version = "1.0",
		description = "Carga de resultados de pacientes, envio por SMS con link corto y visor web con conteo de vistas."))
@SecurityScheme(
		name = "basicAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "basic")
public class OpenApiConfig {
}
