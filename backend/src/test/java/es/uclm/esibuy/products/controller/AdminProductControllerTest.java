package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.products.dto.ProductDetailDTO;
import es.uclm.esibuy.products.dto.ProductSummaryDTO;
import es.uclm.esibuy.products.service.ProductService;
import es.uclm.esibuy.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class AdminProductControllerTest {

        private static final String ADMIN_PRODUCTS_API = "/api/admin/products";
        private static final String ADMIN_PRODUCTOS_SPANISH_API = "/api/admin/productos";
        private static final String ADMIN_USER_EMAIL = "admin@example.com";
        private static final String ROLE_ADMIN_NAME = "ADMIN";
        private static final String ROLE_CLIENT_NAME = "CLIENT";
        private static final String ROLE_SELLER_NAME = "SELLER";
        private static final String DEFAULT_PRODUCT_ID = "prod-100";
        private static final String DEFAULT_PRODUCT_NAME = "Monitor 4K";
        private static final String DEFAULT_CATEGORY_NAME = "Displays";
        private static final String DEFAULT_CATEGORY_ID = "cat-displays";
        private static final String DEFAULT_SELLER_ID = "seller-99";
        private static final String DEFAULT_SELLER_NAME = "Display Tech";
        private static final BigDecimal DEFAULT_PRICE = new BigDecimal("299.99");
        private static final int DEFAULT_STOCK = 12;

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ProductService productService;

        private ProductSummaryDTO createSampleSummaryDTO() {
                return new ProductSummaryDTO(
                                DEFAULT_PRODUCT_ID,
                                DEFAULT_PRODUCT_NAME,
                                "Monitor 4K UHD 27 inches",
                                DEFAULT_PRICE,
                                DEFAULT_STOCK,
                                DEFAULT_CATEGORY_NAME,
                                DEFAULT_SELLER_ID,
                                DEFAULT_SELLER_NAME,
                                true);
        }

        private ProductDetailDTO createSampleDetailDTO() {
                return new ProductDetailDTO(
                                DEFAULT_PRODUCT_ID,
                                DEFAULT_PRODUCT_NAME,
                                "Monitor 4K UHD 27 inches IPS panel",
                                DEFAULT_PRICE,
                                DEFAULT_STOCK,
                                DEFAULT_CATEGORY_NAME,
                                DEFAULT_SELLER_ID,
                                DEFAULT_SELLER_NAME,
                                true);
        }

        @Test
        void shouldAllowAdminToGetAllProductsForSupervision() throws Exception {
                when(productService.getProductsForAdmin(null, null, null))
                                .thenReturn(List.of(createSampleSummaryDTO()));

                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(DEFAULT_PRODUCT_ID))
                                .andExpect(jsonPath("$[0].name").value(DEFAULT_PRODUCT_NAME))
                                .andExpect(jsonPath("$[0].category").value(DEFAULT_CATEGORY_NAME))
                                .andExpect(jsonPath("$[0].sellerName").value(DEFAULT_SELLER_NAME))
                                .andExpect(jsonPath("$[0].stock").value(DEFAULT_STOCK))
                                .andExpect(jsonPath("$[0].active").value(true));

                verify(productService).getProductsForAdmin(null, null, null);
        }

        @Test
        void shouldAllowAdminToFilterProductsBySeller() throws Exception {
                when(productService.getProductsForAdmin(eq(DEFAULT_SELLER_ID), isNull(), isNull()))
                                .thenReturn(List.of(createSampleSummaryDTO()));

                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .param("sellerId", DEFAULT_SELLER_ID)
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].sellerId").value(DEFAULT_SELLER_ID));

                verify(productService).getProductsForAdmin(DEFAULT_SELLER_ID, null, null);
        }

        @Test
        void shouldAllowAdminToFilterProductsByCategory() throws Exception {
                when(productService.getProductsForAdmin(isNull(), eq(DEFAULT_CATEGORY_ID), isNull()))
                                .thenReturn(List.of(createSampleSummaryDTO()));

                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .param("categoryId", DEFAULT_CATEGORY_ID)
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(DEFAULT_PRODUCT_ID));

                verify(productService).getProductsForAdmin(null, DEFAULT_CATEGORY_ID, null);
        }

        @Test
        void shouldAllowAdminToFilterProductsByStockStatus() throws Exception {
                when(productService.getProductsForAdmin(isNull(), isNull(), eq(true)))
                                .thenReturn(List.of(createSampleSummaryDTO()));

                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .param("inStock", "true")
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].stock").value(DEFAULT_STOCK));

                verify(productService).getProductsForAdmin(null, null, true);
        }

        @Test
        void shouldAllowAdminToGetProductDetailById() throws Exception {
                when(productService.getProductDetailForAdmin(DEFAULT_PRODUCT_ID))
                                .thenReturn(createSampleDetailDTO());

                mockMvc.perform(get(ADMIN_PRODUCTS_API + "/" + DEFAULT_PRODUCT_ID)
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(DEFAULT_PRODUCT_ID))
                                .andExpect(jsonPath("$.name").value(DEFAULT_PRODUCT_NAME))
                                .andExpect(jsonPath("$.description").value("Monitor 4K UHD 27 inches IPS panel"))
                                .andExpect(jsonPath("$.sellerName").value(DEFAULT_SELLER_NAME));

                verify(productService).getProductDetailForAdmin(DEFAULT_PRODUCT_ID);
        }

        @Test
        void shouldReturnNotFoundWhenAdminGetsDetailOfNonExistentProduct() throws Exception {
                when(productService.getProductDetailForAdmin("unknown-id"))
                                .thenThrow(new ProductNotFoundException("Producto no encontrado"));

                mockMvc.perform(get(ADMIN_PRODUCTS_API + "/unknown-id")
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Producto no encontrado"));
        }

        @Test
        void shouldRejectClientFromAdminProductSupervision() throws Exception {
                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .with(user("client@example.com").roles(ROLE_CLIENT_NAME)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectSellerFromAdminProductSupervision() throws Exception {
                mockMvc.perform(get(ADMIN_PRODUCTS_API)
                                .with(user("seller@example.com").roles(ROLE_SELLER_NAME)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectUnauthenticatedUserFromAdminProductSupervision() throws Exception {
                mockMvc.perform(get(ADMIN_PRODUCTS_API))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldSupportSpanishRouteAliasForAdminSupervision() throws Exception {
                when(productService.getProductsForAdmin(null, null, null))
                                .thenReturn(List.of(createSampleSummaryDTO()));

                mockMvc.perform(get(ADMIN_PRODUCTOS_SPANISH_API)
                                .with(user(ADMIN_USER_EMAIL).roles(ROLE_ADMIN_NAME)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(DEFAULT_PRODUCT_ID));

                verify(productService).getProductsForAdmin(null, null, null);
        }
}
