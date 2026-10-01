package es.uclm.esibuy.users.controller;

import java.util.Map;

import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public UserController(UserService userService, SessionAuthenticationService sessionAuthenticationService) {
        this.userService = userService;
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
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        User user = userService.login(request);
        sessionAuthenticationService.authenticate(user, servletRequest, servletResponse);
        return ResponseEntity.ok(buildUserResponse(user));
    }

    private Map<String, Object> buildUserResponse(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "active", user.isActive());
    }
}
