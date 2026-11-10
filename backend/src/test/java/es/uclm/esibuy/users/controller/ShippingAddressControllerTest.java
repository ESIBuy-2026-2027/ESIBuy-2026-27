package es.uclm.esibuy.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import es.uclm.esibuy.common.exception.AddressLimitExceededException;
import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.dto.ShippingAddressRequest;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.ShippingAddressService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import es.uclm.esibuy.common.exception.AddressNotFoundException;

@WebMvcTest(ShippingAddressController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class ShippingAddressControllerTest {

        private static final String API = "/api/users/me/addresses";

        private static final String VALID_BODY = """
                        {"recipientName":"Ana García","street":"Calle Mayor, 12","additionalInfo":"2º B",
                         "postalCode":"13071","city":"Ciudad Real","province":"Ciudad Real",
                         "phone":"600123123","isDefault":true}
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ShippingAddressService shippingAddressService;

        private Authentication authenticationFor(UserRole role) {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                "user-1", "ana@example.com", role, true);
                return new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        private ShippingAddress stored() {
                return new ShippingAddress("a1", "Ana García", "Calle Mayor, 12", "2º B", "13071",
                                "Ciudad Real", "Ciudad Real", "600123123", true);
        }

        @Test
        void shouldCreateAnAddressForClients() throws Exception {
                when(shippingAddressService.add(eq("user-1"), any(ShippingAddressRequest.class))).thenReturn(stored());

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("a1"))
                                .andExpect(jsonPath("$.isDefault").value(true))
                                .andExpect(jsonPath("$.postalCode").value("13071"));
        }

        @Test
        void shouldListTheAddressesOfTheClient() throws Exception {
                when(shippingAddressService.list("user-1")).thenReturn(List.of(stored()));

                mockMvc.perform(get(API).with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].street").value("Calle Mayor, 12"))
                                .andExpect(jsonPath("$[0].isDefault").value(true));
        }

        @Test
        void shouldReturnValidationErrorsWhenTheFormatIsInvalid() throws Exception {
                String invalid = VALID_BODY.replace("13071", "1307").replace("600123123", "123");

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                                .andExpect(status().isBadRequest());

                verify(shippingAddressService, never()).add(any(), any());
        }

        @Test
        void shouldRejectMissingRequiredFields() throws Exception {
                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectRequestsWithoutSession() throws Exception {
                mockMvc.perform(post(API).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(get(API)).andExpect(status().isUnauthorized());
        }

        @Test
        void shouldForbidSellersAndAdmins() throws Exception {
                for (UserRole role : List.of(UserRole.SELLER, UserRole.ADMIN)) {
                        mockMvc.perform(post(API)
                                        .with(authentication(authenticationFor(role)))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(VALID_BODY))
                                        .andExpect(status().isForbidden());
                        mockMvc.perform(get(API).with(authentication(authenticationFor(role))))
                                        .andExpect(status().isForbidden());
                }
                verify(shippingAddressService, never()).add(any(), any());
        }

        @Test
        void shouldRejectRequestsWithoutCsrfToken() throws Exception {
                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnConflictWhenTheLimitIsReached() throws Exception {
                when(shippingAddressService.add(eq("user-1"), any(ShippingAddressRequest.class)))
                                .thenThrow(new AddressLimitExceededException(
                                                "Has alcanzado el máximo de 10 direcciones de envío"));

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message")
                                                .value("Has alcanzado el máximo de 10 direcciones de envío"));
        }

        @Test
        void shouldUpdateAnAddressSuccessfully() throws Exception {
                when(shippingAddressService.update(
                                eq("user-1"),
                                eq("a1"),
                                any(ShippingAddressRequest.class)))
                                .thenReturn(es.uclm.esibuy.users.dto.ShippingAddressResponse.from(stored()));

                mockMvc.perform(put(API + "/a1")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("a1"))
                                .andExpect(jsonPath("$.street").value("Calle Mayor, 12"));
        }

        @Test
        void shouldRejectAddressUpdateWithInvalidData() throws Exception {
                String invalid = VALID_BODY.replace("13071", "1307");

                mockMvc.perform(put(API + "/a1")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                                .andExpect(status().isBadRequest());

                verify(shippingAddressService, never()).update(any(), any(), any());
        }

        @Test
        void shouldDeleteAnAddressSuccessfully() throws Exception {
                mockMvc.perform(delete(API + "/a1")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(shippingAddressService).delete("user-1", "a1");
        }

        @Test
        void shouldReturnNotFoundWhenDeletingAnUnknownAddress() throws Exception {
                org.mockito.Mockito.doThrow(new AddressNotFoundException())
                                .when(shippingAddressService).delete("user-1", "missing");

                mockMvc.perform(delete(API + "/missing")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectAddressUpdateWithoutSession() throws Exception {
                mockMvc.perform(put(API + "/a1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectAddressDeletionWithoutSession() throws Exception {
                mockMvc.perform(delete(API + "/a1")
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());
        }
}
