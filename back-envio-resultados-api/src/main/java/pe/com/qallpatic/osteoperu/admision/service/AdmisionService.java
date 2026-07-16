package pe.com.qallpatic.osteoperu.admision.service;

import pe.com.qallpatic.osteoperu.admision.dto.CredencialResponse;
import pe.com.qallpatic.osteoperu.admision.dto.GenerarUsuarioRequest;
import pe.com.qallpatic.osteoperu.admision.dto.PacienteAdmisionResponse;
import pe.com.qallpatic.osteoperu.compartido.respuesta.PaginaResponse;

/** Casos de uso de Admision: listado de pacientes y alta/baja de su cuenta. */
public interface AdmisionService {

	/** Lista los pacientes con resultados y el estado de su cuenta de acceso. */
	PaginaResponse<PacienteAdmisionResponse> listarPacientes(String dni, int pagina, int tamano,
			String ordenarPor, String direccion);

	/** Crea (o reactiva si estaba anulada) la cuenta del paciente y devuelve la credencial con QR. */
	CredencialResponse generarUsuario(GenerarUsuarioRequest solicitud, String actor);

	/** Anula (desactiva) el acceso del paciente por su DNI. */
	void anularUsuario(String dni, String actor);
}
