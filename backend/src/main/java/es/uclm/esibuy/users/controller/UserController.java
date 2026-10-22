package es.uclm.esibuy.users.controller;

import java.util.Map;

import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.TooManyRequestsException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.security.service.TwoFactorAuthenticationService;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateOwnProfileRequestDTO;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String MESSAGE_KEY = "message";

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;

    public UserController(
            UserService userService,
            LoginAttemptService loginAttemptService,
            SessionAuthenticationService sessionAuthenticationService,
            TwoFactorAuthenticationService twoFactorAuthenticationService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.twoFactorAuthenticationService = twoFactorAuthenticationService;
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

        // Si el usuario tiene activo el 2FA
        if (user.isTotpEnabled()) {
            String code = request.getTotpCode();
            if (code == null || code.isBlank()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                        "requires2fa", true,
                        "email", user.getEmail()));
            }

            if (!twoFactorAuthenticationService.verifyLoginCode(user, code)) {
                loginAttemptService.loginFailed(request.getEmail(), ipAddress);
                throw new InvalidCredentialsException();
            }
        }

        loginAttemptService.loginSucceeded(request.getEmail(), ipAddress);
        sessionAuthenticationService.authenticate(user, servletRequest, servletResponse);
        return ResponseEntity.ok(buildUserResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(buildUserResponse(user));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> ownProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getOwnProfile(principal.id()));
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteOwnAccount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.deleteOwnAccount(principal.id());

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateOwnProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UpdateOwnProfileRequestDTO request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileResponse profile = userService.updateOwnProfile(
                principal.id(), request);

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setup2FA(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String secret = twoFactorAuthenticationService.setup(principal.id());
        String qrUri = String.format("otpauth://totp/ESIBuy:%s?secret=%s&issuer=ESIBuy", principal.email(), secret);
        return ResponseEntity.ok(Map.of("secret", secret, "qrUri", qrUri));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Map<String, Object>> enable2FA(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TwoFactorAuthenticationService.EnableResult result =
                twoFactorAuthenticationService.enable(principal.id(), body != null ? body.get("code") : null);
        if (!result.enabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, (Object) "El código introducido es incorrecto o ha caducado"));
        }
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "2FA activado correctamente",
                "backupCodes", result.backupCodes()));
    }

    @PostMapping("/2fa/regenerate-backup-codes")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String code = body != null ? body.get("code") : null;
        TwoFactorAuthenticationService.BackupCodesResult result =
                twoFactorAuthenticationService.regenerateBackupCodes(principal.id(), code);
        if (result.status() == TwoFactorAuthenticationService.Status.TWO_FACTOR_DISABLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El usuario no tiene activado el 2FA"));
        }
        if (result.status() == TwoFactorAuthenticationService.Status.INVALID_CODE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "Código 2FA incorrecto"));
        }
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Nuevos códigos de respaldo generados correctamente",
                "backupCodes", result.backupCodes()));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, Object>> disable2FA(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String code = body != null ? body.get("code") : null;
        TwoFactorAuthenticationService.Status result = twoFactorAuthenticationService.disable(principal.id(), code);
        if (result == TwoFactorAuthenticationService.Status.TWO_FACTOR_DISABLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El 2FA no está activado"));
        }
        if (result == TwoFactorAuthenticationService.Status.INVALID_CODE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El código introducido es incorrecto"));
        }
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "2FA desactivado correctamente"));
    }

    private Map<String, Object> buildUserResponse(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "active", user.isActive(),
                "totpEnabled", user.isTotpEnabled());
    }
}