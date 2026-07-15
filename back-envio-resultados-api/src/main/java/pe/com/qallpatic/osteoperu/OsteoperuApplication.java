package pe.com.qallpatic.osteoperu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicacion OSTEOPERU (monolito modular).
 * El escaneo de componentes cubre todos los dominios bajo el paquete raiz
 * {@code pe.com.qallpatic.osteoperu}: resultados, seguridad, usuarios, etc.
 * El scheduling esta habilitado para tareas internas (ej. limpieza de la tabla
 * de rate-limit de recuperacion de password).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OsteoperuApplication {

	public static void main(String[] args) {
		SpringApplication.run(OsteoperuApplication.class, args);
	}
}
