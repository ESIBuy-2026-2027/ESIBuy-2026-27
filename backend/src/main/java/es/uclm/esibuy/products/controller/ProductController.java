package es.uclm.esibuy.products.controller;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.service.ProductService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.model.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
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
    @PreAuthorize("hasRole('SELLER')")
    public List<ProductResponse> getSellerProducts(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return productService.getSellerProducts(email);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal != null && principal.role() == UserRole.SELLER) {
            return ProductResponse.from(productService.getProduct(id, principal.email()));
        }
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return ProductResponse.from(productService.updateProduct(id, request, email));
    }

    @GetMapping({ "/summary", "/resumen" })
    @PreAuthorize("hasRole('SELLER')")
    public ProductSummaryResponse getSummary(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return productService.getSummary(email);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse updateProductStatus(
            @PathVariable String id,
            @RequestParam boolean available,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return productService.updateProductStatus(id, available, email);
    }
}