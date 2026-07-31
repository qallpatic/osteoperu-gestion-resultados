package pe.com.qallpatic.osteoperu.pacientes.dto;

/**
 * Datos demograficos basicos del paciente para autocompletar formularios
 * (nombre + telefono) cuando ya esta registrado. No expone datos sensibles
 * (id, idUsuario, etc.).
 *
 * {@code usuarioExiste} indica si hay fila en la tabla PACIENTE para ese documento:
 *  - true  -> el operador lo dio de alta en Admision (puede o no tener nombre/telefono cargados).
 *  - false -> no hay fila, por lo que tampoco existe usuario del portal.
 * El front usa este flag para advertir al operador antes de cargar resultados
 * a un paciente sin cuenta.
 */
public record PacienteLookupResponse(String dni, String nombre, String telefono, boolean usuarioExiste) {
}