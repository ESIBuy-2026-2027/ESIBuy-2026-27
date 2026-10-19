package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.service.ProductService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(CustomerProductCatalogController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class CustomerProductCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void shouldReturnAvailableProductsForClients() throws Exception {
        when(productService.getAvailableProducts()).thenReturn(List.of(new ProductResponse(
                "product-1", "Auriculares", List.of("category-1"), 2, null, new BigDecimal("19.99"),
                null, null, null, true, "seller-1")));

        mockMvc.perform(get("/api/catalog/products")
                .with(authentication(authenticationFor(UserRole.CLIENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("product-1"))
                .andExpect(jsonPath("$[0].name").value("Auriculares"))
                .andExpect(jsonPath("$[0].stock").value(2))
                .andExpect(jsonPath("$[0].available").value(true));

        verify(productService).getAvailableProducts();
    }

    @Test
    void shouldRejectSellerAndUnauthenticatedCatalogRequests() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                .with(authentication(authenticationFor(UserRole.SELLER))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserRole role) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                "user-1", "user@example.com", role, true);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
