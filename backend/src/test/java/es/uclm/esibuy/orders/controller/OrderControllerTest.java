
package es.uclm.esibuy.orders.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.orders.dto.CheckoutPaymentMethodDTO;
import es.uclm.esibuy.orders.dto.CheckoutSummaryDTO;
import es.uclm.esibuy.orders.dto.CustomerOrderDTO;
import es.uclm.esibuy.orders.dto.OrderConfirmationDTO;
import es.uclm.esibuy.orders.dto.OrderStatusHistoryDTO;
import es.uclm.esibuy.orders.dto.ShippingTrackingDTO;
import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.service.OrderService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.model.PaymentMethodType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class OrderControllerTest {

        private static final String API = "/api/pedidos";
        private static final String CUSTOMER_ID = "cliente-1";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private OrderService orderService;

        private Authentication authenticationFor(UserRole role) {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                CUSTOMER_ID, "cliente@example.com", role, true);

                return new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        @Test
        void shouldCreateOrderForAuthenticatedClient() throws Exception {
                when(orderService.createOrder(eq(CUSTOMER_ID), any()))
                                .thenReturn(new OrderConfirmationDTO(
                                                "pedido-123",
                                                new BigDecimal("39.98"),
                                                OrderStatus.PENDIENTE,
                                                LocalDateTime.now(),
                                                OrderPaymentStatus.PAGADO));

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"direccionId":"direccion-1","metodoPagoId":"pago-1"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pedidoId").value("pedido-123"))
                                .andExpect(jsonPath("$.precioTotal").value(39.98))
                                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                                .andExpect(jsonPath("$.estadoPago").value("PAGADO"));

                verify(orderService).createOrder(eq(CUSTOMER_ID), any());
        }

        @Test
        void shouldRejectNonClientAndAvoidCallingService() throws Exception {
                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"direccionId":"direccion-1","metodoPagoId":"pago-1"}
                                                """))
                                .andExpect(status().isForbidden());

                verify(orderService, never()).createOrder(any(), any());
        }

        @Test
        void shouldValidateRequiredCheckoutIdentifiers() throws Exception {
                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"direccionId":" ","metodoPagoId":""}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.shippingAddressId").isNotEmpty())
                                .andExpect(jsonPath("$.errors.paymentMethodId").isNotEmpty());

                verify(orderService, never()).createOrder(any(), any());
        }

        @Test
        void shouldReturnCheckoutSummaryForAuthenticatedClient() throws Exception {
                when(orderService.getCheckoutSummary(CUSTOMER_ID)).thenReturn(
                                new CheckoutSummaryDTO(
                                                List.of(),
                                                List.of(new CheckoutPaymentMethodDTO(
                                                                "pago-1",
                                                                PaymentMethodType.CARD,
                                                                "VISA",
                                                                "1234",
                                                                true)),
                                                List.of(),
                                                BigDecimal.ZERO));

                mockMvc.perform(get(API + "/checkout-resumen")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.direcciones").isArray())
                                .andExpect(jsonPath("$.metodosPago").isArray())
                                .andExpect(jsonPath("$.metodosPago[0].lastFourDigits").value("1234"))
                                .andExpect(jsonPath("$.metodosPago[0].paypalEmail").doesNotExist())
                                .andExpect(jsonPath("$.items").isArray())
                                .andExpect(jsonPath("$.precioTotal").value(0));

                verify(orderService).getCheckoutSummary(CUSTOMER_ID);
        }

        @Test
        void shouldReturnSellerOrdersForAuthenticatedSeller() throws Exception {
                when(orderService.getSellerOrders(CUSTOMER_ID)).thenReturn(List.of());

                mockMvc.perform(get(API + "/vendedor")
                                .with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());

                verify(orderService).getSellerOrders(CUSTOMER_ID);
        }

        @Test
        void shouldRejectSellerOrdersForAuthenticatedClient() throws Exception {
                mockMvc.perform(get(API + "/vendedor")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isForbidden());

                verify(orderService, never()).getSellerOrders(any());
        }

        @Test
        void shouldReturnOnlyAuthenticatedClientOrderHistory() throws Exception {
                when(orderService.getCustomerOrders(CUSTOMER_ID)).thenReturn(List.of());

                mockMvc.perform(get(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());

                verify(orderService).getCustomerOrders(CUSTOMER_ID);
        }

        @Test
        void shouldRejectClientOrderHistoryForSeller() throws Exception {
                mockMvc.perform(get(API)
                                .with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isForbidden());

                verify(orderService, never()).getCustomerOrders(any());
        }

        @Test
        void shouldReturnOrderDetailForAuthenticatedClient() throws Exception {
                when(orderService.getCustomerOrder(CUSTOMER_ID, "pedido-123"))
                                .thenReturn(new CustomerOrderDTO(
                                                "pedido-123",
                                                LocalDateTime.now(),
                                                List.of(),
                                                new BigDecimal("39.98"),
                                                OrderStatus.PENDIENTE,
                                                null,
                                                null,
                                                OrderPaymentStatus.PAGADO));

                mockMvc.perform(get(API + "/pedido-123")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pedidoId").value("pedido-123"))
                                .andExpect(jsonPath("$.total").value(39.98))
                                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                                .andExpect(jsonPath("$.estadoPago").value("PAGADO"));

                verify(orderService).getCustomerOrder(CUSTOMER_ID, "pedido-123");
        }

        @Test
        void shouldReturnTrackingHistoryForAuthenticatedClient() throws Exception {
                when(orderService.getShippingStatusHistory(CUSTOMER_ID, "pedido-123"))
                                .thenReturn(new ShippingTrackingDTO(
                                                List.of(new OrderStatusHistoryDTO(
                                                                OrderStatus.ENVIADO,
                                                                LocalDateTime.of(2026, 10, 28, 12, 0))),
                                                null));

                mockMvc.perform(get(API + "/pedido-123/historial-estados")
                                .with(authentication(authenticationFor(UserRole.CLIENT))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.historialEstados[0].estado").value("ENVIADO"))
                                .andExpect(jsonPath("$.historialEstados[0].fecha").value("2026-10-28T12:00:00"))
                                .andExpect(jsonPath("$.estimatedDeliveryDate").doesNotExist());

                verify(orderService).getShippingStatusHistory(CUSTOMER_ID, "pedido-123");
        }

        @Test
        void shouldRejectTrackingHistoryForAnotherRole() throws Exception {
                mockMvc.perform(get(API + "/pedido-123/historial-estados")
                                .with(authentication(authenticationFor(UserRole.SELLER))))
                                .andExpect(status().isForbidden());

                verify(orderService, never()).getShippingStatusHistory(any(), any());
        }

        @Test
        void shouldUpdateSellerOrderStatusForAuthenticatedSeller() throws Exception {
                mockMvc.perform(patch(API + "/vendedor/pedido-123/estado")
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"estado":"ENVIADO"}
                                                """))
                                .andExpect(status().isNoContent());

                verify(orderService).updateOrderStatus(
                                CUSTOMER_ID, "pedido-123", OrderStatus.ENVIADO, null);
        }

        @Test
        void shouldRejectOrderStatusUpdateForAuthenticatedClient() throws Exception {
                mockMvc.perform(patch(API + "/vendedor/pedido-123/estado")
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"estado":"ENVIADO"}
                                                """))
                                .andExpect(status().isForbidden());

                verify(orderService, never()).updateOrderStatus(any(), any(), any(), any());
        }

        @Test
        void shouldValidateRequiredOrderStatus() throws Exception {
                mockMvc.perform(patch(API + "/vendedor/pedido-123/estado")
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"estado":null}
                                                """))
                                .andExpect(status().isBadRequest());

                verify(orderService, never()).updateOrderStatus(any(), any(), any(), any());
        }

        @Test
        void shouldReturnBadRequestWhenOrderDoesNotBelongToSeller() throws Exception {
                doThrow(new IllegalArgumentException("El pedido no pertenece al vendedor"))
                                .when(orderService)
                                .updateOrderStatus(
                                                CUSTOMER_ID, "pedido-123", OrderStatus.ENVIADO, null);

                mockMvc.perform(patch(API + "/vendedor/pedido-123/estado")
                                .with(authentication(authenticationFor(UserRole.SELLER)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"estado":"ENVIADO"}
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnPaymentRequiredWithoutExposingInternalDetails() throws Exception {
                doThrow(new PagoFallidoException())
                                .when(orderService)
                                .createOrder(eq(CUSTOMER_ID), any());

                mockMvc.perform(post(API)
                                .with(authentication(authenticationFor(UserRole.CLIENT)))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"direccionId":"direccion-1","metodoPagoId":"pago-1"}
                                                """))
                                .andExpect(status().isPaymentRequired())
                                .andExpect(jsonPath("$.message").value(
                                                "El pago fue rechazado. No se ha realizado ningún cargo ni modificado tu carrito"))
                                .andExpect(jsonPath("$.stackTrace").doesNotExist());
        }
}
