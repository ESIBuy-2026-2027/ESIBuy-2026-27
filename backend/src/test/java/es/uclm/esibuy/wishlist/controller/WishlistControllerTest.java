package es.uclm.esibuy.wishlist.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.wishlist.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class WishlistControllerTest {

    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String PRODUCT_ID = "product-1";
    private static final String WISHLIST_API = "/api/wishlist";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void shouldReturnUserWishlist() throws Exception {
        ProductResponse product = new ProductResponse(
                PRODUCT_ID, "Smartphone", List.of("cat1"), 10, "Desc",
                new BigDecimal("499.99"), null, null, null, true, "seller-1");
        when(wishlistService.getUserWishlist(CLIENT_EMAIL)).thenReturn(List.of(product));

        mockMvc.perform(get(WISHLIST_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID))
                .andExpect(jsonPath("$[0].name").value("Smartphone"));

        verify(wishlistService).getUserWishlist(CLIENT_EMAIL);
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void shouldReturnWishlistIds() throws Exception {
        when(wishlistService.getWishlistProductIds(CLIENT_EMAIL)).thenReturn(List.of(PRODUCT_ID));

        mockMvc.perform(get(WISHLIST_API + "/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(PRODUCT_ID));
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void shouldAddProductToWishlist() throws Exception {
        mockMvc.perform(post(WISHLIST_API + "/" + PRODUCT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(true));

        verify(wishlistService).addToWishlist(PRODUCT_ID, CLIENT_EMAIL);
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void shouldRemoveProductFromWishlist() throws Exception {
        mockMvc.perform(delete(WISHLIST_API + "/" + PRODUCT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(false));

        verify(wishlistService).removeFromWishlist(PRODUCT_ID, CLIENT_EMAIL);
    }

    @Test
    void shouldRejectAnonymousFromWishlist() throws Exception {
        mockMvc.perform(get(WISHLIST_API))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "seller@example.com", roles = "SELLER")
    void shouldRejectSellerFromWishlist() throws Exception {
        mockMvc.perform(get(WISHLIST_API))
                .andExpect(status().isForbidden());
    }
}

