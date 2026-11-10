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
import es.uclm.esibuy.security.service.TwoFactorAuthenticationService;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import es.uclm.esibuy.security.service.EmailOtpService;

@WebMvcTest(UserController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class, SessionAuthenticationService.class })
class UserProfileControllerTest {

        private static final String PROFILE_API = "/api/users/me/profile";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private LoginAttemptService loginAttemptService;

        @MockBean
        private TotpService totpService;

        @MockBean
        private EncryptionService encryptionService;

        @MockBean
        private TwoFactorAuthenticationService twoFactorAuthenticationService;

        @MockBean
        private EmailOtpService emailOtpService;

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
                                "avatar-client-default", CustomerType.PREMIUM, null, null, null, null, null, null));

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
                                "avatar-seller-default", null, null, "Tienda Luis", "Electrónica", null, null, null));

                mockMvc.perform(get(PROFILE_API).with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Luis"))
                                .andExpect(jsonPath("$.businessName").value("Tienda Luis"))
                                .andExpect(jsonPath("$.defaultCategory").value("Electrónica"));
        }

        @Test
        void shouldReturnOwnProfileForAdmin() throws Exception {
                when(userService.getOwnProfile("user-1")).thenReturn(new UserProfileResponse(
                                "Marta",
                                "López",
                                "marta@example.com",
                                "600333444",
                                null,
                                "avatar-admin-default",
                                null,
                                null,
                                null,
                                null,
                                "Madrid",
                                null, false));
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

        @Test
        void shouldUpdateOwnProfileSuccessfully() throws Exception {
                when(userService.updateOwnProfile(
                                org.mockito.ArgumentMatchers.eq("user-1"),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new UserProfileResponse(
                                                "Ana María", "García López", "ana@example.com",
                                                "611222333", "*****678Z", "avatar-client-default",
                                                CustomerType.PREMIUM, null, null, null, null, null, null));

                mockMvc.perform(put(PROFILE_API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "firstName": "Ana María",
                                                  "lastName": "García López",
                                                  "phone": "611222333"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Ana María"))
                                .andExpect(jsonPath("$.lastName").value("García López"))
                                .andExpect(jsonPath("$.phone").value("611222333"))
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());

                org.mockito.Mockito.verify(userService).updateOwnProfile(
                                org.mockito.ArgumentMatchers.eq("user-1"),
                                org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldRejectProfileUpdateWhenRequiredFieldsAreMissing() throws Exception {
                mockMvc.perform(put(PROFILE_API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "firstName": "",
                                                  "lastName": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.firstName").exists())
                                .andExpect(jsonPath("$.errors.lastName").exists());

                org.mockito.Mockito.verifyNoInteractions(userService);
        }

        @Test
        void shouldReturnUnauthorizedWhenUpdatingProfileWithoutSession() throws Exception {
                mockMvc.perform(put(PROFILE_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "firstName": "Ana",
                                                  "lastName": "García"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnNotFoundWhenUpdatingProfileOfMissingUser() throws Exception {
                when(userService.updateOwnProfile(
                                org.mockito.ArgumentMatchers.eq("user-1"),
                                org.mockito.ArgumentMatchers.any()))
                                .thenThrow(new UserNotFoundException("Usuario no encontrado"));

                mockMvc.perform(put(PROFILE_API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "firstName": "Ana",
                                                  "lastName": "García"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
        }

        @Test
        void shouldDeleteOwnAccountAndInvalidateSession() throws Exception {
                MockHttpSession session = new MockHttpSession();

                doNothing().when(userService).deleteOwnAccount("user-1");

                mockMvc.perform(delete("/api/users/me")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                org.mockito.Mockito.verify(userService).deleteOwnAccount("user-1");
                org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        }

        @Test
        void shouldReturnUnauthorizedWhenDeletingAccountWithoutSession() throws Exception {
                mockMvc.perform(delete("/api/users/me")
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());

                org.mockito.Mockito.verifyNoInteractions(userService);
        }

        @Test
        void shouldForbidAdminFromDeletingOwnAccount() throws Exception {
                MockHttpSession session = new MockHttpSession();

                mockMvc.perform(delete("/api/users/me")
                                .with(authentication(authenticationFor(UserRole.ADMIN)))
                                .session(session)
                                .with(csrf()))
                                .andExpect(status().isForbidden());

                org.mockito.Mockito.verifyNoInteractions(userService);
        }
}