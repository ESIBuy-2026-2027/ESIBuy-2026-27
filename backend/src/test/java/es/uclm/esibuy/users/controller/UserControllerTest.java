package es.uclm.esibuy.users.controller;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class, SessionAuthenticationService.class })
class UserControllerTest {

        private static final String SELLER_EMAIL = "seller@example.com";
        private static final String SELLER_ID = "seller-id";
        private static final String CLIENT_EMAIL = "client@example.com";
        private static final String LOGIN_API = "/api/users/login";
        private static final String CURRENT_USER_API = "/api/users/me";
        private static final String LOCALHOST = "127.0.0.1";
        private static final String RETRY_AFTER_HEADER = "Retry-After";
        private static final String LOGIN_PAYLOAD = """
                        {"email":"seller@example.com","password":"Password123!"}
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @Test
        void shouldCreateSessionOnLoginAndKeepItAcrossRequestsWithConfiguredTimeout() throws Exception {
                User seller = mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getEmail()).thenReturn(SELLER_EMAIL);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(false);
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                MockHttpSession preLoginSession = new MockHttpSession();
                String oldSessionId = preLoginSession.getId();
                MvcResult loginResult = mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .session(preLoginSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"Password123!"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("seller@example.com"))
                                .andExpect(jsonPath("$.role").value("SELLER"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
                assertNotNull(session);
                assertNotEquals(oldSessionId, session.getId());
                assertEquals(30 * 60, session.getMaxInactiveInterval());

                mockMvc.perform(get(CURRENT_USER_API).session(session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("seller-id"))
                                .andExpect(jsonPath("$.email").value("seller@example.com"))
                                .andExpect(jsonPath("$.role").value("SELLER"));
        }

        @Test
        void shouldReturnUnauthorizedWithoutSession() throws Exception {
                mockMvc.perform(get(CURRENT_USER_API))
                                .andExpect(status().isUnauthorized())
                                .andReturn();
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
                User seller = mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getEmail()).thenReturn(SELLER_EMAIL);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(false);
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"Password123!"}
                                                """))
                                .andExpect(status().isOk())
                                .andReturn();
                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
                assertNotNull(session);

                mockMvc.perform(post("/api/users/logout")
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                assertTrue(session.isInvalid());
                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRegisterClient() throws Exception {
                User client = mock(User.class);
                when(client.getId()).thenReturn("client-id");
                when(client.getEmail()).thenReturn("ana@example.com");
                when(client.getRole()).thenReturn(UserRole.CLIENT);
                when(client.isActive()).thenReturn(true);
                when(userService.registerClient(any(ClientRegistrationRequest.class))).thenReturn(client);

                mockMvc.perform(post("/api/users/register/client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Ana","lastName":"García","email":"ana@example.com","nationalId":"12345678Z","birthDate":"2002-01-01","password":"Password123!","repeatedPassword":"Password123!","customerType":"PREMIUM"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("ana@example.com"))
                                .andExpect(jsonPath("$.role").value("CLIENT"));
        }

        @Test
        void shouldRegisterSeller() throws Exception {
                User seller = mock(User.class);
                when(seller.getId()).thenReturn("seller-id");
                when(seller.getEmail()).thenReturn("carlos@example.com");
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(true);
                when(userService.registerSeller(any(SellerRegistrationRequest.class))).thenReturn(seller);

                mockMvc.perform(post("/api/users/register/seller")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Carlos","lastName":"López","email":"carlos@example.com","nationalId":"87654321A","businessName":"Carlos Tech","defaultCategory":"Informatica","password":"Password123!","repeatedPassword":"Password123!"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("carlos@example.com"))
                                .andExpect(jsonPath("$.role").value("SELLER"));
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
                                                {"firstName":"Admin","lastName":"Root","email":"admin@esibuy.com","password":"Admin123!Safe","repeatedPassword":"Admin123!Safe","headquarters":"Ciudad Real"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("admin@esibuy.com"))
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        void shouldRegisterFailedLoginAttempt() throws Exception {
                doThrow(new InvalidCredentialsException())
                                .when(userService).login(any(LoginRequest.class));

                mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"wrong-password"}
                                                """))
                                .andExpect(status().isUnauthorized());

                verify(loginAttemptService).loginFailed("seller@example.com", "127.0.0.1");
        }

        @Test
        void shouldResetLoginAttemptsAfterSuccessfulLogin() throws Exception {
                User seller = mock(User.class);
                when(seller.getId()).thenReturn("seller-id");
                when(seller.getEmail()).thenReturn("seller@example.com");
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(false);
                when(userService.login(any(LoginRequest.class))).thenReturn(seller);

                mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"Password123!"}
                                                """))
                                .andExpect(status().isOk());

                verify(loginAttemptService).loginSucceeded("seller@example.com", "127.0.0.1");
        }

        @Test
        void shouldReturnTooManyRequestsWhenLoginIsBlocked() throws Exception {
                when(loginAttemptService.isBlocked("seller@example.com", "127.0.0.1")).thenReturn(true);
                when(loginAttemptService.getRemainingBlockSeconds("seller@example.com", "127.0.0.1"))
                                .thenReturn(42L);

                mockMvc.perform(post(LOGIN_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"seller@example.com","password":"Password123!"}
                                                """))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "42"))
                                .andExpect(jsonPath("$.message")
                                                .value("Demasiados intentos fallidos. Inténtelo más tarde."));

                verify(userService, never()).login(any(LoginRequest.class));
        }

        @Test
        void shouldBlockTheSixthLoginAfterFiveInvalidCredentials() throws Exception {
                when(loginAttemptService.isBlocked("seller@example.com", "127.0.0.1"))
                                .thenReturn(false, false, false, false, false, true);
                when(loginAttemptService.getRemainingBlockSeconds("seller@example.com", "127.0.0.1"))
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
                when(loginAttemptService.isBlocked("seller@example.com", "127.0.0.1"))
                                .thenReturn(true, false);
                when(loginAttemptService.getRemainingBlockSeconds("seller@example.com", "127.0.0.1"))
                                .thenReturn(30L);

                User seller = mock(User.class);
                when(seller.getId()).thenReturn("seller-id");
                when(seller.getEmail()).thenReturn("seller@example.com");
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(seller.isActive()).thenReturn(false);
                AtomicInteger evaluatedAttempts = new AtomicInteger();
                when(userService.login(any(LoginRequest.class))).thenAnswer(invocation -> {
                        if (evaluatedAttempts.incrementAndGet() == 1) {
                                throw new InvalidCredentialsException();
                        }
                        return seller;
                });

                loginRequest()
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "30"));
                loginRequest()
                                .andExpect(status().isUnauthorized());

                verify(userService).login(any(LoginRequest.class));
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
