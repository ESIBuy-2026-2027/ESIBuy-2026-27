package es.uclm.esibuy.products.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, userRepository);
    }

    @Test
    void shouldCreateProductForAuthenticatedSellerAndDefaultStockToZero() {
        ProductCreateRequest request = validRequest();
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn("seller-id");
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of("category-id")))
                .thenReturn(List.of(new Category("category-id", "Electrónica")));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(request, "seller@example.com");

        assertEquals("seller-id", result.getSellerId());
        assertEquals(List.of("category-id"), result.getCategoryIds());
        assertEquals(0, result.getStock());
        assertEquals(new BigDecimal("19.99"), result.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldPreserveProvidedStockAndDiscounts() {
        ProductCreateRequest request = validRequest();
        request.setStock(7);
        request.setDiscount(new BigDecimal("10"));
        request.setPremiumDiscount(new BigDecimal("20"));
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn("seller-id");
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of("category-id")))
                .thenReturn(List.of(new Category("category-id", "Electrónica")));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(request, "seller@example.com");

        assertEquals(7, result.getStock());
        assertEquals(new BigDecimal("10"), result.getDiscount());
        assertEquals(new BigDecimal("20"), result.getPremiumDiscount());
    }

    @Test
    void shouldRejectUnknownCategoriesWithoutSavingProduct() {
        ProductCreateRequest request = validRequest();
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of("category-id"))).thenReturn(List.of());

        assertThrows(CategoryNotFoundException.class,
                () -> productService.createProduct(request, "seller@example.com"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectNonSellerAndNotSaveProduct() {
        ProductCreateRequest request = validRequest();
        User client = org.mockito.Mockito.mock(User.class);
        when(client.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.of(client));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.createProduct(request, "client@example.com"));
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectMissingAuthenticatedSellerWithoutSelectingAnySeller() {
        ProductCreateRequest request = validRequest();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.createProduct(request, null));
        verify(userRepository, never()).findAll();
        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldReturnSummaryForAuthenticatedSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn("seller-id");
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.countBySellerId("seller-id")).thenReturn(12L);
        when(productRepository.countBySellerIdAndStock("seller-id", 0)).thenReturn(2L);
        when(productRepository.countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
                "seller-id", 0, 5)).thenReturn(3L);

        ProductSummaryResponse result = productService.getSummary("seller@example.com");

        assertEquals(12L, result.totalProducts());
        assertEquals(2L, result.outOfStockProducts());
        assertEquals(3L, result.lowStockProducts());
        assertEquals(5, result.lowStockThreshold());
    }

    @Test
    void shouldRejectNonSellerSummaryRequest() {
        User client = org.mockito.Mockito.mock(User.class);
        when(client.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.of(client));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.getSummary("client@example.com"));
        verify(productRepository, never()).countBySellerId(any());
    }

    @Test
    void shouldReturnOnlyAuthenticatedSellerProductsAndMarkLowStock() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn("seller-1");
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase("vendedor1@tech.com")).thenReturn(Optional.of(seller));
        Product ownProduct = new Product(
                "Producto propio", List.of(), 3, null,
                new BigDecimal("10"), null, null, null, true, "seller-1");
        when(productRepository.findBySellerId("seller-1")).thenReturn(List.of(ownProduct));

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "vendedor1@tech.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")));

        List<ProductResponse> result = productService.getProductsForAuthenticatedUser(authentication);

        assertEquals(1, result.size());
        assertEquals("Producto propio", result.get(0).name());
        assertEquals("seller-1", result.get(0).sellerId());
        assertEquals(true, result.get(0).lowStock());
        verify(productRepository).findBySellerId("seller-1");
        verify(productRepository, never()).findAll();
    }

    @Test
    void shouldReturnAllProductsForAuthenticatedAdmin() {
        Product first = new Product(
                "Producto vendedor 1", List.of(), 10, null,
                new BigDecimal("10"), null, null, null, true, "seller-1");
        Product second = new Product(
                "Producto vendedor 2", List.of(), 0, null,
                new BigDecimal("20"), null, null, null, true, "seller-2");
        when(productRepository.findAll()).thenReturn(List.of(first, second));

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin@esibuy.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<ProductResponse> result = productService.getProductsForAuthenticatedUser(authentication);

        assertEquals(2, result.size());
        assertEquals("Producto vendedor 1", result.get(0).name());
        assertEquals("Producto vendedor 2", result.get(1).name());
        assertEquals(false, result.get(0).lowStock());
        assertEquals(false, result.get(1).lowStock());
        verify(productRepository).findAll();
        verify(productRepository, never()).findBySellerId(any());
    }

    @Test
    void shouldRejectUnauthorisedProductCatalogAccess() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "cliente@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> productService.getProductsForAuthenticatedUser(authentication));
        verify(productRepository, never()).findAll();
        verify(productRepository, never()).findBySellerId(any());
    }

    private ProductCreateRequest validRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Auriculares");
        request.setCategoryIds(List.of("category-id"));
        request.setPrice(new BigDecimal("19.99"));
        request.setAvailable(true);
        return request;
    }
}
