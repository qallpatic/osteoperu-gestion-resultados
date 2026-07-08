package pe.com.qallpatic.osteoperu.resultados.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros dinamicos para la consulta de logs de carga. */
public final class LogCargaSpecs {

	private LogCargaSpecs() {
	}

	public static Specification<LogCarga> filtro(String dni, LocalDateTime desde, LocalDateTime hasta) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (dni != null && !dni.isBlank()) {
				ps.add(cb.like(root.get("dni"), "%" + dni.trim() + "%"));
			}
			if (desde != null) {
				ps.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
			}
			if (hasta != null) {
				ps.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
			}
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}
}
