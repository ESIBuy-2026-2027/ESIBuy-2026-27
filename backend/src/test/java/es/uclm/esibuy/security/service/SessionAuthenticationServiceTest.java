package es.uclm.esibuy.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

class SessionAuthenticationServiceTest {

    private SecurityContextRepository securityContextRepository;
    private SessionAuthenticationService service;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        securityContextRepository = mock(SecurityContextRepository.class);
        service = new SessionAuthenticationService(
                securityContextRepository,
                Duration.ofMinutes(30));

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        when(request.getSession(true)).thenReturn(session);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUserAndCreatesSessionWithConfiguredTimeout() {
        User user = mock(User.class);

        when(user.getId()).thenReturn("user-1");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(UserRole.CLIENT);
        when(user.isActive()).thenReturn(true);

        service.authenticate(user, request, response);

        verify(request).getSession(true);
        verify(session).setMaxInactiveInterval(1800);
        verify(request).changeSessionId();

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        assertEquals(true, authentication.isAuthenticated());

        AuthenticatedUserPrincipal principal = assertInstanceOf(
                AuthenticatedUserPrincipal.class,
                authentication.getPrincipal());

        assertEquals("user-1", principal.id());
        assertEquals("user@example.com", principal.email());
        assertEquals(UserRole.CLIENT, principal.role());
        assertEquals(true, principal.active());

        assertEquals(
                "ROLE_CLIENT",
                authentication.getAuthorities().iterator().next().getAuthority());

        verify(securityContextRepository)
                .saveContext(context, request, response);
    }

    @Test
    void authenticatesUserWithoutRoleAndCreatesEmptyAuthorities() {
        User user = mock(User.class);

        when(user.getId()).thenReturn("user-2");
        when(user.getEmail()).thenReturn("norole@example.com");
        when(user.getRole()).thenReturn(null);
        when(user.isActive()).thenReturn(false);

        service.authenticate(user, request, response);

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        assertTrueAuthoritiesAreEmpty(authentication);

        AuthenticatedUserPrincipal principal = assertInstanceOf(
                AuthenticatedUserPrincipal.class,
                authentication.getPrincipal());

        assertNull(principal.role());
        assertEquals(false, principal.active());

        verify(securityContextRepository)
                .saveContext(context, request, response);
    }

    private void assertTrueAuthoritiesAreEmpty(Authentication authentication) {
        org.junit.jupiter.api.Assertions.assertTrue(
                authentication.getAuthorities().isEmpty());
    }
}