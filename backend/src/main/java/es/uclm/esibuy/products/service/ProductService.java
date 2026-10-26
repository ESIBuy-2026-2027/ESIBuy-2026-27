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
import es.uclm.esibuy.products.dto.ProductDetailDTO;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryDTO;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final String NO_PERMISSIONS_MESSAGE = "No tiene permisos para modificar este producto";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Producto no encontrado";

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

    public Product createProduct(
            ProductCreateRequest request,
            String authenticatedEmail) {
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
                .orElseThrow(ProductNotFoundException::new);

        if (!seller.getId().equals(product.getSellerId())) {
            throw new ProductNotFoundException();
        }

        return product;
    }

    public Product updateProduct(
            String id,
            ProductCreateRequest request,
            String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        if (!seller.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        List<String> categoryIds = validateCategories(request);

        product.setName(request.getName().trim());
        product.setCategoryIds(categoryIds);
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setDescription(
                request.getDescription() != null
                        ? request.getDescription().trim()
                        : null);
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        product.setPremiumDiscount(request.getPremiumDiscount());
        product.setImageUrl(
                request.getImageUrl() != null && !request.getImageUrl().isBlank()
                        ? request.getImageUrl().trim()
                        : null);
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
                .filter(product -> matchesQuery(product, query))
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> matchesMinPrice(product, minPrice))
                .filter(product -> matchesMaxPrice(product, maxPrice))
                .filter(product -> matchesMinRating(product, minRating))
                .filter(product -> matchesStock(product, inStock))
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductSummaryDTO> getProductsForAdmin(
            String sellerId,
            String categoryId,
            Boolean inStock) {

        List<Product> products;
        if (sellerId != null && !sellerId.isBlank()) {
            products = productRepository.findAllBySellerId(sellerId.trim());
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> matchesStockFilter(product, inStock))
                .map(this::toProductSummaryDTO)
                .toList();
    }

    public ProductDetailDTO getProductDetailForAdmin(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));
        String categoryName = resolveCategoryName(product);
        String sellerName = resolveSellerName(product.getSellerId());
        return ProductDetailDTO.from(product, categoryName, sellerName);
    }

    public void deleteProduct(String id, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (!seller.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        productRepository.delete(product);
    }

    private ProductSummaryDTO toProductSummaryDTO(Product product) {
        String categoryName = resolveCategoryName(product);
        String sellerName = resolveSellerName(product.getSellerId());
        return ProductSummaryDTO.from(product, categoryName, sellerName);
    }

    private String resolveCategoryName(Product product) {
        if (product.getCategoryIds() == null || product.getCategoryIds().isEmpty()) {
            return null;
        }
        String firstCategoryId = product.getCategoryIds().get(0);
        return categoryRepository.findById(firstCategoryId)
                .map(Category::getName)
                .orElse(firstCategoryId);
    }

    private String resolveSellerName(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            return null;
        }
        return userRepository.findById(sellerId)
                .map(user -> {
                    if (user.getSellerProfile() != null
                            && user.getSellerProfile().getBusinessName() != null
                            && !user.getSellerProfile().getBusinessName().isBlank()) {
                        return user.getSellerProfile().getBusinessName();
                    }
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName.isBlank() ? user.getEmail() : fullName;
                })
                .orElse(null);
    }

    private boolean matchesStockFilter(Product product, Boolean inStock) {
        if (inStock == null) {
            return true;
        }
        if (Boolean.TRUE.equals(inStock)) {
            return product.getStock() > 0;
        }
        return product.getStock() <= 0;
    }

    private boolean matchesQuery(Product product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();

        boolean matchesName = product.getName() != null
                && product.getName().toLowerCase().contains(normalizedQuery);

        boolean matchesDescription = product.getDescription() != null
                && product.getDescription().toLowerCase().contains(normalizedQuery);

        return matchesName || matchesDescription;
    }

    private boolean matchesCategory(Product product, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return true;
        }

        return product.getCategoryIds() != null
                && product.getCategoryIds().contains(categoryId.trim());
    }

    private boolean matchesMinPrice(Product product, BigDecimal minPrice) {
        if (minPrice == null) {
            return true;
        }

        return product.getPrice() != null
                && product.getPrice().compareTo(minPrice) >= 0;
    }

    private boolean matchesMaxPrice(Product product, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return true;
        }

        return product.getPrice() != null
                && product.getPrice().compareTo(maxPrice) <= 0;
    }

    private boolean matchesMinRating(Product product, Double minRating) {
        if (minRating == null) {
            return true;
        }

        return product.getRating() != null
                && product.getRating() >= minRating;
    }

    private boolean matchesStock(Product product, Boolean inStock) {
        if (!Boolean.TRUE.equals(inStock)) {
            return true;
        }

        return product.getStock() > 0;
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        return ProductResponse.from(product);
    }

    public ProductResponse updateProductStatus(
            String id,
            boolean available,
            String authenticatedEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Autenticación requerida");
        }

        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(
                        () -> new AccessDeniedException("Usuario no encontrado"));

        if (user.getRole() != UserRole.ADMIN
                && !user.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        product.setAvailable(available);

        return ProductResponse.from(productRepository.save(product));
    }

    private User findSeller(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException(
                    "Se requiere una sesión de vendedor autenticada");
        }

        User seller = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "No se encontró el vendedor autenticado"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new AccessDeniedException(
                    "Solo un vendedor puede acceder a este recurso");
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
