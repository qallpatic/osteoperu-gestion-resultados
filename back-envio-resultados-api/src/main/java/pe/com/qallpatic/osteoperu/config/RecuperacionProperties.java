package pe.com.qallpatic.osteoperu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del flujo de recuperacion de password bajo el prefijo
 * {@code recuperacion.*}. Las variables de entorno que los sobreescriben se
 * documentan en .env.example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "recuperacion")
public class RecuperacionProperties {

	/** Longitud de la clave temporal generada (sin ambiguos 0/O/1/I). */
	private int longitudClave = 6;

	/** Maximo de intentos por IP en la ventana. */
	private int maxIntentosIp = 3;

	/** Maximo de intentos por telefono normalizado en la ventana. */
	private int maxIntentosTelefono = 3;

	/** Maximo de telefonos distintos solicitados desde la misma IP en la ventana. */
	private int maxDistintosPorIp = 3;

	/** Tamano de la ventana deslizante, en minutos. */
	private int ventanaMinutos = 10;

	/** Retencion de la tabla de intentos antes de la purga (@Scheduled), en horas. */
	private int retentionHoras = 24;

	/** Cron para la tarea programada de limpieza. Default: a la hora en punto. */
	private String limpiezaCron = "0 0 * * * *";
}
