package pe.com.qallpatic.resultados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad:
 *  - El panel/endpoints de carga de resultados exigen Basic Auth.
 *  - El acortador (/s/**), el visor de resultados (/r/**) y los archivos
 *    son publicos (el paciente no tiene cuenta; el token del link es el secreto).
 *  - CORS habilitado para que el front (Angular) consuma la API desde otro origen.
 */
@Configuration
public class SecurityConfig {

	private final AppProperties appProperties;

	public SecurityConfig(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				// Permite embeber los archivos (PDF) en un <iframe> del mismo origen (el visor).
				// Por defecto Spring Security envia X-Frame-Options: DENY, que lo bloquearia.
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.authorizeHttpRequests(auth -> auth
						// Rutas publicas para el paciente
						.requestMatchers("/s/**", "/r/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						// Todo lo demas (carga de resultados, metricas) requiere autenticacion
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	/** Origenes permitidos configurables via {@code app.cors.allowed-origins}. */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		// El front autentica con header Authorization (Basic), no con cookies.
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		UserDetails panelUser = User.withUsername(appProperties.getSecurity().getUsername())
				.password("{noop}" + appProperties.getSecurity().getPassword())
				.roles("PANEL")
				.build();
		return new InMemoryUserDetailsManager(panelUser);
	}
}
