package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ApiExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(username = "seller@example.com")
    void shouldReturnCreatedProductAndUseAuthenticatedIdentity() throws Exception {
        Product product = new Product(
                "Auriculares", List.of("category-id"), 0, null,
                new BigDecimal("19.99"), null, null, null, true, "seller-id");
        when(productService.createProduct(any(ProductCreateRequest.class), eq("seller@example.com")))
                .thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Auriculares"))
                .andExpect(jsonPath("$.categoryIds[0]").value("category-id"))
                .andExpect(jsonPath("$.sellerId").value("seller-id"));

        verify(productService).createProduct(any(ProductCreateRequest.class), eq("seller@example.com"));
    }

    @Test
    @WithMockUser(username = "seller@example.com")
    void shouldReturnAllFieldValidationErrors() throws Exception {
        mockMvc.perform(post("/api/products")
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
    @WithMockUser(username = "seller@example.com")
    void shouldReturnClearBadRequestForUnknownCategory() throws Exception {
        when(productService.createProduct(any(ProductCreateRequest.class), eq("seller@example.com")))
                .thenThrow(new CategoryNotFoundException(List.of("category-id")));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No existen las categorías indicadas: category-id"));
    }

    @Test
    @WithMockUser(username = "seller@example.com")
    void shouldReturnSellerSummary() throws Exception {
        when(productService.getSummary("seller@example.com"))
                .thenReturn(new ProductSummaryResponse(12, 2, 3, 5));

        mockMvc.perform(get("/api/products/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(12))
                .andExpect(jsonPath("$.outOfStockProducts").value(2))
                .andExpect(jsonPath("$.lowStockProducts").value(3))
                .andExpect(jsonPath("$.lowStockThreshold").value(5));
    }

    private String validRequest() {
        return """
                {"name":"Auriculares","categoryIds":["category-id"],"price":19.99,"available":true}
                """;
    }
}
