package es.uclm.esibuy.wishlist.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.wishlist.model.WishlistItem;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
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
class WishlistServiceTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String PRODUCT_ID = "product-1";

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

    @Test
    void shouldGetUserWishlistSuccessfully() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));

        WishlistItem item = new WishlistItem(CLIENT_ID, PRODUCT_ID);
        when(wishlistRepository.findAllByUserIdOrderByAddedAtDesc(CLIENT_ID)).thenReturn(List.of(item));

        Product product = new Product("Laptop", List.of("cat1"), 5, "Desc", new BigDecimal("999.99"), null, null, null, true, "seller1");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        List<ProductResponse> wishlist = wishlistService.getUserWishlist(CLIENT_EMAIL);

        assertEquals(1, wishlist.size());
        assertEquals("Laptop", wishlist.get(0).name());
    }

    @Test
    void shouldGetWishlistProductIdsSuccessfully() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));

        WishlistItem item = new WishlistItem(CLIENT_ID, PRODUCT_ID);
        when(wishlistRepository.findAllByUserIdOrderByAddedAtDesc(CLIENT_ID)).thenReturn(List.of(item));

        List<String> ids = wishlistService.getWishlistProductIds(CLIENT_EMAIL);

        assertEquals(1, ids.size());
        assertEquals(PRODUCT_ID, ids.get(0));
    }

    @Test
    void shouldAddToWishlistSuccessfully() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(wishlistRepository.existsByUserIdAndProductId(CLIENT_ID, PRODUCT_ID)).thenReturn(false);

        wishlistService.addToWishlist(PRODUCT_ID, CLIENT_EMAIL);

        verify(wishlistRepository).save(any(WishlistItem.class));
    }

    @Test
    void shouldNotAddDuplicateToWishlist() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(wishlistRepository.existsByUserIdAndProductId(CLIENT_ID, PRODUCT_ID)).thenReturn(true);

        wishlistService.addToWishlist(PRODUCT_ID, CLIENT_EMAIL);

        verify(wishlistRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void shouldThrowProductNotFoundWhenAddingNonExistingProduct() {
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () ->
                wishlistService.addToWishlist(PRODUCT_ID, CLIENT_EMAIL));
    }

    @Test
    void shouldRemoveFromWishlistSuccessfully() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getRole()).thenReturn(UserRole.CLIENT);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));

        wishlistService.removeFromWishlist(PRODUCT_ID, CLIENT_EMAIL);

        verify(wishlistRepository).deleteByUserIdAndProductId(CLIENT_ID, PRODUCT_ID);
    }

    @Test
    void shouldCheckIfInWishlist() {
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(userRepository.findByEmailIgnoreCase(CLIENT_EMAIL)).thenReturn(Optional.of(clientUser));
        when(wishlistRepository.existsByUserIdAndProductId(CLIENT_ID, PRODUCT_ID)).thenReturn(true);

        assertTrue(wishlistService.isInWishlist(PRODUCT_ID, CLIENT_EMAIL));
        assertFalse(wishlistService.isInWishlist(PRODUCT_ID, null));
    }

    @Test
    void shouldRejectNonClientFromWishlist() {
        when(sellerUser.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findByEmailIgnoreCase(SELLER_EMAIL)).thenReturn(Optional.of(sellerUser));

        assertThrows(AccessDeniedException.class, () ->
                wishlistService.getUserWishlist(SELLER_EMAIL));
    }
}

