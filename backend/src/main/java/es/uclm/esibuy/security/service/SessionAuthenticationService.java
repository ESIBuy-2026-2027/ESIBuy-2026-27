package es.uclm.esibuy.security.service;

import java.time.Duration;
import java.util.List;

import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthenticationService {

        private final SecurityContextRepository securityContextRepository;
        private final Duration sessionTimeout;

        public SessionAuthenticationService(
                        SecurityContextRepository securityContextRepository,
                        @Value("${server.servlet.session.timeout:30m}") Duration sessionTimeout) {
                this.securityContextRepository = securityContextRepository;
                this.sessionTimeout = sessionTimeout;
        }

        public void authenticate(User user, HttpServletRequest request, HttpServletResponse response) {
                HttpSession session = request.getSession(true);
                session.setMaxInactiveInterval(Math.toIntExact(sessionTimeout.toSeconds()));
                request.changeSessionId();

                List<SimpleGrantedAuthority> authorities = user.getRole() == null
                                ? List.of()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                user.getId(), user.getEmail(), user.getRole(), user.isActive());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, authorities));
                SecurityContextHolder.setContext(context);
                securityContextRepository.saveContext(context, request, response);
        }
}
