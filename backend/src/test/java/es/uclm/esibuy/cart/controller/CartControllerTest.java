package es.uclm.esibuy.cart.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.cart.dto.CartDTO;
import es.uclm.esibuy.cart.dto.CartItemDTO;
import es.uclm.esibuy.cart.service.CartService;
import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

@WebMvcTest(CartController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class CartControllerTest {

        private static final String API = "/api/carrito";
        private static final String CUSTOMER_ID = "cliente-1";
        private static final String PRODUCT_ID = "producto-1";
        private static final BigDecimal PRICE = new BigDecimal("19.99");
        private static final String VALID_BODY = """
                        {"productoId":"producto-1","cantidad":2}
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CartService cartService;

        private Authentication authenticationFor(UserRole role) {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                CUSTOMER_ID, "cliente@example.com", role, true);

                return new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        private CartDTO cart() {
                return new CartDTO(
                                "carrito-1",
                                CUSTOMER_ID,
                                List.of(new CartItemDTO(
                                                PRODUCT_ID,
                                                "Auriculares",
                                                PRICE,
                                                2,
                                                new BigDecimal("39.98"))),
                                new BigDecimal("39.98"));
        }

        @Test
        void shouldAddAnItemUsingTheAuthenticatedClientId() throws Exception {
                when(cartService.addItem(eq(CUSTOMER_ID), any())).thenReturn(cart());

                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.clienteId").value(CUSTOMER_ID))
                                .andExpect(jsonPath("$.items[0].productoId").value(PRODUCT_ID))
                                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                                .andExpect(jsonPath("$.items[0].subtotal").value(39.98))
                                .andExpect(jsonPath("$.precioTotal").value(39.98));

                verify(cartService).addItem(eq(CUSTOMER_ID), any());
        }

        @Test
        void shouldReturnTheAuthenticatedClientCart() throws Exception {
                when(cartService.getCart(CUSTOMER_ID)).thenReturn(cart());

                mockMvc.perform(get(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("carrito-1"))
                                .andExpect(jsonPath("$.clienteId").value(CUSTOMER_ID))
                                .andExpect(jsonPath("$.precioTotal").value(39.98));

                verify(cartService).getCart(CUSTOMER_ID);
        }

        @Test
        void shouldRejectInvalidQuantityWithoutCallingService() throws Exception {
                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"productoId":"producto-1","cantidad":0}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.quantity").isNotEmpty());

                verify(cartService, never()).addItem(any(), any());
        }

        @Test
        void shouldReturnBadRequestWhenStockIsInsufficient() throws Exception {
                when(cartService.addItem(eq(CUSTOMER_ID), any()))
                                .thenThrow(new StockInsuficienteException());

                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("No hay stock suficiente para la cantidad solicitada"));
        }

        @Test
        void shouldUpdateItemQuantityForTheAuthenticatedClient() throws Exception {
                when(cartService.updateItemQuantity(eq(CUSTOMER_ID), eq(PRODUCT_ID), any()))
                                .thenReturn(new CartDTO(
                                                "carrito-1",
                                                CUSTOMER_ID,
                                                List.of(new CartItemDTO(
                                                                PRODUCT_ID,
                                                                "Auriculares",
                                                                PRICE,
                                                                3,
                                                                new BigDecimal("59.97"))),
                                                new BigDecimal("59.97")));

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":3}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.clienteId").value(CUSTOMER_ID))
                                .andExpect(jsonPath("$.items[0].cantidad").value(3))
                                .andExpect(jsonPath("$.precioTotal").value(59.97));

                verify(cartService).updateItemQuantity(
                                eq(CUSTOMER_ID),
                                eq(PRODUCT_ID),
                                argThat(request -> request.quantity() == 3));
        }

        @Test
        void shouldAllowZeroQuantityAndRejectNegativeQuantity() throws Exception {
                when(cartService.updateItemQuantity(eq(CUSTOMER_ID), eq(PRODUCT_ID), any()))
                                .thenReturn(new CartDTO(
                                                "carrito-1",
                                                CUSTOMER_ID,
                                                List.of(),
                                                BigDecimal.ZERO));

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":0}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items").isEmpty())
                                .andExpect(jsonPath("$.precioTotal").value(0));

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":-1}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.quantity").isNotEmpty());

                verify(cartService, never()).updateItemQuantity(
                                eq(CUSTOMER_ID),
                                eq(PRODUCT_ID),
                                argThat(request -> request != null && request.quantity() < 0));
        }

        @Test
        void shouldReturnBadRequestWhenUpdatedQuantityExceedsStock() throws Exception {
                when(cartService.updateItemQuantity(eq(CUSTOMER_ID), eq(PRODUCT_ID), any()))
                                .thenThrow(new StockInsuficienteException());

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":5}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("No hay stock suficiente para la cantidad solicitada"));
        }

        @Test
        void shouldDeleteItemAndReturnUpdatedCart() throws Exception {
                when(cartService.removeItem(CUSTOMER_ID, PRODUCT_ID))
                                .thenReturn(new CartDTO(
                                                "carrito-1",
                                                CUSTOMER_ID,
                                                List.of(),
                                                BigDecimal.ZERO));

                mockMvc.perform(delete(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items").isEmpty())
                                .andExpect(jsonPath("$.precioTotal").value(0));

                verify(cartService).removeItem(CUSTOMER_ID, PRODUCT_ID);
        }

        @Test
        void shouldReturnNotFoundWhenDeletingItemNotInCart() throws Exception {
                when(cartService.removeItem(CUSTOMER_ID, PRODUCT_ID))
                                .thenThrow(new ProductNotFoundException());

                mockMvc.perform(delete(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnConflictForInactiveProduct() throws Exception {
                when(cartService.addItem(eq(CUSTOMER_ID), any()))
                                .thenThrow(new ProductoNoDisponibleException());

                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("El producto no está disponible"));
        }

        @Test
        void shouldReturnBadRequestForSemanticallyInvalidRequest() throws Exception {
                when(cartService.addItem(eq(CUSTOMER_ID), any()))
                                .thenThrow(new IllegalArgumentException("La cantidad debe ser como mínimo 1"));

                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La cantidad debe ser como mínimo 1"));
        }

        @Test
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
                when(cartService.addItem(eq(CUSTOMER_ID), any()))
                                .thenThrow(new ProductNotFoundException());

                mockMvc.perform(post(API + "/items")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticatedAndNonClientUsers() throws Exception {
                mockMvc.perform(get(API))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":1}
                                                """))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(delete(API + "/items/" + PRODUCT_ID)
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get(API)
                                .with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isForbidden());

                mockMvc.perform(put(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"cantidad":1}
                                                """))
                                .andExpect(status().isForbidden());

                mockMvc.perform(delete(API + "/items/" + PRODUCT_ID)
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }
}