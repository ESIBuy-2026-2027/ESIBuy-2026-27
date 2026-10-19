package es.uclm.esibuy.products.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public Product createProduct(ProductCreateRequest request, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);
        List<String> categoryIds = validateCategories(request);

        Product product = new Product(
                request.getName().trim(),
                categoryIds,
                request.getStock() == null ? 0 : request.getStock(),
                request.getDescription(),
                request.getPrice(),
                request.getDiscount(),
                request.getPremiumDiscount(),
                request.getImageUrl(),
                Boolean.TRUE.equals(request.getAvailable()),
                seller.getId());
        return productRepository.save(product);
    }

    public ProductSummaryResponse getSummary(String authenticatedEmail) {
        String sellerId = findSeller(authenticatedEmail).getId();
        return new ProductSummaryResponse(
                productRepository.countBySellerId(sellerId),
                productRepository.countBySellerIdAndStock(sellerId, 0),
                productRepository.countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
                        sellerId, 0, LOW_STOCK_THRESHOLD),
                LOW_STOCK_THRESHOLD);
    }

    public List<ProductResponse> getSellerProducts(String authenticatedEmail) {
        String sellerId = findSeller(authenticatedEmail).getId();
        return productRepository.findAllBySellerId(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Product getProduct(String id, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException());
        if (!seller.getId().equals(product.getSellerId())) {
            throw new ProductNotFoundException();
        }
        return product;
    }

    public Product updateProduct(String id, ProductCreateRequest request, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException());
        if (!seller.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException("No tiene permisos para modificar este producto");
        }
        List<String> categoryIds = validateCategories(request);
        product.setName(request.getName().trim());
        product.setCategoryIds(categoryIds);
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        product.setPremiumDiscount(request.getPremiumDiscount());
        product.setImageUrl(request.getImageUrl() != null && !request.getImageUrl().isBlank() ? request.getImageUrl().trim() : null);
        product.setAvailable(Boolean.TRUE.equals(request.getAvailable()));
        return productRepository.save(product);
    }

    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findAllByAvailableTrueAndStockGreaterThan(0).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getPublicCatalog(
            String query,
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean inStock) {
        return productRepository.findAllByAvailableTrue().stream()
                .filter(p -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.trim().toLowerCase();
                    boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(q);
                    boolean matchDesc = p.getDescription() != null && p.getDescription().toLowerCase().contains(q);
                    return matchName || matchDesc;
                })
                .filter(p -> {
                    if (categoryId == null || categoryId.isBlank()) return true;
                    return p.getCategoryIds() != null && p.getCategoryIds().contains(categoryId.trim());
                })
                .filter(p -> {
                    if (minPrice == null) return true;
                    return p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0;
                })
                .filter(p -> {
                    if (maxPrice == null) return true;
                    return p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0;
                })
                .filter(p -> {
                    if (minRating == null) return true;
                    return p.getRating() != null && p.getRating() >= minRating;
                })
                .filter(p -> {
                    if (!Boolean.TRUE.equals(inStock)) return true;
                    return p.getStock() > 0;
                })
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado"));
        return ProductResponse.from(product);
    }

    public ProductResponse updateProductStatus(String id, boolean available, String authenticatedEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado"));

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Autenticación requerida");
        }

        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));

        if (user.getRole() != UserRole.ADMIN && !user.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException("No tiene permisos para modificar este producto");
        }

        product.setAvailable(available);
        return ProductResponse.from(productRepository.save(product));
    }

    private User findSeller(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Se requiere una sesión de vendedor autenticada");
        }
        User seller = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("No se encontró el vendedor autenticado"));
        if (seller.getRole() != UserRole.SELLER) {
            throw new AccessDeniedException("Solo un vendedor puede acceder a este recurso");
        }
        return seller;
    }

    private List<String> validateCategories(ProductCreateRequest request) {
        List<String> categoryIds = List.copyOf(request.getCategoryIds());
        Set<String> existingCategoryIds = categoryRepository.findAllById(categoryIds).stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        List<String> missingCategoryIds = categoryIds.stream()
                .filter(categoryId -> !existingCategoryIds.contains(categoryId))
                .distinct()
                .toList();
        if (!missingCategoryIds.isEmpty()) {
            throw new CategoryNotFoundException(missingCategoryIds);
        }
        return categoryIds;
    }
}
