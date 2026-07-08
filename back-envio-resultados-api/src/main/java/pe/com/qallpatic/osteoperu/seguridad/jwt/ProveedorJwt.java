package pe.com.qallpatic.osteoperu.seguridad.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Genera y valida los access tokens JWT (firma HS256). */
@Slf4j
@Component
public class ProveedorJwt {

	private final SeguridadProperties propiedades;
	private final SecretKey clave;

	public ProveedorJwt(SeguridadProperties propiedades) {
		this.propiedades = propiedades;
		this.clave = Keys.hmacShaKeyFor(propiedades.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
	}

	/** Emite un access token con el usuario como subject y sus roles/uid como claims. */
	public String generarAccessToken(UsuarioAutenticado usuario) {
		Date ahora = new Date();
		Date expira = new Date(ahora.getTime() + propiedades.getJwt().getAccessExpirationMs());
		List<String> roles = usuario.getRoles().stream().toList();

		return Jwts.builder()
				.subject(usuario.getUsername())
				.claim("uid", usuario.getId())
				.claim("roles", roles)
				.issuer(propiedades.getJwt().getIssuer())
				.issuedAt(ahora)
				.expiration(expira)
				.signWith(clave)
				.compact();
	}

	/** Duracion del access token en segundos (para exponerla al cliente). */
	public long getAccessExpirationSeconds() {
		return propiedades.getJwt().getAccessExpirationMs() / 1000;
	}

	/** Valida la firma/expiracion/issuer y devuelve los claims, o vacio si es invalido. */
	public Optional<Claims> validar(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(clave)
					.requireIssuer(propiedades.getJwt().getIssuer())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(claims);
		} catch (JwtException | IllegalArgumentException e) {
			log.debug("Token JWT invalido: {}", e.getMessage());
			return Optional.empty();
		}
	}
}
