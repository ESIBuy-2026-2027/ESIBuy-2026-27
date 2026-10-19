package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.service.ProductService;
import es.uclm.esibuy.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class ProductControllerTest {

        private static final String CATEGORY_ID = "category-id";
        private static final String PRODUCT_NAME = "auriculares";
        private static final String SELLER_ID = "seller-id";
        private static final String SELLER_EMAIL = "seller@example.com";
        private static final String PRODUCTS_API = "/api/products";
        private static final String PRODUCT_PRICE = "19.99";

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldReturnCreatedProductAndUseAuthenticatedIdentity() throws Exception {
                Product product = new Product(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 0, null,
                                new BigDecimal("19.99"), null, null, null, true, SELLER_ID);
                when(productService.createProduct(any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenReturn(product);

                mockMvc.perform(post(PRODUCTS_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                                .andExpect(jsonPath("$.categoryIds[0]").value(CATEGORY_ID))
                                .andExpect(jsonPath("$.sellerId").value(SELLER_ID));

                verify(productService).createProduct(any(ProductCreateRequest.class), eq(SELLER_EMAIL));
        }

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldReturnAllFieldValidationErrors() throws Exception {
                mockMvc.perform(post(PRODUCTS_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"name":"","categoryIds":[],"price":0,"available":null}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.name").isNotEmpty())
                                .andExpect(jsonPath("$.errors.categoryIds").isNotEmpty())
                                .andExpect(jsonPath("$.errors.price").isNotEmpty())
                                .andExpect(jsonPath("$.errors.available").isNotEmpty());
        }

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldReturnClearBadRequestForUnknownCategory() throws Exception {
                when(productService.createProduct(any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenThrow(new CategoryNotFoundException(List.of(CATEGORY_ID)));

                mockMvc.perform(post(PRODUCTS_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("No existen las categorías indicadas: " + CATEGORY_ID));
        }

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldReturnSellerSummary() throws Exception {
                when(productService.getSummary(SELLER_EMAIL))
                                .thenReturn(new ProductSummaryResponse(12, 2, 3, 5));

                mockMvc.perform(get(PRODUCTS_API + "/summary"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalProducts").value(12))
                                .andExpect(jsonPath("$.outOfStockProducts").value(2))
                                .andExpect(jsonPath("$.lowStockProducts").value(3))
                                .andExpect(jsonPath("$.lowStockThreshold").value(5));
        }

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldReturnProductsForAuthenticatedSeller() throws Exception {
                when(productService.getSellerProducts(SELLER_EMAIL))
                                .thenReturn(List.of(new ProductResponse(
                                                "product-1", PRODUCT_NAME, List.of(CATEGORY_ID), 3,
                                                "Inalámbricos", new BigDecimal(PRODUCT_PRICE), null, null,
                                                null, true, SELLER_ID)));

                mockMvc.perform(get(PRODUCTS_API + "/mine"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("product-1"))
                                .andExpect(jsonPath("$[0].name").value(PRODUCT_NAME))
                                .andExpect(jsonPath("$[0].stock").value(3))
                                .andExpect(jsonPath("$[0].sellerId").value(SELLER_ID));

                verify(productService).getSellerProducts(SELLER_EMAIL);
        }

        @Test
        void shouldRejectClientFromProductEndpoints() throws Exception {
                mockMvc.perform(get(PRODUCTS_API + "/mine")
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnUnauthorizedForProductCreationWithoutSession() throws Exception {
                mockMvc.perform(post(PRODUCTS_API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldAllowPublicAccessToCatalog() throws Exception {
                when(productService.getPublicCatalog(null, null, null, null, null, null))
                                .thenReturn(List.of(new ProductResponse(
                                                "prod-1", PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID)));

                mockMvc.perform(get(PRODUCTS_API))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("prod-1"))
                                .andExpect(jsonPath("$[0].name").value(PRODUCT_NAME));
        }

        @Test
        void shouldAllowPublicAccessToProductById() throws Exception {
                when(productService.getProductById("prod-1"))
                                .thenReturn(new ProductResponse(
                                                "prod-1", PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));

                mockMvc.perform(get(PRODUCTS_API + "/prod-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("prod-1"))
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
        }

        @Test
        @WithMockUser(username = SELLER_EMAIL, roles = "SELLER")
        void shouldAllowSellerToUpdateStatus() throws Exception {
                when(productService.updateProductStatus("prod-1", false, SELLER_EMAIL))
                                .thenReturn(new ProductResponse(
                                                "prod-1", PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, false, SELLER_ID));

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(PRODUCTS_API + "/prod-1/status")
                                .param("available", "false")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.available").value(false));
        }

        @Test
        void shouldRejectClientFromUpdatingStatus() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(PRODUCTS_API + "/prod-1/status")
                                .param("available", "false")
                                .with(csrf())
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        private String validRequest() {
                return """
                                {"name":"%s","categoryIds":["%s"],"price":19.99,"available":true}
                                """.formatted(PRODUCT_NAME, CATEGORY_ID);
        }
}
