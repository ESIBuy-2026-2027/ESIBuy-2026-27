package es.uclm.esibuy.reviews.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.reviews.service.ReviewService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;

@WebMvcTest(ReviewController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class ReviewControllerTest {

    private static final String API = "/api/reviews";
    private static final String CUSTOMER_ID = "cliente-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    void unClientePuedeCrearUnaValoracion() throws Exception {
        mockMvc.perform(post(API)
                .with(authentication(authenticationFor(UserRole.CLIENT)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void unVendedorNoPuedeCrearUnaValoracion() throws Exception {
        mockMvc.perform(post(API)
                .with(authentication(authenticationFor(UserRole.SELLER)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewRequest()))
                .andExpect(status().isForbidden());
    }

    private String reviewRequest() {
        return """
                {
                    "orderId": "pedido-123",
                    "productId": "producto-1",
                    "rating": 5,
                    "comment": "Producto excelente"
                }
                """;
    }

    private Authentication authenticationFor(UserRole role) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                CUSTOMER_ID,
                "cliente@example.com",
                role,
                true);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void crearValoracionCorrectamenteDevuelveMensajeDeExito() throws Exception {
        // Arrange
        Authentication autenticacion = authenticationFor(UserRole.CLIENT);

        // Act + Assert
        mockMvc.perform(post(API)
                .with(authentication(autenticacion))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewRequest()))
                .andExpect(status().isCreated())
                .andExpect(content().string("Valoración creada correctamente"));
    }

    @Test
    void noPermiteUnaPuntuacionFueraDelRango() throws Exception {
        mockMvc.perform(post(API)
                .with(authentication(authenticationFor(UserRole.CLIENT)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "orderId": "pedido-123",
                            "productId": "producto-1",
                            "rating": 6
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasValoracionesDeUnProductoSonPublicas() throws Exception {
        mockMvc.perform(get(API + "/product/producto-1"))
                .andExpect(status().isOk());
    }

    @Test
    void soloElClientePuedeConsultarSusComprasValoradas() throws Exception {
        mockMvc.perform(get(API + "/mine")
                .with(authentication(authenticationFor(UserRole.SELLER))))
                .andExpect(status().isForbidden());
    }
}
