package es.uclm.esibuy.users.controller;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.security.service.EmailOtpService;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.security.service.TwoFactorAuthenticationService;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
                ApiExceptionHandler.class,
                SecurityConfig.class,
                SessionAuthenticationService.class
})
class UserControllerTest {

        private static final String SELLER_EMAIL = "seller@example.com";
        private static final String SELLER_ID = "seller-id";
        private static final String LOGIN_API = "/api/users/login";
        private static final String VERIFY_OTP_API = "/api/users/login/verify-otp";
        private static final String CURRENT_USER_API = "/api/users/me";
        private static final String EMAIL_OTP_SETTINGS_API = "/api/users/me/email-otp-settings";
        private static final String TEST_OTP = "123456";

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @MockitoBean
        private TwoFactorAuthenticationService twoFactorAuthenticationService;

        @MockitoBean
        private EmailOtpService emailOtpService;

        @MockitoSpyBean
        private SessionAuthenticationService sessionAuthenticationService;

        @Test
        void shouldCreateSessionOnLoginAndKeepItAcrossRequestsWithConfiguredTimeout()
                        throws Exception {

                User seller = activeSeller();

                when(userService.login(any(LoginRequest.class))).thenReturn(seller);
                when(userService.findById(SELLER_ID)).thenReturn(seller);
                doNothing().when(emailOtpService).verifyChallenge(SELLER_ID, TEST_OTP);

                MockHttpSession preLoginSession = new MockHttpSession();
                String oldSessionId = preLoginSession.getId();

                MvcResult loginResult = mockMvc.perform(
                                post(LOGIN_API)
                                                .with(csrf())
                                                .session(preLoginSession)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"email":"seller@example.com","password":"Password123!"}
                                                                """))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.requires3fa").value(true))
                                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                                .andReturn();

                MockHttpSession pendingSession = (MockHttpSession) loginResult.getRequest().getSession(false);

                assertNotNull(pendingSession);
                assertEquals(oldSessionId, pendingSession.getId());
                assertEquals(30 * 60, pendingSession.getMaxInactiveInterval());

                MvcResult verificationResult = mockMvc.perform(
                                post(VERIFY_OTP_API)
                                                .with(csrf())
                                                .session(pendingSession)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"code":"123456"}
                                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(SELLER_ID))
                                .andExpect(jsonPath("$.email").value(SELLER_EMAIL))
                                .andExpect(jsonPath("$.role").value("SELLER"))
                                .andReturn();

                MockHttpSession authenticatedSession = (MockHttpSession) verificationResult.getRequest()
                                .getSession(false);

                assertNotNull(authenticatedSession);
                assertNotEquals(oldSessionId, authenticatedSession.getId());
                assertEquals(30 * 60, authenticatedSession.getMaxInactiveInterval());

                mockMvc.perform(get(CURRENT_USER_API).session(authenticatedSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(SELLER_ID))
                                .andExpect(jsonPath("$.email").value(SELLER_EMAIL))
                                .andExpect(jsonPath("$.role").value("SELLER"));
        }

        @Test
        void shouldReturnUnauthorizedWithoutSession() throws Exception {
                mockMvc.perform(get(CURRENT_USER_API))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnForbiddenWhenAuthenticatedUserHasWrongRole() throws Exception {
                mockMvc.perform(get("/api/products/summary")
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRequireAdminRoleToCreateAnAdmin() throws Exception {
                mockMvc.perform(post("/api/users/admin")
                                .with(user("seller@example.com").roles("SELLER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldInvalidateSessionOnLogout() throws Exception {
                MockHttpSession authenticatedSession = new MockHttpSession();

                mockMvc.perform(post("/api/users/logout")
                                .session(authenticatedSession)
                                .with(user("seller@example.com").roles("SELLER"))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                assertTrue(authenticatedSession.isInvalid());

                mockMvc.perform(get(CURRENT_USER_API))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRegisterClient() throws Exception {
                User client = mock(User.class);
                when(client.getId()).thenReturn("client-id");
                when(client.getEmail()).thenReturn("ana@example.com");
                when(client.getRole()).thenReturn(UserRole.CLIENT);
                when(client.isActive()).thenReturn(true);

                when(userService.registerClient(any(ClientRegistrationRequest.class)))
                                .thenReturn(client);

                mockMvc.perform(post("/api/users/register/client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Ana","lastName":"García",
                                                 "email":"ana@example.com","nationalId":"12345678Z",
                                                 "birthDate":"2002-01-01","password":"Password123!",
                                                 "repeatedPassword":"Password123!","customerType":"PREMIUM"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("ana@example.com"))
                                .andExpect(jsonPath("$.role").value("CLIENT"));
        }

        @Test
        void shouldRegisterSeller() throws Exception {
                User seller = mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getEmail()).thenReturn("carlos@example.com");
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(true);

                when(userService.registerSeller(any(SellerRegistrationRequest.class)))
                                .thenReturn(seller);

                mockMvc.perform(post("/api/users/register/seller")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Carlos","lastName":"López",
                                                 "email":"carlos@example.com","nationalId":"87654321A",
                                                 "businessName":"Carlos Tech","defaultCategory":"Informatica",
                                                 "password":"Password123!","repeatedPassword":"Password123!"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("carlos@example.com"))
                                .andExpect(jsonPath("$.role").value("SELLER"));
        }

        @Test
        void shouldPreserveEmailValidationMessagesOnAdminAndRegistrationRequests()
                        throws Exception {

                mockMvc.perform(post("/api/users/admin")
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Admin","lastName":"Root",
                                                 "email":"invalid","password":"Admin123!Safe",
                                                 "repeatedPassword":"Admin123!Safe",
                                                 "headquarters":"Ciudad Real"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.email")
                                                .value("El formato de email no es válido (ejemplo: user@example.com)"));

                mockMvc.perform(post("/api/users/register/client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Ana","lastName":"García",
                                                 "email":"invalid","nationalId":"documento-1",
                                                 "birthDate":"2002-01-01","password":"Password123!",
                                                 "repeatedPassword":"Password123!","customerType":"PREMIUM"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.email")
                                                .value("El formato de email no es válido"));
        }

        @Test
        void shouldCreateAdmin() throws Exception {
                User admin = mock(User.class);
                when(admin.getId()).thenReturn("admin-id");
                when(admin.getEmail()).thenReturn("admin@esibuy.com");
                when(admin.getRole()).thenReturn(UserRole.ADMIN);
                when(admin.isActive()).thenReturn(true);

                when(userService.createAdmin(any(AdminCreateRequest.class))).thenReturn(admin);

                mockMvc.perform(post("/api/users/admin")
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Admin","lastName":"Root",
                                                 "email":"admin@esibuy.com","password":"Admin123!Safe",
                                                 "repeatedPassword":"Admin123!Safe",
                                                 "headquarters":"Ciudad Real"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("admin@esibuy.com"))
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        void shouldRegisterFailedLoginAttempt() throws Exception {
                doThrow(new InvalidCredentialsException())
                                .when(userService).login(any(LoginRequest.class));

                loginRequest()
                                .andExpect(status().isUnauthorized());

                verify(loginAttemptService).loginFailed(SELLER_EMAIL, "127.0.0.1");
        }

        @Test
        void shouldNotMarkSellerLoginAsSuccessfulBeforeOtpVerification() throws Exception {
                User seller = activeSeller();
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                loginRequest()
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.requires3fa").value(true));

                verify(loginAttemptService, never())
                                .loginSucceeded(SELLER_EMAIL, "127.0.0.1");
                verify(emailOtpService).issueChallenge(seller);
        }

        @Test
        void shouldReturnTooManyRequestsWhenLoginIsBlocked() throws Exception {
                when(loginAttemptService.isBlocked(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(true);
                when(loginAttemptService.getRemainingBlockSeconds(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(42L);

                loginRequest()
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "42"))
                                .andExpect(jsonPath("$.message")
                                                .value("Demasiados intentos fallidos. Inténtelo más tarde."));

                verify(userService, never()).login(any(LoginRequest.class));
        }

        @Test
        void shouldBlockTheSixthLoginAfterFiveInvalidCredentials() throws Exception {
                when(loginAttemptService.isBlocked(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(false, false, false, false, false, true);
                when(loginAttemptService.getRemainingBlockSeconds(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(30L);

                doThrow(new InvalidCredentialsException())
                                .when(userService).login(any(LoginRequest.class));

                for (int attempt = 0; attempt < 5; attempt++) {
                        loginRequest().andExpect(status().isUnauthorized());
                }

                loginRequest()
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "30"));

                verify(userService, times(5)).login(any(LoginRequest.class));
        }

        @Test
        void shouldEvaluateTheSeventhLoginAfterTheBlockExpires() throws Exception {
                when(loginAttemptService.isBlocked(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(true, false);
                when(loginAttemptService.getRemainingBlockSeconds(SELLER_EMAIL, "127.0.0.1"))
                                .thenReturn(30L);

                User seller = activeSeller();
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                loginRequest()
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "30"));

                loginRequest()
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.requires3fa").value(true));

                verify(userService).login(any(LoginRequest.class));
        }

        @Test
        void sellerLoginRequiresEmailOtp() throws Exception {
                User seller = activeSeller();
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                loginRequest()
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.requires3fa").value(true))
                                .andExpect(jsonPath("$.expiresInSeconds").value(600));

                verify(emailOtpService).issueChallenge(seller);
                verify(sessionAuthenticationService, never())
                                .authenticate(any(), any(), any());
        }

        @Test
        void clientLoginDoesNotRequireEmailOtp() throws Exception {
                User client = mock(User.class);
                when(client.getId()).thenReturn("client-id");
                when(client.getEmail()).thenReturn("client@example.com");
                when(client.getRole()).thenReturn(UserRole.CLIENT);
                when(client.isActive()).thenReturn(true);
                when(client.isTotpEnabled()).thenReturn(false);

                when(userService.login(any(LoginRequest.class))).thenReturn(client);

                mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"client@example.com","password":"Password123!"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("CLIENT"));

                verify(emailOtpService, never()).issueChallenge(any());
                verify(sessionAuthenticationService).authenticate(any(), any(), any());
        }

        @Test
        void emailOtpSettingsExposeWhetherTotpIsEnabled() throws Exception {
                User seller = activeSeller();
                when(userService.findById(SELLER_ID)).thenReturn(seller);

                mockMvc.perform(get(EMAIL_OTP_SETTINGS_API)
                                .with(sellerAuthentication()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.enabled").value(true))
                                .andExpect(jsonPath("$.totpEnabled").value(false))
                                .andExpect(jsonPath("$.email").value(SELLER_EMAIL));
        }

        @Test
        void shouldRejectEnablingEmailOtpWhenTotpIsDisabled() throws Exception {
                User seller = activeSeller();
                when(userService.findById(SELLER_ID)).thenReturn(seller);

                mockMvc.perform(put(EMAIL_OTP_SETTINGS_API)
                                .with(sellerAuthentication())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"enabled":true}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Activa primero la verificación en dos pasos con una aplicación Authenticator."));

                verify(userService, never()).save(any(User.class));
        }

        @Test
        void shouldAllowEnablingEmailOtpWhenTotpIsEnabled() throws Exception {
                User seller = activeSeller();
                when(seller.isTotpEnabled()).thenReturn(true);
                when(userService.findById(SELLER_ID)).thenReturn(seller);

                mockMvc.perform(put(EMAIL_OTP_SETTINGS_API)
                                .with(sellerAuthentication())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"enabled":true}
                                                """))
                                .andExpect(status().isOk());

                verify(userService).save(seller);
        }

        @Test
        void shouldRejectOtpVerificationWithoutPendingSession() throws Exception {
                mockMvc.perform(post(VERIFY_OTP_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"code":"123456"}
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectInvalidOtpAndKeepAuthenticationUnauthenticated() throws Exception {
                User seller = activeSeller();
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);
                when(userService.findById(SELLER_ID)).thenReturn(seller);

                doThrow(new es.uclm.esibuy.common.exception.InvalidOtpException())
                                .when(emailOtpService).verifyChallenge(SELLER_ID, "000000");

                MvcResult loginResult = loginRequest()
                                .andExpect(status().isAccepted())
                                .andReturn();

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
                assertNotNull(session);

                mockMvc.perform(post(VERIFY_OTP_API)
                                .with(csrf())
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"code":"000000"}
                                                """))
                                .andExpect(status().isUnauthorized());

                verify(sessionAuthenticationService, never())
                                .authenticate(any(), any(), any());
        }

        private User activeSeller() {
                User seller = mock(User.class);

                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getEmail()).thenReturn(SELLER_EMAIL);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(true);
                when(seller.isBlocked()).thenReturn(false);
                when(seller.isTotpEnabled()).thenReturn(false);
                when(seller.isEmailOtpEnabled()).thenReturn(true);

                return seller;
        }

        private RequestPostProcessor sellerAuthentication() {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                SELLER_ID, SELLER_EMAIL, UserRole.SELLER, true);

                return authentication(UsernamePasswordAuthenticationToken.authenticated(
                                principal,
                                null,
                                AuthorityUtils.createAuthorityList("ROLE_SELLER")));
        }

        private ResultActions loginRequest() throws Exception {
                return mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"Password123!"}
                                                """));
        }
}