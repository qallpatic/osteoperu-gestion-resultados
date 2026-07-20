package pe.com.qallpatic.osteoperu.pacientes.dto;

/**
 * Datos demograficos basicos del paciente para autocompletar formularios
 * (nombre + telefono) cuando ya esta registrado. No expone datos sensibles
 * (id, idUsuario, etc.).
 */
public record PacienteLookupResponse(String dni, String nombre, String telefono) {
}