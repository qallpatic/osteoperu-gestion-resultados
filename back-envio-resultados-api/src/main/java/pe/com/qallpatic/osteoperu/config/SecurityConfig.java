package pe.com.qallpatic.osteoperu.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import pe.com.qallpatic.osteoperu.seguridad.jwt.FiltroAutenticacionJwt;
import pe.com.qallpatic.osteoperu.seguridad.jwt.ManejadorAccesoDenegado;
import pe.com.qallpatic.osteoperu.seguridad.jwt.PuntoEntradaNoAutorizado;
import pe.com.qallpatic.osteoperu.seguridad.service.DetalleUsuarioService;

import java.util.List;

/**
 * Seguridad basada en JWT (stateless) con autorizacion por roles.
 *  - Publico: login/refresh, visor y acortador (/r, /s), swagger, health.
 *  - Cada modulo protegido por su rol (ADMIN tiene acceso a todo por convencion
 *    de rutas). El visor Thymeleaf embebe PDFs vía frameOptions=sameOrigin.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AppProperties appProperties;
	private final FiltroAutenticacionJwt filtroAutenticacionJwt;
	private final PuntoEntradaNoAutorizado puntoEntradaNoAutorizado;
	private final ManejadorAccesoDenegado manejadorAccesoDenegado;
	private final DetalleUsuarioService detalleUsuarioService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(detalleUsuarioService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(puntoEntradaNoAutorizado)
						.accessDeniedHandler(manejadorAccesoDenegado))
				.authorizeHttpRequests(auth -> auth
						// Publico
						.requestMatchers("/s/**", "/r/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						// RENIEC: lookup compartido por resultados, admin y admision.
						// Ruta propia para no abrir /api/resultados a ADMISION solo por este caso.
						.requestMatchers(HttpMethod.GET, "/api/reniec/**").hasAnyRole("ADMIN", "RESULTADOS", "ADMISION")
						// Protegido por rol (ADMIN incluido en cada modulo)
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/resultados/**").hasAnyRole("ADMIN", "RESULTADOS")
						// Admision: lectura abierta a RESULTADOS (solo listado de pacientes),
						// escritura (generar/anular cuenta) restringida a ADMIN/ADMISION.
						.requestMatchers(HttpMethod.GET, "/api/admision/pacientes").hasAnyRole("ADMIN", "RESULTADOS", "ADMISION")
						.requestMatchers("/api/admision/**").hasAnyRole("ADMIN", "ADMISION", "RESULTADOS")
						// Lookup de paciente por DNI (autocompletar nombre/telefono en carga de resultados).
						// Mismos roles que RENIEC: lo usan resultados, admin y admision.
						.requestMatchers("/api/pacientes/**").hasAnyRole("ADMIN", "RESULTADOS", "ADMISION")
						.requestMatchers("/api/paciente/**").hasAnyRole("ADMIN", "PACIENTE")
						.requestMatchers("/api/callcenter/**").hasAnyRole("ADMIN", "CALLCENTER")
						.anyRequest().authenticated())
				.addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/** Origenes permitidos configurables via {@code app.cors.allowed-origins}. */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		// El front autentica con header Authorization (Bearer), no con cookies.
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
