package es.uclm.esibuy.products.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String SELLER_ID = "seller-id";
    private static final String OTHER_SELLER_ID = "other-seller-id";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String CATEGORY_ID = "category-id";
    private static final String PRODUCT_ID = "product-id";
    private static final String PRODUCT_NAME = "Auriculares";
    private static final String PRODUCT_PRICE = "19.99";
    private static final String CATEGORY_NAME_ELECTRONICS = "Electrónica";
    private static final String UNKNOWN_ID = "unknown";
    private static final String DESC_TEXT = "Descripción";

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
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of(CATEGORY_ID)))
                .thenReturn(List.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(request, SELLER_EMAIL);

        assertEquals(SELLER_ID, result.getSellerId());
        assertEquals(List.of(CATEGORY_ID), result.getCategoryIds());
        assertEquals(0, result.getStock());
        assertEquals(new BigDecimal(PRODUCT_PRICE), result.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldListOnlyAvailableProductsWithStockForCustomerCatalog() {
        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 3, null, new BigDecimal(PRODUCT_PRICE),
                null, null, null, true, SELLER_ID);
        when(productRepository.findAllByAvailableTrueAndStockGreaterThan(0)).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(PRODUCT_NAME, result.get(0).name());
        assertEquals(3, result.get(0).stock());
        verify(productRepository).findAllByAvailableTrueAndStockGreaterThan(0);
    }

    @Test
    void shouldPreserveProvidedStockAndDiscounts() {
        ProductCreateRequest request = validRequest();
        request.setStock(7);
        request.setDiscount(new BigDecimal("10"));
        request.setPremiumDiscount(new BigDecimal("20"));
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of(CATEGORY_ID)))
                .thenReturn(List.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(request, SELLER_EMAIL);

        assertEquals(7, result.getStock());
        assertEquals(new BigDecimal("10"), result.getDiscount());
        assertEquals(new BigDecimal("20"), result.getPremiumDiscount());
    }

    @Test
    void shouldRejectUnknownCategoriesWithoutSavingProduct() {
        ProductCreateRequest request = validRequest();
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(categoryRepository.findAllById(List.of(CATEGORY_ID))).thenReturn(List.of());

        assertThrows(CategoryNotFoundException.class,
                () -> productService.createProduct(request, SELLER_EMAIL));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectNonSellerAndNotSaveProduct() {
        ProductCreateRequest request = validRequest();
        User client = org.mockito.Mockito.mock(User.class);
        when(client.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(client));

        assertThrows(AccessDeniedException.class,
                () -> productService.createProduct(request, CLIENT_EMAIL));
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectMissingAuthenticatedSellerWithoutSelectingAnySeller() {
        ProductCreateRequest request = validRequest();

        assertThrows(AccessDeniedException.class,
                () -> productService.createProduct(request, null));
        verify(userRepository, never()).findAll();
        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldReturnSummaryForAuthenticatedSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(productRepository.countBySellerId(SELLER_ID)).thenReturn(12L);
        when(productRepository.countBySellerIdAndStock(SELLER_ID, 0)).thenReturn(2L);
        when(productRepository.countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
                SELLER_ID, 0, 5)).thenReturn(3L);

        ProductSummaryResponse result = productService.getSummary(SELLER_EMAIL);

        assertEquals(12L, result.totalProducts());
        assertEquals(2L, result.outOfStockProducts());
        assertEquals(3L, result.lowStockProducts());
        assertEquals(5, result.lowStockThreshold());
    }

    @Test
    void shouldReturnOnlyProductsBelongingToAuthenticatedSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(productRepository.findAllBySellerId(SELLER_ID)).thenReturn(List.of(
                new Product(
                        PRODUCT_NAME, List.of(CATEGORY_ID), 3, "Inalámbricos",
                        new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID)));

        List<ProductResponse> result = productService.getSellerProducts(SELLER_EMAIL);

        assertEquals(1, result.size());
        assertEquals(PRODUCT_NAME, result.get(0).name());
        assertEquals(3, result.get(0).stock());
        verify(productRepository).findAllBySellerId(SELLER_ID);
    }

    @Test
    void shouldUpdateOwnedProductAndPreserveItsSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 3, "Descripción anterior",
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(List.of(CATEGORY_ID)))
                .thenReturn(List.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));
        when(productRepository.save(product)).thenReturn(product);

        ProductCreateRequest request = validRequest();
        request.setName("  Nuevo nombre  ");
        request.setStock(8);
        request.setDescription("Nueva descripción");
        request.setPrice(new BigDecimal("25.50"));
        request.setAvailable(false);

        Product result = productService.updateProduct(PRODUCT_ID, request, SELLER_EMAIL);

        assertEquals("Nuevo nombre", result.getName());
        assertEquals(8, result.getStock());
        assertEquals("Nueva descripción", result.getDescription());
        assertEquals(new BigDecimal("25.50"), result.getPrice());
        assertFalse(result.isAvailable());
        assertEquals(SELLER_ID, result.getSellerId());
        verify(productRepository).save(product);
    }

    @Test
    void shouldHideProductsOwnedByAnotherSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 3, null, new BigDecimal(PRODUCT_PRICE),
                null, null, null, true, "another-seller")));

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProduct(PRODUCT_ID, SELLER_EMAIL));
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectUpdatingUnknownProduct() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(productRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(UNKNOWN_ID, validRequest(), SELLER_EMAIL));
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectNonSellerProductListRequest() {
        User client = org.mockito.Mockito.mock(User.class);
        when(client.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(client));

        assertThrows(AccessDeniedException.class,
                () -> productService.getSellerProducts(CLIENT_EMAIL));
        verify(productRepository, never()).findAllBySellerId(any());
    }

    @Test
    void shouldRejectNonSellerSummaryRequest() {
        User client = org.mockito.Mockito.mock(User.class);
        when(client.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(client));

        assertThrows(AccessDeniedException.class,
                () -> productService.getSummary(CLIENT_EMAIL));
        verify(productRepository, never()).countBySellerId(any());
    }

    @Test
    void shouldAllowAdminToQueryProductsFilteredBySeller() {
        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 10, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        product.setId(PRODUCT_ID);

        when(productRepository.findAllBySellerId(SELLER_ID)).thenReturn(List.of(product));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getFirstName()).thenReturn("Jane");
        when(seller.getLastName()).thenReturn("Doe");
        when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

        List<ProductSummaryDTO> results = productService.getProductsForAdmin(SELLER_ID, null, null);

        assertEquals(1, results.size());
        ProductSummaryDTO dto = results.get(0);
        assertEquals(PRODUCT_ID, dto.id());
        assertEquals(PRODUCT_NAME, dto.name());
        assertEquals(CATEGORY_NAME_ELECTRONICS, dto.category());
        assertEquals("Jane Doe", dto.sellerName());
        assertEquals(10, dto.stock());
        assertEquals(new BigDecimal(PRODUCT_PRICE), dto.price());
        assertTrue(dto.active());
    }

    @Test
    void shouldAllowAdminToQueryProductsFilteredByCategory() {
        Product p1 = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 10, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        Product p2 = new Product(
                "Libro", List.of("cat-books"), 5, "Desc libro",
                new BigDecimal("12.50"), null, null, null, true, SELLER_ID);

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));
        when(userRepository.findById(SELLER_ID)).thenReturn(Optional.empty());

        List<ProductSummaryDTO> results = productService.getProductsForAdmin(null, CATEGORY_ID, null);

        assertEquals(1, results.size());
        assertEquals(PRODUCT_NAME, results.get(0).name());
    }

    @Test
    void shouldAllowAdminToQueryProductsFilteredByStockStatus() {
        Product inStock = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 4, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        Product outOfStock = new Product(
                "Agotado", List.of(CATEGORY_ID), 0, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);

        when(productRepository.findAll()).thenReturn(List.of(inStock, outOfStock));

        List<ProductSummaryDTO> inStockResults = productService.getProductsForAdmin(null, null, true);
        assertEquals(1, inStockResults.size());
        assertEquals(PRODUCT_NAME, inStockResults.get(0).name());

        List<ProductSummaryDTO> outOfStockResults = productService.getProductsForAdmin(null, null, false);
        assertEquals(1, outOfStockResults.size());
        assertEquals("Agotado", outOfStockResults.get(0).name());
    }

    @Test
    void shouldAllowAdminToGetProductDetailById() {
        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 8, "Super descripción",
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        product.setId(PRODUCT_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

        User seller = org.mockito.Mockito.mock(User.class);
        SellerProfile profile = org.mockito.Mockito.mock(SellerProfile.class);
        when(profile.getBusinessName()).thenReturn("Audio Tech Store");
        when(seller.getSellerProfile()).thenReturn(profile);
        when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

        ProductDetailDTO detail = productService.getProductDetailForAdmin(PRODUCT_ID);

        assertEquals(PRODUCT_ID, detail.id());
        assertEquals(PRODUCT_NAME, detail.name());
        assertEquals("Super descripción", detail.description());
        assertEquals(CATEGORY_NAME_ELECTRONICS, detail.category());
        assertEquals("Audio Tech Store", detail.sellerName());
        assertEquals(8, detail.stock());
        assertEquals(new BigDecimal(PRODUCT_PRICE), detail.price());
        assertTrue(detail.active());
    }

    @Test
    void shouldThrowProductNotFoundWhenAdminGetsDetailOfNonExistentProduct() {
        when(productRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductDetailForAdmin(UNKNOWN_ID));
    }

    @Test
    void shouldRejectAdminAttemptingToCreateProduct() {
        ProductCreateRequest request = validRequest();
        User admin = org.mockito.Mockito.mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class,
                () -> productService.createProduct(request, ADMIN_EMAIL));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectAdminAttemptingToUpdateProduct() {
        ProductCreateRequest request = validRequest();
        User admin = org.mockito.Mockito.mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class,
                () -> productService.updateProduct(PRODUCT_ID, request, ADMIN_EMAIL));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectAdminAttemptingToDeleteProduct() {
        User admin = org.mockito.Mockito.mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class,
                () -> productService.deleteProduct(PRODUCT_ID, ADMIN_EMAIL));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void shouldAllowSellerToDeleteOwnedProduct() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 5, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        productService.deleteProduct(PRODUCT_ID, SELLER_EMAIL);

        verify(productRepository).delete(product);
    }

    @Test
    void shouldRejectSellerAttemptingToDeleteProductOwnedByAnotherSeller() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn(SELLER_ID);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        Product product = new Product(
                PRODUCT_NAME, List.of(CATEGORY_ID), 5, DESC_TEXT,
                new BigDecimal(PRODUCT_PRICE), null, null, null, true, OTHER_SELLER_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThrows(AccessDeniedException.class,
                () -> productService.deleteProduct(PRODUCT_ID, SELLER_EMAIL));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void shouldRejectDeletingNonExistentProduct() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(productRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(UNKNOWN_ID, SELLER_EMAIL));
        verify(productRepository, never()).delete(any(Product.class));
    }

    private ProductCreateRequest validRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(PRODUCT_NAME);
        request.setCategoryIds(List.of(CATEGORY_ID));
        request.setPrice(new BigDecimal(PRODUCT_PRICE));
        request.setAvailable(true);
        return request;
    }
}
