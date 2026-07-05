package pe.com.qallpatic.resultados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackEnvioResultadosApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackEnvioResultadosApplication.class, args);
	}
}
