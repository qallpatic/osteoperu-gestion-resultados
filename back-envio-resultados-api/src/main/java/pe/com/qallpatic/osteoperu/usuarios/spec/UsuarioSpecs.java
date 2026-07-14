package pe.com.qallpatic.osteoperu.usuarios.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import pe.com.qallpatic.osteoperu.seguridad.entity.Usuario;

import java.util.ArrayList;
import java.util.List;

/** Construye filtros dinamicos para la busqueda de usuarios. */
public final class UsuarioSpecs {

	private UsuarioSpecs() {
	}

	/**
	 * @param buscar texto libre (usuario, nombre, correo o dni)
	 * @param rol    nombre de rol exacto (opcional)
	 * @param estado activo/inactivo (opcional)
	 */
	public static Specification<Usuario> filtro(String buscar, String rol, Boolean estado) {
		return (root, query, cb) -> {
			List<Predicate> predicados = new ArrayList<>();

			if (buscar != null && !buscar.isBlank()) {
				String like = "%" + buscar.toLowerCase() + "%";
				predicados.add(cb.or(
						cb.like(cb.lower(root.get("usuario")), like),
						cb.like(cb.lower(root.<String>get("nombre")), like),
						cb.like(cb.lower(root.<String>get("nombres")), like),
						cb.like(cb.lower(root.<String>get("apellidoPaterno")), like),
						cb.like(cb.lower(root.<String>get("apellidoMaterno")), like),
						cb.like(cb.lower(root.<String>get("correo")), like),
						cb.like(cb.lower(root.<String>get("dni")), like)));
			}

			if (estado != null) {
				predicados.add(cb.equal(root.get("estado"), estado));
			}

			if (rol != null && !rol.isBlank()) {
				if (query != null) {
					query.distinct(true);
				}
				Join<Object, Object> joinRoles = root.join("roles");
				predicados.add(cb.equal(joinRoles.get("nombre"), rol));
			}

			return cb.and(predicados.toArray(new Predicate[0]));
		};
	}
}