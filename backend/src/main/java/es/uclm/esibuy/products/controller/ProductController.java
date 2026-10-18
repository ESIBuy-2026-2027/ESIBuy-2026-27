package es.uclm.esibuy.products.controller;

import java.util.List;

import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasRole('SELLER')")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        ProductResponse response = ProductResponse.from(
                productService.createProduct(request, email));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public List<ProductResponse> getSellerProducts(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return productService.getSellerProducts(email);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return ProductResponse.from(productService.getProduct(id, email));
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return ProductResponse.from(productService.updateProduct(id, request, email));
    }

    @GetMapping({"/summary", "/resumen"})
    public ProductSummaryResponse getSummary(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        String email = principal.email();
        return productService.getSummary(email);
    }
}