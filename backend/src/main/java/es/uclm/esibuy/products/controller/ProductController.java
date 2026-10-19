package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        ProductResponse response = ProductResponse.from(
                productService.createProduct(request, email));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ProductResponse> getPublicCatalog(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean inStock) {
        return productService.getPublicCatalog(query, categoryId, minPrice, maxPrice, minRating, inStock);
    }

    @GetMapping("/mine")
    public List<ProductResponse> getSellerProducts(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return productService.getSellerProducts(email);
    }

    @GetMapping({"/summary", "/resumen"})
    public ProductSummaryResponse getSummary(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return productService.getSummary(email);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse updateProductStatus(
            @PathVariable String id,
            @RequestParam boolean available,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return productService.updateProductStatus(id, available, email);
    }
}
