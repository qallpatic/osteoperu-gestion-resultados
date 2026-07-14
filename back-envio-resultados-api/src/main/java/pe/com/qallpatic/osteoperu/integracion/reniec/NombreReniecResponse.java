package pe.com.qallpatic.osteoperu.integracion.reniec;

/**
 * Nombre completo obtenido de RENIEC para autocompletar formularios.
 * Usado por los modulos de resultados, admin y admision.
 */
public record NombreReniecResponse(String dni, String nombre) {
}