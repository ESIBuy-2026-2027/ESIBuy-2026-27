package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.service.ProductService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import es.uclm.esibuy.products.model.ProductDetails;

@WebMvcTest(ProductController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class ProductControllerTest {

        private static final String CATEGORY_ID = "category-id";
        private static final String PRODUCT_NAME = "auriculares";
        private static final String SELLER_ID = "seller-id";
        private static final String SELLER_EMAIL = "seller@example.com";
        private static final String ADMIN_EMAIL = "admin@example.com";
        private static final String PRODUCTS_API = "/api/products";
        private static final String PRODUCT_PRICE = "19.99";
        private static final String PROD_1_ID = "prod-1";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ProductService productService;

        private Authentication authenticationForSeller() {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                                SELLER_ID, SELLER_EMAIL, UserRole.SELLER, true);
                return new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
        }

        @Test
        void shouldReturnCreatedProductAndUseAuthenticatedIdentity() throws Exception {
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                0,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID));
                when(productService.createProduct(any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenReturn(product);

                mockMvc.perform(post(PRODUCTS_API)
                                .with(authentication(authenticationForSeller()))
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
        void shouldReturnAllFieldValidationErrors() throws Exception {
                mockMvc.perform(post(PRODUCTS_API)
                                .with(authentication(authenticationForSeller()))
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
        void shouldReturnClearBadRequestForUnknownCategory() throws Exception {
                when(productService.createProduct(any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenThrow(new CategoryNotFoundException(List.of(CATEGORY_ID)));

                mockMvc.perform(post(PRODUCTS_API)
                                .with(authentication(authenticationForSeller()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("No existen las categorías indicadas: " + CATEGORY_ID));
        }

        @Test
        void shouldReturnSellerSummary() throws Exception {
                when(productService.getSummary(SELLER_EMAIL))
                                .thenReturn(new ProductSummaryResponse(12, 2, 3, 5));

                mockMvc.perform(get(PRODUCTS_API + "/summary")
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalProducts").value(12))
                                .andExpect(jsonPath("$.outOfStockProducts").value(2))
                                .andExpect(jsonPath("$.lowStockProducts").value(3))
                                .andExpect(jsonPath("$.lowStockThreshold").value(5));
        }

        @Test
        void shouldReturnSellerProducts() throws Exception {
                when(productService.getSellerProducts(SELLER_EMAIL))
                                .thenReturn(List.of(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 3,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID)));

                mockMvc.perform(get(PRODUCTS_API + "/mine")
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value(PRODUCT_NAME))
                                .andExpect(jsonPath("$[0].sellerId").value(SELLER_ID));
        }

        @Test
        void shouldReturnProductDetailsForEditing() throws Exception {
                when(productService.getProductById("product-1")).thenReturn(new ProductResponse(
                                "product-1",
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                3,
                                "Descripción",
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID));

                mockMvc.perform(get(PRODUCTS_API + "/product-1")
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                                .andExpect(jsonPath("$.categoryIds[0]").value(CATEGORY_ID))
                                .andExpect(jsonPath("$.stock").value(3));

                verify(productService).getProductById("product-1");
        }

        @Test
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
                when(productService.getProductById("product-1"))
                                .thenThrow(new ProductNotFoundException());

                mockMvc.perform(get(PRODUCTS_API + "/product-1")
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Producto no encontrado"))
                                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }

        @Test
        void shouldUpdateProductForAuthenticatedSeller() throws Exception {
                when(productService.updateProduct(
                                eq("product-1"), any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenReturn(new Product(new ProductDetails(
                                                "Producto actualizado",
                                                List.of(CATEGORY_ID),
                                                4,
                                                null,
                                                new BigDecimal(PRODUCT_PRICE),
                                                null,
                                                null,
                                                null,
                                                true,
                                                SELLER_ID)));

                mockMvc.perform(put(PRODUCTS_API + "/product-1")
                                .with(authentication(authenticationForSeller()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Producto actualizado"))
                                .andExpect(jsonPath("$.sellerId").value(SELLER_ID));

                verify(productService).updateProduct(
                                eq("product-1"), any(ProductCreateRequest.class), eq(SELLER_EMAIL));
        }

        @Test
        void shouldValidateProductUpdates() throws Exception {
                mockMvc.perform(put(PRODUCTS_API + "/product-1")
                                .with(authentication(authenticationForSeller()))
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
        void shouldRejectClientFromProductEndpoints() throws Exception {
                mockMvc.perform(get(PRODUCTS_API + "/mine")
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnForbiddenForClientAccessingSellerSummary() throws Exception {
                mockMvc.perform(get(PRODUCTS_API + "/summary")
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldAllowSellerToAccessOwnedProduct() throws Exception {
                when(productService.getProductById(PROD_1_ID))
                                .thenReturn(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 3, "Desc",
                                                new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID));

                mockMvc.perform(get(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
        }

        @Test
        void shouldAllowSellerToUpdateOwnedProduct() throws Exception {
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 8, "Updated",
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));
                when(productService.updateProduct(eq(PROD_1_ID), any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenReturn(product);

                mockMvc.perform(put(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(authentication(authenticationForSeller()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
        }

        @Test
        void shouldReturnNotFoundWhenSellerAccessesMissingProduct() throws Exception {
                when(productService.getProductById("missing"))
                                .thenThrow(new ProductNotFoundException());

                mockMvc.perform(get(PRODUCTS_API + "/missing")
                                .with(authentication(authenticationForSeller())))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundWhenSellerUpdatesMissingProduct() throws Exception {
                when(productService.updateProduct(eq("missing"), any(ProductCreateRequest.class), eq(SELLER_EMAIL)))
                                .thenThrow(new ProductNotFoundException());

                mockMvc.perform(put(PRODUCTS_API + "/missing")
                                .with(authentication(authenticationForSeller()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldAllowPublicAccessToCatalog() throws Exception {
                when(productService.getPublicCatalog(null, null, null, null, null, null))
                                .thenReturn(List.of(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID)));

                mockMvc.perform(get(PRODUCTS_API))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(PROD_1_ID))
                                .andExpect(jsonPath("$[0].name").value(PRODUCT_NAME));
        }

        @Test
        void shouldAllowPublicAccessToProductById() throws Exception {
                when(productService.getProductById(PROD_1_ID))
                                .thenReturn(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID));

                mockMvc.perform(get(PRODUCTS_API + "/" + PROD_1_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(PROD_1_ID))
                                .andExpect(jsonPath("$.name").value(PRODUCT_NAME));
        }

        @Test
        void shouldAllowSellerToUpdateStatus() throws Exception {
                when(productService.updateProductStatus(PROD_1_ID, false, SELLER_EMAIL))
                                .thenReturn(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, false,
                                                SELLER_ID));

                mockMvc.perform(patch(PRODUCTS_API + "/" + PROD_1_ID + "/status")
                                .param("available", "false")
                                .with(authentication(authenticationForSeller()))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.available").value(false));

                verify(productService).updateProductStatus(PROD_1_ID, false, SELLER_EMAIL);
        }

        @Test
        void shouldRejectClientFromUpdatingStatus() throws Exception {
                mockMvc.perform(patch(PRODUCTS_API + "/" + PROD_1_ID + "/status")
                                .param("available", "false")
                                .with(csrf())
                                .with(user("client@example.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldAllowSellerToDeleteProduct() throws Exception {
                mockMvc.perform(delete(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(authentication(authenticationForSeller()))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(productService).deleteProduct(PROD_1_ID, SELLER_EMAIL);
        }

        @Test
        void shouldRejectAdminAttemptingToCreateProductWithForbidden() throws Exception {
                mockMvc.perform(post(PRODUCTS_API)
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectAdminAttemptingToUpdateProductWithForbidden() throws Exception {
                mockMvc.perform(put(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectAdminAttemptingToDeleteProductWithForbidden() throws Exception {
                mockMvc.perform(delete(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(user(ADMIN_EMAIL).roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldAllowAdminToReadPublicCatalog() throws Exception {
                when(productService.getPublicCatalog(null, null, null, null, null, null))
                                .thenReturn(List.of(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID)));

                mockMvc.perform(get(PRODUCTS_API)
                                .with(user(ADMIN_EMAIL).roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(PROD_1_ID));
        }

        @Test
        void shouldAllowAdminToReadPublicProductDetail() throws Exception {
                when(productService.getProductById(PROD_1_ID))
                                .thenReturn(new ProductResponse(
                                                PROD_1_ID, PRODUCT_NAME, List.of(CATEGORY_ID), 5,
                                                "Desc", new BigDecimal(PRODUCT_PRICE), null, null, null, true,
                                                SELLER_ID));

                mockMvc.perform(get(PRODUCTS_API + "/" + PROD_1_ID)
                                .with(user(ADMIN_EMAIL).roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(PROD_1_ID));
        }

        private String validRequest() {
                return """
                                {"name":"%s","categoryIds":["%s"],"price":19.99,"available":true}
                                """.formatted(PRODUCT_NAME, CATEGORY_ID);
        }
}
