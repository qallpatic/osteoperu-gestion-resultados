package pe.com.qallpatic.osteoperu.compartido.excepcion;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando la verificacion de reCAPTCHA falla: token ausente, score
 * por debajo del umbral, o error de Google siteverify. Mapea a HTTP 403.
 */
public class ExcepcionCaptchaInvalidoException extends ExcepcionNegocio {

	public ExcepcionCaptchaInvalidoException(String mensaje) {
		super(mensaje, HttpStatus.FORBIDDEN);
	}

	public static ExcepcionCaptchaInvalidoException porScore(double score, double umbral, List<String> errores) {
		StringBuilder sb = new StringBuilder();
		sb.append("Verificacion de seguridad fallida (score=").append(score)
				.append(", umbral=").append(umbral).append(").");
		if (errores != null && !errores.isEmpty()) {
			sb.append(" Codigos: ").append(String.join(", ", errores)).append(".");
		}
		return new ExcepcionCaptchaInvalidoException(sb.toString());
	}
}
