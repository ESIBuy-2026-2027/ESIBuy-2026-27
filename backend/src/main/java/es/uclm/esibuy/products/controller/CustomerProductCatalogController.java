package es.uclm.esibuy.products.controller;

import java.util.List;

import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/products")
@PreAuthorize("hasRole('CLIENT')")
public class CustomerProductCatalogController {

    private final ProductService productService;

    public CustomerProductCatalogController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAvailableProducts() {
        return productService.getAvailableProducts();
    }
}
