package pe.com.qallpatic.osteoperu.seguridad.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Valida el JWT del header {@code Authorization: Bearer <token>} y, si es valido,
 * puebla el contexto de seguridad con el usuario y sus roles. Es stateless: no
 * consulta la base de datos (los roles viajan en el token).
 */
@Component
@RequiredArgsConstructor
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

	private final ProveedorJwt proveedorJwt;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {

		String token = extraerToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			proveedorJwt.validar(token).ifPresent(claims -> {
				@SuppressWarnings("unchecked")
				List<String> roles = claims.get("roles", List.class);
				var autoridades = UsuarioAutenticado.autoridadesDesdeRoles(
						roles == null ? Collections.emptyList() : roles);

				var autenticacion = new UsernamePasswordAuthenticationToken(
						claims.getSubject(), null, autoridades);
				autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(autenticacion);
			});
		}

		filterChain.doFilter(request, response);
	}

	private String extraerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7).trim();
		}
		return null;
	}
}
