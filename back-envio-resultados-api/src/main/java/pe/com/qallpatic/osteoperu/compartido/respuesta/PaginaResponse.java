package pe.com.qallpatic.osteoperu.compartido.respuesta;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envoltura uniforme para respuestas paginadas. */
public record PaginaResponse<T>(
		List<T> contenido,
		int pagina,
		int tamano,
		long totalElementos,
		int totalPaginas,
		boolean ultima) {

	public static <T> PaginaResponse<T> de(Page<T> page) {
		return new PaginaResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast());
	}
}
