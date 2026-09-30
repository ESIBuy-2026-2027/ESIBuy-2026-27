package es.uclm.esibuy.products.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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

    private User findSeller(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return userRepository.findAll().stream()
                    .filter(user -> user.getRole() == UserRole.SELLER)
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException("No se encontró ningún vendedor registrado en el sistema"));
        }
        User seller = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("No se encontró el vendedor autenticado"));
        if (seller.getRole() != UserRole.SELLER) {
            throw new AccessDeniedException("Solo un vendedor puede acceder a este recurso");
        }
        return seller;
    }
}
