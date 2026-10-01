package es.uclm.esibuy.security.service;

import java.util.List;

import es.uclm.esibuy.users.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthenticationService {

    private final SecurityContextRepository securityContextRepository;

    public SessionAuthenticationService(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void authenticate(User user, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();

        List<SimpleGrantedAuthority> authorities = user.getRole() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null, authorities));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}