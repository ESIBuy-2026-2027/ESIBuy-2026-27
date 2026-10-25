package es.uclm.esibuy.pedidos.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.dto.MetodoPagoCheckoutDTO;
import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.service.PedidoService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.model.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest(PedidoController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class PedidoControllerTest {

    private static final String API = "/api/pedidos";
    private static final String CLIENTE_ID = "cliente-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    private Authentication authenticationFor(UserRole role) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                CLIENTE_ID, "cliente@example.com", role, true);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void shouldCreateOrderForAuthenticatedClient() throws Exception {
        when(pedidoService.crearPedido(eq(CLIENTE_ID), any()))
                .thenReturn(new PedidoConfirmadoDTO(
                        "pedido-123", new BigDecimal("39.98"), PedidoEstado.PAGADO, LocalDateTime.now()));

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
                .andExpect(jsonPath("$.estado").value("PAGADO"));

        verify(pedidoService).crearPedido(eq(CLIENTE_ID), any());
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

        verify(pedidoService, never()).crearPedido(any(), any());
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
                .andExpect(jsonPath("$.errors.direccionId").isNotEmpty())
                .andExpect(jsonPath("$.errors.metodoPagoId").isNotEmpty());

        verify(pedidoService, never()).crearPedido(any(), any());
    }

    @Test
    void shouldReturnCheckoutSummaryForAuthenticatedClient() throws Exception {
        when(pedidoService.obtenerCheckoutResumen(CLIENTE_ID)).thenReturn(
                new es.uclm.esibuy.pedidos.dto.CheckoutResumenDTO(
                        List.of(),
                        List.of(new MetodoPagoCheckoutDTO("pago-1", PaymentMethodType.CARD, "VISA", "1234", true)),
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

        verify(pedidoService).obtenerCheckoutResumen(CLIENTE_ID);
    }

    @Test
    void shouldReturnSellerOrdersForAuthenticatedSeller() throws Exception {
        when(pedidoService.obtenerPedidosDelVendedor(CLIENTE_ID)).thenReturn(List.of());

        mockMvc.perform(get(API + "/vendedor")
                .with(authentication(authenticationFor(UserRole.SELLER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(pedidoService).obtenerPedidosDelVendedor(CLIENTE_ID);
    }

    @Test
    void shouldRejectSellerOrdersForAuthenticatedClient() throws Exception {
        mockMvc.perform(get(API + "/vendedor")
                .with(authentication(authenticationFor(UserRole.CLIENT))))
                .andExpect(status().isForbidden());

        verify(pedidoService, never()).obtenerPedidosDelVendedor(any());
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

        verify(pedidoService).actualizarEstadoPedido(CLIENTE_ID, "pedido-123", PedidoEstado.ENVIADO, null);
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

        verify(pedidoService, never()).actualizarEstadoPedido(any(), any(), any(), any());
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

        verify(pedidoService, never()).actualizarEstadoPedido(any(), any(), any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenOrderDoesNotBelongToSeller() throws Exception {
        doThrow(new IllegalArgumentException("El pedido no pertenece al vendedor"))
                .when(pedidoService).actualizarEstadoPedido(CLIENTE_ID, "pedido-123", PedidoEstado.ENVIADO, null);

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
        doThrow(new PagoFallidoException()).when(pedidoService)
                .crearPedido(eq(CLIENTE_ID), any());

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
