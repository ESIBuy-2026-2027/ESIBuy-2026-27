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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProductWithImage(
            @Valid @RequestPart("product") ProductCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ProductResponse response = ProductResponse.from(
                productService.createProductWithImage(request, image, principal.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse uploadProductImage(
            @PathVariable String id,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ProductResponse.from(productService.uploadProductImage(id, image, principal.email()));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String id) {
        ProductService.ProductImage image = productService.getProductImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache())
                .body(image.bytes());
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
    public ProductResponse getProduct(@PathVariable String id) {
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse updateProductWithImage(
            @PathVariable String id,
            @Valid @RequestPart("product") ProductCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ProductResponse.from(
                productService.updateProductWithImage(id, request, image, principal.email()));
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
