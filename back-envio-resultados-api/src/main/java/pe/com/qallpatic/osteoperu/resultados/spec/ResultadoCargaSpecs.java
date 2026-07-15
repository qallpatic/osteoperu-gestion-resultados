package pe.com.qallpatic.osteoperu.resultados.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import pe.com.qallpatic.osteoperu.resultados.entity.ResultadoCarga;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros dinamicos para la consulta de cabeceras (DNI, nombre, rango de fechas, estado). */
public final class ResultadoCargaSpecs {

	private ResultadoCargaSpecs() {
	}

	public static Specification<ResultadoCarga> filtro(String dni, String nombre,
			Integer estado, LocalDateTime desde, LocalDateTime hasta) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (dni != null && !dni.isBlank()) {
				ps.add(cb.like(root.get("documentoIdentidad"), "%" + dni.trim() + "%"));
			}
			if (nombre != null && !nombre.isBlank()) {
				ps.add(cb.like(cb.lower(root.get("nombrePaciente")), "%" + nombre.toLowerCase().trim() + "%"));
			}
			if (estado != null) {
				ps.add(cb.equal(root.get("estado"), estado));
			}
			rangoFechas(desde, hasta, cb, root, ps);
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	/** Cabeceras del paciente (DNI exacto) y activas. Para el portal del paciente. */
	public static Specification<ResultadoCarga> delPacienteActivo(String dni,
			LocalDateTime desde, LocalDateTime hasta) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			ps.add(cb.equal(root.get("documentoIdentidad"), dni));
			ps.add(cb.equal(root.get("estado"), 1));
			rangoFechas(desde, hasta, cb, root, ps);
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	private static void rangoFechas(LocalDateTime desde, LocalDateTime hasta,
			jakarta.persistence.criteria.CriteriaBuilder cb,
			jakarta.persistence.criteria.Root<ResultadoCarga> root, List<Predicate> ps) {
		if (desde != null) {
			ps.add(cb.greaterThanOrEqualTo(root.get("fechaHoraCarga"), desde));
		}
		if (hasta != null) {
			ps.add(cb.lessThanOrEqualTo(root.get("fechaHoraCarga"), hasta));
		}
	}
}
