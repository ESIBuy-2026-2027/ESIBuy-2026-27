
package es.uclm.esibuy.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.dto.PaymentMethodRequest;
import es.uclm.esibuy.users.dto.PaymentMethodResponse;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.PaymentMethodService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(PaymentMethodController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class PaymentMethodControllerTest {

        private static final String API = "/api/users/me/payment-methods";

        private static final String VALID_BODY = """
                        {
                          "type": "CARD",
                          "label": "Visa",
                          "cardholderName": "Ana García",
                          "cardBrand": "Visa",
                          "lastFourDigits": "1234",
                          "expirationMonth": 12,
                          "expirationYear": 2099,
                          "isDefault": true
                        }
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PaymentMethodService paymentMethodService;

        private Authentication authenticationFor(UserRole role) {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                "user-1", "ana@example.com", role, true);

                return new UsernamePasswordAuthenticationToken(
                                principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        private PaymentMethodResponse stored() {
                return PaymentMethodResponse.from(PaymentMethod.card(
                                "p1", "Visa", "Ana García", "Visa",
                                "1234", 12, 2099, true));
        }

        @Test
        void shouldListPaymentMethods() throws Exception {
                when(paymentMethodService.list("user-1")).thenReturn(List.of(stored()));

                mockMvc.perform(get(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("p1"))
                                .andExpect(jsonPath("$[0].type").value("CARD"))
                                .andExpect(jsonPath("$[0].isDefault").value(true));
        }

        @Test
        void shouldAddPaymentMethod() throws Exception {
                when(paymentMethodService.add(
                                eq("user-1"), any(PaymentMethodRequest.class)))
                                .thenReturn(stored());

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("p1"))
                                .andExpect(jsonPath("$.type").value("CARD"));
        }

        @Test
        void shouldRejectInvalidPaymentMethodData() throws Exception {
                String invalid = VALID_BODY.replace("\"1234\"", "\"12\"");

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                                .andExpect(status().isBadRequest());

                verify(paymentMethodService, never()).add(any(), any());
        }

        @Test
        void shouldSetDefaultPaymentMethod() throws Exception {
                when(paymentMethodService.setDefault("user-1", "p1"))
                                .thenReturn(stored());

                mockMvc.perform(put(API + "/p1/default")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("p1"))
                                .andExpect(jsonPath("$.isDefault").value(true));
        }

        @Test
        void shouldUpdatePaymentMethod() throws Exception {
                when(paymentMethodService.update(
                                eq("user-1"), eq("p1"), any(PaymentMethodRequest.class)))
                                .thenReturn(stored());

                mockMvc.perform(put(API + "/p1")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("p1"));
        }

        @Test
        void shouldDeletePaymentMethod() throws Exception {
                mockMvc.perform(delete(API + "/p1")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(paymentMethodService).delete("user-1", "p1");
        }

        @Test
        void shouldRejectRequestsWithoutAuthentication() throws Exception {
                mockMvc.perform(get(API)).andExpect(status().isUnauthorized());

                mockMvc.perform(post(API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(put(API + "/p1/default")
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(put(API + "/p1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(delete(API + "/p1")
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldForbidSellersAndAdmins() throws Exception {
                for (UserRole role : List.of(UserRole.SELLER, UserRole.ADMIN)) {
                        RequestPostProcessor auth = authentication(authenticationFor(role));
                        mockMvc.perform(get(API).with(auth))
                                        .andExpect(status().isForbidden());

                        mockMvc.perform(post(API)
                                        .with(auth)
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(VALID_BODY))
                                        .andExpect(status().isForbidden());

                        mockMvc.perform(put(API + "/p1/default")
                                        .with(authentication(authenticationFor(role)))
                                        .with(csrf()))
                                        .andExpect(status().isForbidden());

                        mockMvc.perform(put(API + "/p1")
                                        .with(authentication(authenticationFor(role)))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(VALID_BODY))
                                        .andExpect(status().isForbidden());

                        mockMvc.perform(delete(API + "/p1")
                                        .with(authentication(authenticationFor(role)))
                                        .with(csrf()))
                                        .andExpect(status().isForbidden());
                }

                verify(paymentMethodService, never()).add(any(), any());
                verify(paymentMethodService, never()).delete(any(), any());
        }

        @Test
        void shouldRejectStateChangingRequestsWithoutCsrf() throws Exception {
                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isForbidden());

                mockMvc.perform(put(API + "/p1/default")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isForbidden());

                mockMvc.perform(delete(API + "/p1")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isForbidden());
        }
}
