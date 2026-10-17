package es.uclm.esibuy.cart.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.cart.dto.CarritoDTO;
import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.cart.service.CarritoService;
import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarritoController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class CarritoControllerTest {

    private static final String API = "/api/carrito";
    private static final String CLIENTE_ID = "cliente-1";
    private static final String PRODUCTO_ID = "producto-1";
    private static final BigDecimal PRECIO = new BigDecimal("19.99");
    private static final String VALID_BODY = """
            {"productoId":"producto-1","cantidad":2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarritoService carritoService;

    private Authentication authenticationFor(UserRole role) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                CLIENTE_ID, "cliente@example.com", role, true);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    private CarritoDTO carrito() {
        return new CarritoDTO("carrito-1", CLIENTE_ID,
                List.of(new ItemCarritoDTO(PRODUCTO_ID, "Auriculares", PRECIO, 2, new BigDecimal("39.98"))),
                new BigDecimal("39.98"));
    }

    @Test
    void shouldAddAnItemUsingTheAuthenticatedClientId() throws Exception {
        when(carritoService.anadirItem(eq(CLIENTE_ID), any())).thenReturn(carrito());

        mockMvc.perform(post(API + "/items")
                .with(authentication(authenticationFor(UserRole.CLIENT)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_ID))
                .andExpect(jsonPath("$.items[0].productoId").value(PRODUCTO_ID))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(39.98))
                .andExpect(jsonPath("$.precioTotal").value(39.98));

        verify(carritoService).anadirItem(eq(CLIENTE_ID), any());
    }

    @Test
    void shouldReturnTheAuthenticatedClientCart() throws Exception {
        when(carritoService.getCarrito(CLIENTE_ID)).thenReturn(carrito());

        mockMvc.perform(get(API).with(authentication(authenticationFor(UserRole.CLIENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("carrito-1"))
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_ID))
                .andExpect(jsonPath("$.precioTotal").value(39.98));

        verify(carritoService).getCarrito(CLIENTE_ID);
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
                .andExpect(jsonPath("$.errors.cantidad").isNotEmpty());

        verify(carritoService, never()).anadirItem(any(), any());
    }

    @Test
    void shouldReturnConflictWhenStockIsInsufficient() throws Exception {
        when(carritoService.anadirItem(eq(CLIENTE_ID), any()))
                .thenThrow(new StockInsuficienteException());

        mockMvc.perform(post(API + "/items")
                .with(authentication(authenticationFor(UserRole.CLIENT)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay stock suficiente para la cantidad solicitada"));
    }

    @Test
    void shouldReturnConflictForInactiveProduct() throws Exception {
        when(carritoService.anadirItem(eq(CLIENTE_ID), any()))
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
        when(carritoService.anadirItem(eq(CLIENTE_ID), any()))
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
        when(carritoService.anadirItem(eq(CLIENTE_ID), any()))
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

        mockMvc.perform(get(API).with(authentication(authenticationFor(UserRole.SELLER))))
                .andExpect(status().isForbidden());
    }
}
