package es.uclm.esibuy.security.config;

import java.io.IOException;
import java.util.List;

import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie,
            @Value("${server.servlet.session.cookie.same-site:Lax}") String sameSite) {

        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();

        repository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .secure(secureCookie)
                .sameSite(sameSite));

        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository) {
        try {
            return http
                    .cors(Customizer.withDefaults())
                    .csrf(csrf -> csrf
                            .csrfTokenRepository(csrfTokenRepository)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                    .securityContext(context -> context.securityContextRepository(securityContextRepository))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                    request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                    "Autenticación requerida"))
                            .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                    request, response, HttpServletResponse.SC_FORBIDDEN,
                                    "No tiene permisos para realizar esta acción")))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categorias").permitAll()
                            .requestMatchers(
                                    "/", "/api/users/register/**", "/api/users/login", "/actuator/health")
                            .permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/users/admin").hasRole("ADMIN")
                            .requestMatchers("/api/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/products/**").hasRole("SELLER")
                            .requestMatchers(HttpMethod.GET, "/api/users/me/profile").hasRole("CLIENT")
                            .anyRequest().authenticated())
                    .addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo construir la configuracion de seguridad", exception);
        }
    }

    private void writeSecurityError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message) throws IOException {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String id = correlationId == null ? "" : correlationId.toString();
        LOGGER.warn("Solicitud de seguridad rechazada. status={}, correlationId={}", status, id);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationIdFilter.HEADER, id);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message
                + "\",\"correlationId\":\"" + id + "\"}");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Retry-After", CorrelationIdFilter.HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
