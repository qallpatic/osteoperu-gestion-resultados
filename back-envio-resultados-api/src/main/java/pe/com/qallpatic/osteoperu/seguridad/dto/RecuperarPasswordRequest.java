package pe.com.qallpatic.osteoperu.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud publica al endpoint de recuperacion de password. Se pide el DNI
 * (o CE) del paciente; el back busca el telefono asociado en PACIENTE.TELEFONO,
 * regenera la clave y envia el SMS a ese telefono. La validacion del formato
 * basico (8-12 digitos) se hace aqui para responder rapido con 400 antes de
 * tocar el rate-limit.
 *
 * @param documento DNI (8 digitos) o Carnet de Extranjeria (9-12 digitos).
 */
public record RecuperarPasswordRequest(
		@NotBlank(message = "El documento es obligatorio.")
		@Pattern(regexp = "^\\d{8,12}$", message = "Documento invalido. DNI debe tener 8 digitos y CE entre 9 y 12.")
		String documento) {
}
