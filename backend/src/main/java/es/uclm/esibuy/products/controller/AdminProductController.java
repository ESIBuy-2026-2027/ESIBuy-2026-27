package es.uclm.esibuy.products.controller;

import java.util.List;

import es.uclm.esibuy.products.dto.ProductDetailDTO;
import es.uclm.esibuy.products.dto.ProductSummaryDTO;
import es.uclm.esibuy.products.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/api/admin/products", "/api/admin/productos" })
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductSummaryDTO> getProducts(
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean inStock) {
        return productService.getProductsForAdmin(sellerId, categoryId, inStock);
    }

    @GetMapping("/{id}")
    public ProductDetailDTO getProductDetail(@PathVariable String id) {
        return productService.getProductDetailForAdmin(id);
    }
}

