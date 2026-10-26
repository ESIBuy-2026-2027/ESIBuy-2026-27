package es.uclm.esibuy.products.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Optional;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.bson.types.ObjectId;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import es.uclm.esibuy.products.model.ProductDetails;
import es.uclm.esibuy.products.dto.ProductSummaryDTO;
import es.uclm.esibuy.products.dto.ProductDetailDTO;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

        private static final String SELLER_ID = "seller-id";
        private static final String SELLER_EMAIL = "seller@example.com";
        private static final String CLIENT_EMAIL = "client@example.com";
        private static final String CATEGORY_ID = "category-id";
        private static final String PRODUCT_NAME = "Auriculares";
        private static final String PRODUCT_PRICE = "19.99";
        private static final String OTHER_SELLER_ID = "other-seller-id";
        private static final String ADMIN_EMAIL = "admin@example.com";
        private static final String PRODUCT_ID = "product-id";
        private static final String CATEGORY_NAME_ELECTRONICS = "Electrónica";
        private static final String UNKNOWN_ID = "unknown";
        private static final String DESC_TEXT = "Descripción";

        @Mock
        private ProductRepository productRepository;

        @Mock
        private CategoryRepository categoryRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private GridFsTemplate gridFsTemplate;

        private ProductService productService;

        @BeforeEach
        void setUp() {
                productService = new ProductService(productRepository, categoryRepository, userRepository,
                                gridFsTemplate);
        }

        @Test
        void shouldRejectImageWhenMimeDoesNotMatchBinarySignature() {
                stubSellerAndOwnedProduct();
                MockMultipartFile image = new MockMultipartFile(
                                "image", "photo.png", "image/png", "not an image".getBytes());

                assertThrows(ResponseStatusException.class,
                                () -> productService.uploadProductImage("product-id", image, SELLER_EMAIL));
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldRejectImagesLargerThanFiveMegabytes() {
                stubSellerAndOwnedProduct();
                MockMultipartFile image = new MockMultipartFile(
                                "image", "photo.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

                assertThrows(ResponseStatusException.class,
                                () -> productService.uploadProductImage("product-id", image, SELLER_EMAIL));
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldNormalizeAndStoreValidPngImage() throws IOException {
                stubSellerAndOwnedProduct();
                BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(source, "png", bytes);
                MockMultipartFile image = new MockMultipartFile(
                                "image", "photo.png", "image/png", bytes.toByteArray());
                when(gridFsTemplate.store(any(java.io.InputStream.class), any(String.class), any(String.class)))
                                .thenReturn(new ObjectId("507f1f77bcf86cd799439011"));
                when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

                Product result = productService.uploadProductImage("product-id", image, SELLER_EMAIL);

                assertEquals("/api/products/product-id/image", result.getImageUrl());
                assertEquals("image/png", result.getImageContentType());
                org.junit.jupiter.api.Assertions.assertNotNull(result.getImageFileId());
                verify(productRepository).save(any(Product.class));
        }

        private void stubSellerAndOwnedProduct() {
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                1,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID));
                product.setId("product-id");
                when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        }

        @Test
        void shouldCreateProductForAuthenticatedSellerAndDefaultStockToZero() {
                ProductCreateRequest request = validRequest();
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
                when(categoryRepository.findAllById(List.of(CATEGORY_ID)))
                                .thenReturn(List.of(new Category(CATEGORY_ID, "Electr├│nica")));
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
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                3,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID));
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
                                .thenReturn(List.of(new Category(CATEGORY_ID, "Electr├│nica")));
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

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.createProduct(request, CLIENT_EMAIL));
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
                when(productRepository.findAllBySellerId(SELLER_ID)).thenReturn(List.of(new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                3,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID))));

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
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                1,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                SELLER_ID));
                when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
                when(categoryRepository.findAllById(List.of(CATEGORY_ID)))
                                .thenReturn(List.of(new Category(CATEGORY_ID, "Electr├│nica")));
                when(productRepository.save(product)).thenReturn(product);

                ProductCreateRequest request = validRequest();
                request.setName("  Nuevo nombre  ");
                request.setStock(8);
                request.setDescription("Nueva descripci├│n");
                request.setPrice(new BigDecimal("25.50"));
                request.setAvailable(false);

                Product result = productService.updateProduct("product-id", request, SELLER_EMAIL);

                assertEquals("Nuevo nombre", result.getName());
                assertEquals(8, result.getStock());
                assertEquals("Nueva descripci├│n", result.getDescription());
                assertEquals(new BigDecimal("25.50"), result.getPrice());
                assertEquals(false, result.isAvailable());
                assertEquals(SELLER_ID, result.getSellerId());
                verify(productRepository).save(product);
        }

        @Test
        void shouldHideProductsOwnedByAnotherSeller() {
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
                when(productRepository.findById("product-id")).thenReturn(Optional.of(new Product(new ProductDetails(
                                PRODUCT_NAME,
                                List.of(CATEGORY_ID),
                                1,
                                null,
                                new BigDecimal(PRODUCT_PRICE),
                                null,
                                null,
                                null,
                                true,
                                OTHER_SELLER_ID))));

                assertThrows(ProductNotFoundException.class,
                                () -> productService.getProduct("product-id", SELLER_EMAIL));
                verify(categoryRepository, never()).findAllById(any());
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldRejectUpdatingUnknownProduct() {
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));
                when(productRepository.findById("unknown")).thenReturn(Optional.empty());

                assertThrows(ProductNotFoundException.class,
                                () -> productService.updateProduct("unknown", validRequest(), SELLER_EMAIL));
                verify(categoryRepository, never()).findAllById(any());
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldRejectNonSellerProductListRequest() {
                User client = org.mockito.Mockito.mock(User.class);
                when(client.getRole()).thenReturn(UserRole.CLIENT);
                when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(client));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.getSellerProducts(CLIENT_EMAIL));
                verify(productRepository, never()).findAllBySellerId(any());
        }

        @Test
        void shouldRejectNonSellerSummaryRequest() {
                User client = org.mockito.Mockito.mock(User.class);
                when(client.getRole()).thenReturn(UserRole.CLIENT);
                when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(client));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.getSummary(CLIENT_EMAIL));
                verify(productRepository, never()).countBySellerId(any());
        }

        private ProductCreateRequest validRequest() {
                ProductCreateRequest request = new ProductCreateRequest();
                request.setName(PRODUCT_NAME);
                request.setCategoryIds(List.of(CATEGORY_ID));
                request.setPrice(new BigDecimal(PRODUCT_PRICE));
                request.setAvailable(true);
                return request;
        }

        @Test
        void shouldAllowAdminToQueryProductsFilteredBySeller() {
                Product product1 = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 10, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));

                when(productRepository.findAllBySellerId(SELLER_ID)).thenReturn(List.of(product1));
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getFirstName()).thenReturn("TechStore");
                when(seller.getLastName()).thenReturn("");
                when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
                when(categoryRepository.findById(CATEGORY_ID))
                                .thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

                List<ProductSummaryDTO> results = productService.getProductsForAdmin(SELLER_ID, null, null);

                assertEquals(1, results.size());
                assertEquals(PRODUCT_NAME, results.get(0).name());
                assertEquals("TechStore", results.get(0).sellerName());
                assertEquals(CATEGORY_NAME_ELECTRONICS, results.get(0).category());
        }

        @Test
        void shouldAllowAdminToQueryProductsFilteredByCategory() {
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 10, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));

                when(productRepository.findAll()).thenReturn(List.of(product));
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getFirstName()).thenReturn("TechStore");
                when(seller.getLastName()).thenReturn("");
                when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
                when(categoryRepository.findById(CATEGORY_ID))
                                .thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

                List<ProductSummaryDTO> results = productService.getProductsForAdmin(null, CATEGORY_ID, null);

                assertEquals(1, results.size());
                assertEquals(PRODUCT_NAME, results.get(0).name());
        }

        @Test
        void shouldAllowAdminToQueryProductsFilteredByStockStatus() {
                Product inStock = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 10, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));
                Product outOfStock = new Product(new ProductDetails(
                                "Agotado", List.of(CATEGORY_ID), 0, DESC_TEXT,
                                new BigDecimal("9.99"), null, null, null, true, SELLER_ID));

                when(productRepository.findAll()).thenReturn(List.of(inStock, outOfStock));
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getFirstName()).thenReturn("TechStore");
                when(seller.getLastName()).thenReturn("");
                when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
                when(categoryRepository.findById(CATEGORY_ID))
                                .thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

                List<ProductSummaryDTO> resultsInStock = productService.getProductsForAdmin(null, null, true);
                assertEquals(1, resultsInStock.size());
                assertEquals(PRODUCT_NAME, resultsInStock.get(0).name());

                List<ProductSummaryDTO> resultsOutOfStock = productService.getProductsForAdmin(null, null, false);
                assertEquals(1, resultsOutOfStock.size());
                assertEquals("Agotado", resultsOutOfStock.get(0).name());
        }

        @Test
        void shouldAllowAdminToGetProductDetailById() {
                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 15, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getFirstName()).thenReturn("TechStore");
                when(seller.getLastName()).thenReturn("");
                when(userRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
                when(categoryRepository.findById(CATEGORY_ID))
                                .thenReturn(Optional.of(new Category(CATEGORY_ID, CATEGORY_NAME_ELECTRONICS)));

                ProductDetailDTO detail = productService.getProductDetailForAdmin(PRODUCT_ID);

                assertEquals(PRODUCT_NAME, detail.name());
                assertEquals("TechStore", detail.sellerName());
                assertEquals(CATEGORY_NAME_ELECTRONICS, detail.category());
                assertEquals(15, detail.stock());
                assertEquals(new BigDecimal(PRODUCT_PRICE), detail.price());
        }

        @Test
        void shouldThrowProductNotFoundWhenAdminGetsDetailOfNonExistentProduct() {
                when(productRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

                assertThrows(ProductNotFoundException.class,
                                () -> productService.getProductDetailForAdmin(UNKNOWN_ID));
        }

        @Test
        void shouldRejectAdminAttemptingToCreateProduct() {
                User admin = org.mockito.Mockito.mock(User.class);
                when(admin.getRole()).thenReturn(UserRole.ADMIN);
                when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.createProduct(validRequest(), ADMIN_EMAIL));
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldRejectAdminAttemptingToUpdateProduct() {
                User admin = org.mockito.Mockito.mock(User.class);
                when(admin.getRole()).thenReturn(UserRole.ADMIN);
                when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.updateProduct(PRODUCT_ID, validRequest(), ADMIN_EMAIL));
                verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        void shouldRejectAdminAttemptingToDeleteProduct() {
                User admin = org.mockito.Mockito.mock(User.class);
                when(admin.getRole()).thenReturn(UserRole.ADMIN);
                when(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                                () -> productService.deleteProduct(PRODUCT_ID, ADMIN_EMAIL));
                verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        void shouldAllowSellerToDeleteOwnedProduct() {
                User seller = org.mockito.Mockito.mock(User.class);
                when(seller.getId()).thenReturn(SELLER_ID);
                when(seller.getRole()).thenReturn(UserRole.SELLER);
                when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(seller));

                Product product = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 5, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, SELLER_ID));
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

                Product otherProduct = new Product(new ProductDetails(
                                PRODUCT_NAME, List.of(CATEGORY_ID), 5, DESC_TEXT,
                                new BigDecimal(PRODUCT_PRICE), null, null, null, true, OTHER_SELLER_ID));
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(otherProduct));

                assertThrows(org.springframework.security.access.AccessDeniedException.class,
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
}