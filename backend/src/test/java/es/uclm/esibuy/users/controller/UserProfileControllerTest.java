package es.uclm.esibuy.users.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.security.service.EncryptionService;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.security.service.TotpService;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class, SessionAuthenticationService.class })
class UserProfileControllerTest {

        private static final String PROFILE_API = "/api/users/me/profile";

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @MockitoBean
        private TotpService totpService;

        @MockitoBean
        private EncryptionService encryptionService;

        private Authentication authenticationFor(UserRole role) {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                "user-1", "ana@example.com", role, true);
                return new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        @Test
        void shouldReturnOwnProfileForClient() throws Exception {
                when(userService.getOwnProfile("user-1")).thenReturn(new UserProfileResponse(
                                "Ana", "García", "ana@example.com", "600123123", "*****678Z",
                                "avatar-client-default", CustomerType.PREMIUM, null, null, null, null));

                mockMvc.perform(get(PROFILE_API).with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Ana"))
                                .andExpect(jsonPath("$.maskedNationalId").value("*****678Z"))
                                .andExpect(jsonPath("$.customerType").value("PREMIUM"))
                                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                                .andExpect(jsonPath("$.totpSecret").doesNotExist())
                                .andExpect(jsonPath("$.nationalId").doesNotExist());
        }

        @Test
        void shouldReturnUnauthorizedWithoutSession() throws Exception {
                mockMvc.perform(get(PROFILE_API))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnOwnProfileForSeller() throws Exception {
                when(userService.getOwnProfile("user-1")).thenReturn(new UserProfileResponse(
                                "Luis", "Martín", "luis@example.com", "600111222", null,
                                "avatar-seller-default", null, "Tienda Luis", "Electrónica", null, null));

                mockMvc.perform(get(PROFILE_API).with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Luis"))
                                .andExpect(jsonPath("$.businessName").value("Tienda Luis"))
                                .andExpect(jsonPath("$.defaultCategory").value("Electrónica"));
        }

        @Test
        void shouldReturnOwnProfileForAdmin() throws Exception {
                when(userService.getOwnProfile("user-1")).thenReturn(new UserProfileResponse(
                                "Marta", "López", "marta@example.com", "600333444", null,
                                "avatar-admin-default", null, null, null, "Madrid", null));

                mockMvc.perform(get(PROFILE_API).with(authentication(authenticationFor(UserRole.ADMIN))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Marta"))
                                .andExpect(jsonPath("$.headquarters").value("Madrid"));
        }

        @Test
        void shouldReturnNotFoundWhenSessionUserNoLongerExists() throws Exception {
                when(userService.getOwnProfile("user-1")).thenThrow(new UserNotFoundException("Usuario no encontrado"));

                mockMvc.perform(get(PROFILE_API).with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
        }
}