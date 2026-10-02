package es.uclm.esibuy.users.controller;

import java.util.Map;

import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.TooManyRequestsException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public UserController(
            UserService userService,
            LoginAttemptService loginAttemptService,
            SessionAuthenticationService sessionAuthenticationService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @PostMapping("/register/client")
    public ResponseEntity<Map<String, Object>> registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        User user = userService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildUserResponse(user));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<Map<String, Object>> registerSeller(@Valid @RequestBody SellerRegistrationRequest request) {
        User user = userService.registerSeller(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildUserResponse(user));
    }

    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        User user = userService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String ipAddress = servletRequest.getRemoteAddr();
        if (loginAttemptService.isBlocked(request.getEmail(), ipAddress)) {
            throw new TooManyRequestsException(
                    loginAttemptService.getRemainingBlockSeconds(request.getEmail(), ipAddress));
        }

        User user;
        try {
            user = userService.login(request);
        } catch (InvalidCredentialsException exception) {
            loginAttemptService.loginFailed(request.getEmail(), ipAddress);
            throw exception;
        }

        loginAttemptService.loginSucceeded(request.getEmail(), ipAddress);
        sessionAuthenticationService.authenticate(user, servletRequest, servletResponse);
        return ResponseEntity.ok(buildUserResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(buildUserResponse(principal));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> buildUserResponse(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "active", user.isActive());
    }

    private Map<String, Object> buildUserResponse(AuthenticatedUserPrincipal principal) {
        return Map.of(
                "id", principal.id(),
                "email", principal.email(),
                "role", principal.role(),
                "active", principal.active());
    }
}
