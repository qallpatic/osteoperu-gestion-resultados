package pe.com.qallpatic.osteoperu.resultados.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import pe.com.qallpatic.osteoperu.resultados.entity.Resultado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros dinamicos para la consulta de resultados (DNI, nombre, rango de fechas). */
public final class ResultadoSpecs {

	private ResultadoSpecs() {
	}

	public static Specification<Resultado> filtro(String dni, String nombre,
			LocalDateTime desde, LocalDateTime hasta) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (dni != null && !dni.isBlank()) {
				ps.add(cb.like(root.get("dni"), "%" + dni.trim() + "%"));
			}
			if (nombre != null && !nombre.isBlank()) {
				ps.add(cb.like(cb.lower(root.get("nombrePaciente")), "%" + nombre.toLowerCase().trim() + "%"));
			}
			rangoFechas(desde, hasta, cb, root, ps);
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	/** Filtro por DNI EXACTO (portal del paciente: no debe ver DNIs ajenos). */
	public static Specification<Resultado> delPaciente(String dni, LocalDateTime desde, LocalDateTime hasta) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			ps.add(cb.equal(root.get("dni"), dni));
			rangoFechas(desde, hasta, cb, root, ps);
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	private static void rangoFechas(LocalDateTime desde, LocalDateTime hasta,
			jakarta.persistence.criteria.CriteriaBuilder cb,
			jakarta.persistence.criteria.Root<Resultado> root, List<Predicate> ps) {
		if (desde != null) {
			ps.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde));
		}
		if (hasta != null) {
			ps.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta));
		}
	}
}
