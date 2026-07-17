package pe.com.qallpatic.osteoperu.seguridad.service;

import pe.com.qallpatic.osteoperu.seguridad.dto.RecuperarPasswordResponse;

/**
 * Caso de uso publico para que un paciente pueda recibir su clave de acceso
 * por SMS. Por seguridad, la implementacion siempre devuelve un mensaje
 * generico (exista o no el paciente en BD); el envio real del SMS y la
 * actualizacion de la clave ocurren solo cuando aplica.
 */
public interface ServicioRecuperacionPassword {

	/**
	 * Procesa una solicitud. La evaluacion de rate-limit sucede dentro y, si
	 * se supera, lanza una excepcion que el controller convierte a 429.
	 *
	 * @param ip            IP del cliente (header X-Forwarded-For si hay proxy).
	 * @param documento     DNI (8 digitos) o CE (9-12 digitos) del paciente.
	 * @return respuesta generica + telefono ofuscado + telefono de soporte + retryAfter si fue bloqueado.
	 */
	RecuperarPasswordResponse solicitar(String ip, String documento);
}
