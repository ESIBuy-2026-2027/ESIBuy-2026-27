package es.uclm.esibuy.users.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.InvalidOtpException;
import es.uclm.esibuy.common.exception.TooManyRequestsException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.service.EmailOtpService;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.security.service.TwoFactorAuthenticationService;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.EmailOtpSettingsRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateOwnProfileRequestDTO;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.dto.VerifyEmailOtpDisableRequest;
import es.uclm.esibuy.users.dto.VerifyOtpRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String MESSAGE_KEY = "message";
    private static final String EMAIL_KEY = "email";
    private static final String ENABLED_KEY = "enabled";
    private static final String SOLO_ADMINS_SELLERS_MESSAGE = "Esta configuración solo está disponible para administradores y vendedores";

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final TwoFactorAuthenticationService twoFactorAuthenticationService;
    private static final String PENDING_OTP_USER_ID = "PENDING_OTP_USER_ID";
    private final EmailOtpService emailOtpService;

    public UserController(
            UserService userService,
            LoginAttemptService loginAttemptService,
            SessionAuthenticationService sessionAuthenticationService,
            TwoFactorAuthenticationService twoFactorAuthenticationService,
            EmailOtpService emailOtpService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.twoFactorAuthenticationService = twoFactorAuthenticationService;
        this.emailOtpService = emailOtpService;
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
                    loginAttemptService.getRemainingBlockSeconds(
                            request.getEmail(), ipAddress));
        }

        User user;

        try {
            user = userService.login(request);
        } catch (InvalidCredentialsException exception) {
            loginAttemptService.loginFailed(
                    request.getEmail(), ipAddress);
            throw exception;
        }

        if (user.isTotpEnabled()) {
            String code = request.getTotpCode();

            if (code == null || code.isBlank()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(Map.of(
                                "requires2fa", true,
                                EMAIL_KEY, user.getEmail()));
            }

            if (!twoFactorAuthenticationService
                    .verifyLoginCode(user, code)) {
                loginAttemptService.loginFailed(
                        request.getEmail(), ipAddress);
                throw new InvalidCredentialsException();
            }
        }

        if ((user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.SELLER)
                && user.isEmailOtpEnabled()) {

            emailOtpService.issueChallenge(user);

            HttpSession session = servletRequest.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute(PENDING_OTP_USER_ID, user.getId());

            SecurityContextHolder.clearContext();

            return ResponseEntity.accepted().body(
                    Map.of(
                            "requires3fa", true,
                            "expiresInSeconds", 600));
        }
        // CLIENT mantiene el comportamiento actual.
        loginAttemptService.loginSucceeded(
                request.getEmail(), ipAddress);

        sessionAuthenticationService.authenticate(
                user, servletRequest, servletResponse);

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

        TwoFactorAuthenticationService.EnableResult result = twoFactorAuthenticationService.enable(principal.id(),
                body != null ? body.get("code") : null);
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
        TwoFactorAuthenticationService.BackupCodesResult result = twoFactorAuthenticationService
                .regenerateBackupCodes(principal.id(), code);
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

    @PostMapping("/login/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        HttpSession session = servletRequest.getSession(false);

        if (session == null) {
            throw new InvalidOtpException();
        }

        Object pendingUserId = session.getAttribute(
                PENDING_OTP_USER_ID);

        if (!(pendingUserId instanceof String userId)) {
            throw new InvalidOtpException();
        }

        User user = userService.findById(userId);

        if (user == null
                || (user.getRole() != UserRole.ADMIN
                        && user.getRole() != UserRole.SELLER)) {
            session.removeAttribute(PENDING_OTP_USER_ID);
            throw new InvalidOtpException();
        }

        if (!user.isActive() || user.isBlocked()) {
            session.removeAttribute(PENDING_OTP_USER_ID);
            throw new InvalidCredentialsException();
        }
        emailOtpService.verifyChallenge(userId, request.code());

        // El desafío ya se ha consumido correctamente.
        session.removeAttribute(PENDING_OTP_USER_ID);

        loginAttemptService.loginSucceeded(
                user.getEmail(), servletRequest.getRemoteAddr());

        sessionAuthenticationService.authenticate(
                user, servletRequest, servletResponse);

        return ResponseEntity.ok(buildUserResponse(user));
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put(EMAIL_KEY, user.getEmail());
        response.put("role", user.getRole());
        response.put("active", user.isActive());
        response.put("totpEnabled", user.isTotpEnabled());
        if (user.getRole() == UserRole.CLIENT && user.getClientProfile() != null && user.getClientProfile().getCustomerType() != null) {
            response.put("customerType", user.getClientProfile().getCustomerType().name());
        }
        return response;
    }

    @GetMapping("/me/email-otp-settings")
    public ResponseEntity<Map<String, Object>> getEmailOtpSettings(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!supportsEmailOtp(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MESSAGE_KEY,
                            SOLO_ADMINS_SELLERS_MESSAGE));
        }

        return ResponseEntity.ok(Map.of(
                ENABLED_KEY, user.isEmailOtpEnabled(),
                "totpEnabled", user.isTotpEnabled(),
                EMAIL_KEY, user.getEmail()));
    }

    @PutMapping("/me/email-otp-settings")
    public ResponseEntity<Map<String, Object>> updateEmailOtpSettings(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody EmailOtpSettingsRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());

        if (!supportsEmailOtp(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MESSAGE_KEY,
                            SOLO_ADMINS_SELLERS_MESSAGE));
        }

        if (!request.enabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(MESSAGE_KEY,
                            "Para desactivar la verificación debes confirmar el código enviado a tu correo."));
        }

        if (!user.isTotpEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(MESSAGE_KEY,
                            "Activa primero la verificación en dos pasos con una aplicación Authenticator."));
        }

        user.setEmailOtpEnabled(true);
        userService.save(user);

        return ResponseEntity.ok(Map.of(
                ENABLED_KEY, user.isEmailOtpEnabled(),
                EMAIL_KEY, user.getEmail(),
                MESSAGE_KEY, "Verificación por correo activada correctamente."));
    }

    @PostMapping("/me/email-otp-settings/disable-challenge")
    public ResponseEntity<Map<String, Object>> requestEmailOtpDisableChallenge(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());

        if (!supportsEmailOtp(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MESSAGE_KEY,
                            SOLO_ADMINS_SELLERS_MESSAGE));
        }

        if (!user.isEmailOtpEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(MESSAGE_KEY,
                            "La verificación por correo ya está desactivada."));
        }

        emailOtpService.issueDisableChallenge(user);

        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY,
                "Hemos enviado un código de verificación a tu correo.",
                "expiresInSeconds", 600));
    }

    @PostMapping("/me/email-otp-settings/disable")
    public ResponseEntity<Map<String, Object>> confirmEmailOtpDisable(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody VerifyEmailOtpDisableRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(principal.id());

        if (!supportsEmailOtp(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MESSAGE_KEY,
                            SOLO_ADMINS_SELLERS_MESSAGE));
        }

        if (!user.isEmailOtpEnabled()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(MESSAGE_KEY,
                            "La verificación por correo ya está desactivada."));
        }

        emailOtpService.verifyDisableChallenge(user.getId(), request.code());

        user.setEmailOtpEnabled(false);
        userService.save(user);

        return ResponseEntity.ok(Map.of(
                ENABLED_KEY, false,
                EMAIL_KEY, user.getEmail(),
                MESSAGE_KEY,
                "Verificación por correo desactivada correctamente."));
    }

    private boolean supportsEmailOtp(User user) {
        return user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.SELLER;
    }
}