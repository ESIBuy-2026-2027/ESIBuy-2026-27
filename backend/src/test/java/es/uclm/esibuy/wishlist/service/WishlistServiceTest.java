package es.uclm.esibuy.wishlist.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.DuplicateWishlistException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.WishlistNotFoundException;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.model.ProductDetails;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.wishlist.dto.WishlistDTO;
import es.uclm.esibuy.wishlist.model.Wishlist;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String PRODUCT_ID_1 = "product-1";
    private static final String PRODUCT_ID_2 = "product-2";
    private static final String WISHLIST_ID_1 = "list-1";

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User clientUser;

    @Mock
    private User sellerUser;

    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistService(wishlistRepository, productRepository, userRepository);
    }

    private void mockClient() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
    }

    @Test
    void shouldGetUserWishlistsSuccessfully() {
        mockClient();
        Wishlist w1 = new Wishlist(CLIENT_ID, "Favoritos");
        w1.setId(WISHLIST_ID_1);
        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of(w1));

        List<WishlistDTO> result = wishlistService.getUserWishlists(CLIENT_EMAIL);

        assertEquals(1, result.size());
        assertEquals("Favoritos", result.get(0).name());
    }

    @Test
    void shouldCreateDefaultWishlistWhenUserHasNone() {
        mockClient();
        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of());
        Wishlist created = new Wishlist(CLIENT_ID, WishlistService.DEFAULT_WISHLIST_NAME);
        created.setId("default-id");
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(created);

        List<WishlistDTO> result = wishlistService.getUserWishlists(CLIENT_EMAIL);

        assertEquals(1, result.size());
        assertEquals(WishlistService.DEFAULT_WISHLIST_NAME, result.get(0).name());
    }

    @Test
    void shouldCreateNewWishlistSuccessfully() {
        mockClient();
        when(wishlistRepository.existsByUserIdAndNameIgnoreCase(CLIENT_ID, "Tecnología")).thenReturn(false);
        Wishlist saved = new Wishlist(CLIENT_ID, "Tecnología");
        saved.setId("tech-id");
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(saved);

        WishlistDTO result = wishlistService.createWishlist("Tecnología", CLIENT_EMAIL);

        assertNotNull(result);
        assertEquals("Tecnología", result.name());
    }

    @Test
    void shouldRejectCreatingDuplicateWishlistName() {
        mockClient();
        when(wishlistRepository.existsByUserIdAndNameIgnoreCase(CLIENT_ID, "Tecnología")).thenReturn(true);

        assertThrows(DuplicateWishlistException.class,
                () -> wishlistService.createWishlist("Tecnología", CLIENT_EMAIL));
    }

    @Test
    void shouldRejectInvalidWishlistNames() {
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));

        assertThrows(IllegalArgumentException.class, () -> wishlistService.createWishlist("", CLIENT_EMAIL));
        assertThrows(IllegalArgumentException.class, () -> wishlistService.createWishlist("a", CLIENT_EMAIL));
        assertThrows(IllegalArgumentException.class,
                () -> wishlistService.createWishlist("a".repeat(51), CLIENT_EMAIL));
    }

    @Test
    void shouldUpdateWishlistNameSuccessfully() {
        mockClient();
        Wishlist existing = new Wishlist(CLIENT_ID, "Antiguo");
        existing.setId(WISHLIST_ID_1);
        when(wishlistRepository.findByIdAndUserId(WISHLIST_ID_1, CLIENT_ID)).thenReturn(Optional.of(existing));
        when(wishlistRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(CLIENT_ID, "Nuevo Nombre", WISHLIST_ID_1))
                .thenReturn(false);
        when(wishlistRepository.save(existing)).thenReturn(existing);

        WishlistDTO updated = wishlistService.updateWishlistName(WISHLIST_ID_1, "Nuevo Nombre", CLIENT_EMAIL);

        assertEquals("Nuevo Nombre", updated.name());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingWishlist() {
        mockClient();
        when(wishlistRepository.findByIdAndUserId("none", CLIENT_ID)).thenReturn(Optional.empty());

        assertThrows(WishlistNotFoundException.class,
                () -> wishlistService.updateWishlistName("none", "Nuevo", CLIENT_EMAIL));
    }

    @Test
    void shouldDeleteWishlistSuccessfully() {
        mockClient();
        Wishlist existing = new Wishlist(CLIENT_ID, "A borrar");
        existing.setId(WISHLIST_ID_1);
        when(wishlistRepository.findByIdAndUserId(WISHLIST_ID_1, CLIENT_ID)).thenReturn(Optional.of(existing));

        wishlistService.deleteWishlist(WISHLIST_ID_1, CLIENT_EMAIL);

        verify(wishlistRepository).delete(existing);
    }

    @Test
    void shouldGetWishlistProductsSuccessfully() {
        mockClient();
        Wishlist wishlist = new Wishlist(CLIENT_ID, "Favoritos");
        wishlist.setId(WISHLIST_ID_1);
        wishlist.getProductIds().add(PRODUCT_ID_1);
        when(wishlistRepository.findByIdAndUserId(WISHLIST_ID_1, CLIENT_ID)).thenReturn(Optional.of(wishlist));

        Product product = new Product(new ProductDetails(
                "Laptop", List.of("cat1"), 5, "Desc", new BigDecimal("999.99"),
                null, null, null, true, "seller1"));
        when(productRepository.findById(PRODUCT_ID_1)).thenReturn(Optional.of(product));

        List<ProductResponse> products = wishlistService.getWishlistProducts(WISHLIST_ID_1, CLIENT_EMAIL);

        assertEquals(1, products.size());
        assertEquals("Laptop", products.get(0).name());
    }

    @Test
    void shouldGetWishlistProductIdsUnionAcrossAllLists() {
        mockClient();
        Wishlist w1 = new Wishlist(CLIENT_ID, "L1");
        w1.getProductIds().addAll(List.of(PRODUCT_ID_1, PRODUCT_ID_2));
        Wishlist w2 = new Wishlist(CLIENT_ID, "L2");
        w2.getProductIds().addAll(List.of(PRODUCT_ID_2));

        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of(w1, w2));

        List<String> ids = wishlistService.getWishlistProductIds(CLIENT_EMAIL);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(PRODUCT_ID_1));
        assertTrue(ids.contains(PRODUCT_ID_2));
    }

    @Test
    void shouldAddToWishlistDefaultSuccessfully() {
        mockClient();
        when(productRepository.existsById(PRODUCT_ID_1)).thenReturn(true);
        Wishlist defaultList = new Wishlist(CLIENT_ID, "Mi lista");
        defaultList.setProductIds(new ArrayList<>());
        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of(defaultList));

        wishlistService.addToWishlist(PRODUCT_ID_1, CLIENT_EMAIL);

        assertTrue(defaultList.getProductIds().contains(PRODUCT_ID_1));
        verify(wishlistRepository).save(defaultList);
    }

    @Test
    void shouldAddProductToSpecificWishlistSuccessfully() {
        mockClient();
        when(productRepository.existsById(PRODUCT_ID_1)).thenReturn(true);
        Wishlist wishlist = new Wishlist(CLIENT_ID, "Específica");
        wishlist.setId(WISHLIST_ID_1);
        when(wishlistRepository.findByIdAndUserId(WISHLIST_ID_1, CLIENT_ID)).thenReturn(Optional.of(wishlist));

        wishlistService.addProductToWishlist(WISHLIST_ID_1, PRODUCT_ID_1, CLIENT_EMAIL);

        assertTrue(wishlist.getProductIds().contains(PRODUCT_ID_1));
        verify(wishlistRepository).save(wishlist);
    }

    @Test
    void shouldRemoveFromAllWishlists() {
        mockClient();
        Wishlist w1 = new Wishlist(CLIENT_ID, "L1");
        w1.getProductIds().add(PRODUCT_ID_1);
        Wishlist w2 = new Wishlist(CLIENT_ID, "L2");
        w2.getProductIds().add(PRODUCT_ID_1);

        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of(w1, w2));

        wishlistService.removeFromWishlist(PRODUCT_ID_1, CLIENT_EMAIL);

        assertFalse(w1.getProductIds().contains(PRODUCT_ID_1));
        assertFalse(w2.getProductIds().contains(PRODUCT_ID_1));
        verify(wishlistRepository).saveAll(List.of(w1, w2));
    }

    @Test
    void shouldRemoveProductFromSpecificWishlist() {
        mockClient();
        Wishlist wishlist = new Wishlist(CLIENT_ID, "L1");
        wishlist.setId(WISHLIST_ID_1);
        wishlist.getProductIds().add(PRODUCT_ID_1);
        when(wishlistRepository.findByIdAndUserId(WISHLIST_ID_1, CLIENT_ID)).thenReturn(Optional.of(wishlist));

        wishlistService.removeProductFromWishlist(WISHLIST_ID_1, PRODUCT_ID_1, CLIENT_EMAIL);

        assertFalse(wishlist.getProductIds().contains(PRODUCT_ID_1));
        verify(wishlistRepository).save(wishlist);
    }

    @Test
    void shouldCheckIfInWishlist() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
        Wishlist w = new Wishlist(CLIENT_ID, "L1");
        w.getProductIds().add(PRODUCT_ID_1);
        when(wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(CLIENT_ID)).thenReturn(List.of(w));

        assertTrue(wishlistService.isInWishlist(PRODUCT_ID_1, CLIENT_EMAIL));
        assertFalse(wishlistService.isInWishlist("unknown", CLIENT_EMAIL));
        assertFalse(wishlistService.isInWishlist(PRODUCT_ID_1, null));
    }

    @Test
    void shouldRejectNonClientFromWishlist() {
        when(sellerUser.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(sellerUser));

        assertThrows(AccessDeniedException.class, () -> wishlistService.getUserWishlists(SELLER_EMAIL));
    }
}
