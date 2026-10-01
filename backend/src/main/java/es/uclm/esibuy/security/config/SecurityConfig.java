package es.uclm.esibuy.security.config;

import java.io.IOException;
import java.util.List;

import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) {
        try {
            return http
                    .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/api/users/register/**", "/api/users/login", "/api/admin/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                        .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/", "/api/users/register/**", "/api/users/login", "/api/admin/**",
                                "/actuator/health")
                            .permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "Autenticación requerida"))
                            .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                request, response, HttpServletResponse.SC_FORBIDDEN,
                                "No tiene permisos para realizar esta acción")))
                    .addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo construir la configuracion de seguridad", exception);
        }
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private void writeSecurityError(HttpServletRequest request,
            HttpServletResponse response, int status, String message) throws IOException {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String id = correlationId == null ? "" : correlationId.toString();
        response.setStatus(status);
        response.setContentType("application/json");
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
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
