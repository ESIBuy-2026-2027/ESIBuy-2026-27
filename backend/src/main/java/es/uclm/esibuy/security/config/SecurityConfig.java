package es.uclm.esibuy.security.config;

import java.io.IOException;
import java.util.List;

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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);
        private static final String ROLE_CLIENT = "CLIENT";
        private static final String ROLE_ADMIN = "ADMIN";
        private static final String ROLE_SELLER = "SELLER";

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
                                                        .csrfTokenRequestHandler(
                                                                        new CsrfTokenRequestAttributeHandler()))
                                        .securityContext(context -> context
                                                        .securityContextRepository(securityContextRepository))
                                        .sessionManagement(session -> session
                                                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                        .exceptionHandling(errors -> errors
                                                        .authenticationEntryPoint((request, response,
                                                                        exception) -> writeSecurityError(
                                                                                        request, response,
                                                                                        HttpServletResponse.SC_UNAUTHORIZED,
                                                                                        "Autenticación requerida"))
                                                        .accessDeniedHandler((request, response,
                                                                        exception) -> writeSecurityError(
                                                                                        request, response,
                                                                                        HttpServletResponse.SC_FORBIDDEN,
                                                                                        "No tiene permisos para realizar esta acción")))
                                        .authorizeHttpRequests(auth -> auth
                                                        .requestMatchers(HttpMethod.GET, "/api/categories",
                                                                        "/api/categorias")
                                                        .permitAll()
                                                        .requestMatchers(
                                                                        "/", "/api/users/register/**",
                                                                        "/api/users/login", "/actuator/health",
                                                                        "/api/users/login/verify-otp")
                                                        .permitAll()
                                                        .requestMatchers(HttpMethod.POST, "/api/users/admin")
                                                        .hasRole(ROLE_ADMIN)
                                                        .requestMatchers("/api/admin/**", "/api/v1/admin/**")
                                                        .hasRole(ROLE_ADMIN)
                                                        .requestMatchers(HttpMethod.GET, "/api/products/mine",
                                                                        "/api/products/summary",
                                                                        "/api/products/resumen")
                                                        .hasRole(ROLE_SELLER)
                                                        .requestMatchers(HttpMethod.POST, "/api/products")
                                                        .hasRole(ROLE_SELLER)
                                                        .requestMatchers(HttpMethod.PATCH, "/api/products/*/status")
                                                        .hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                                                        .requestMatchers(HttpMethod.GET, "/api/products",
                                                                        "/api/products/*")
                                                        .permitAll()
                                                        .requestMatchers("/api/catalog/**").hasRole(ROLE_CLIENT)
                                                        .requestMatchers("/api/products/**").hasRole(ROLE_SELLER)
                                                        .requestMatchers("/api/wishlist/**").hasRole(ROLE_CLIENT)
                                                        .requestMatchers(HttpMethod.GET, "/api/users/me/profile")
                                                        .hasAnyRole(ROLE_CLIENT, ROLE_SELLER, ROLE_ADMIN)
                                                        .requestMatchers(HttpMethod.DELETE, "/api/users/me")
                                                        .hasAnyRole(ROLE_CLIENT, ROLE_SELLER)
                                                        .requestMatchers(HttpMethod.PUT, "/api/users/me/profile")
                                                        .hasAnyRole(ROLE_CLIENT, ROLE_SELLER, ROLE_ADMIN)
                                                        .requestMatchers("/api/users/me/addresses/**")
                                                        .hasRole(ROLE_CLIENT)
                                                        .requestMatchers("/api/users/me/payment-methods/**")
                                                        .hasRole(ROLE_CLIENT)
                                                        .requestMatchers(
                                                                        HttpMethod.POST,
                                                                        "/api/auth/password-reset/request",
                                                                        "/api/auth/password-reset/confirm")
                                                        .permitAll()
                                                        .requestMatchers("/api/auth/password-reset/**").permitAll()
                                                        .anyRequest().authenticated())
                                        .addFilterBefore(new CorrelationIdFilter(),
                                                        UsernamePasswordAuthenticationFilter.class)
                                        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                                        .build();
                } catch (Exception exception) {
                        throw new IllegalStateException("No se pudo construir la configuracion de seguridad",
                                        exception);
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
