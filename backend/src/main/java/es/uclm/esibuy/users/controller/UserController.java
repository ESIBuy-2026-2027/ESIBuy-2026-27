package es.uclm.esibuy.users.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.TooManyRequestsException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.service.EncryptionService;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.security.service.TotpService;
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
import org.springframework.security.core.Authentication;
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
    private final TotpService totpService;
    private final EncryptionService encryptionService;

    public UserController(
            UserService userService,
            LoginAttemptService loginAttemptService,
            SessionAuthenticationService sessionAuthenticationService,
            TotpService totpService,
            EncryptionService encryptionService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
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

            // DESCIFRAR el secreto cifrado con AES antes de verificar el código TOTP
            String rawSecret = encryptionService.decrypt(user.getTotpSecret());
            boolean isValidTotp = totpService.verifyCode(rawSecret, code);
            boolean isValidBackup = false;

            if (!isValidTotp && user.getBackupCodes() != null) {
                isValidBackup = user.getBackupCodes().contains(code.trim());
                if (isValidBackup) {
                    // Consumir el código de respaldo usado (un solo uso)
                    user.getBackupCodes().remove(code.trim());
                    userService.save(user);
                }
            }

            if (!isValidTotp && !isValidBackup) {
                loginAttemptService.loginFailed(request.getEmail(), ipAddress);
                throw new InvalidCredentialsException();
            }
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

        User user = userService.findById(principal.id());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(buildUserResponse(user));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> ownProfile(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
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
            Authentication authentication,
            HttpServletRequest request) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
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
            Authentication authentication,
            @Valid @RequestBody UpdateOwnProfileRequestDTO request) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileResponse profile = userService.updateOwnProfile(
                principal.id(), request);

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setup2FA(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());
        String secret = totpService.generateSecret();

        String encryptedSecret = encryptionService.encrypt(secret);
        user.setTotpSecret(encryptedSecret);
        userService.save(user);

        String qrUri = String.format("otpauth://totp/ESIBuy:%s?secret=%s&issuer=ESIBuy", user.getEmail(), secret);
        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "qrUri", qrUri));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Map<String, Object>> enable2FA(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String code = body.get("code");
        User user = userService.findById(principal.id());
        String rawSecret = encryptionService.decrypt(user.getTotpSecret());

        if (!totpService.verifyCode(rawSecret, code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, (Object) "El código introducido es incorrecto o ha caducado"));
        }

        List<String> rawBackupCodes = totpService.generateBackupCodes(8);
        user.setTotpEnabled(true);
        user.setBackupCodes(new ArrayList<>(rawBackupCodes));
        userService.save(user);

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "2FA activado correctamente",
                "backupCodes", rawBackupCodes));
    }

    @PostMapping("/2fa/regenerate-backup-codes")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());
        if (!user.isTotpEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El usuario no tiene activado el 2FA"));
        }

        String code = body != null ? body.get("code") : null;
        if (code != null && !code.isBlank()) {
            String rawSecret = encryptionService.decrypt(user.getTotpSecret());
            if (!totpService.verifyCode(rawSecret, code)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(MESSAGE_KEY, "Código 2FA incorrecto"));
            }
        }

        List<String> newBackupCodes = totpService.generateBackupCodes(8);
        user.setBackupCodes(new ArrayList<>(newBackupCodes));
        userService.save(user);

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Nuevos códigos de respaldo generados correctamente",
                "backupCodes", newBackupCodes));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, Object>> disable2FA(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String code = body != null ? body.get("code") : null;
        User user = userService.findById(principal.id());

        if (!user.isTotpEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El 2FA no está activado"));
        }

        String rawSecret = encryptionService.decrypt(user.getTotpSecret());
        if (code == null || !totpService.verifyCode(rawSecret, code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MESSAGE_KEY, "El código introducido es incorrecto"));
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.getBackupCodes().clear();
        userService.save(user);

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