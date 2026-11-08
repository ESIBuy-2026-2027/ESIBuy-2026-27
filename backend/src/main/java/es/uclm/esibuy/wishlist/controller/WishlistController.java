package es.uclm.esibuy.wishlist.controller;

import java.util.List;
import java.util.Map;

import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.wishlist.dto.WishlistDTO;
import es.uclm.esibuy.wishlist.dto.WishlistRequest;
import es.uclm.esibuy.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private static final String IN_WISHLIST_KEY = "inWishlist";
    private static final String MESSAGE_KEY = "message";

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/lists")
    public List<WishlistDTO> getWishlists(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return wishlistService.getUserWishlists(email);
    }

    @PostMapping("/lists")
    public ResponseEntity<WishlistDTO> createWishlist(
            @Valid @RequestBody WishlistRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        WishlistDTO created = wishlistService.createWishlist(request.name(), email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/lists/{listId}")
    public ResponseEntity<WishlistDTO> updateWishlist(
            @PathVariable String listId,
            @Valid @RequestBody WishlistRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        WishlistDTO updated = wishlistService.updateWishlistName(listId, request.name(), email);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/lists/{listId}")
    public ResponseEntity<Map<String, String>> deleteWishlist(
            @PathVariable String listId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.deleteWishlist(listId, email);
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Lista de deseos eliminada"));
    }

    @GetMapping("/lists/{listId}/products")
    public List<ProductResponse> getWishlistProducts(
            @PathVariable String listId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return wishlistService.getWishlistProducts(listId, email);
    }

    @PostMapping("/lists/{listId}/products/{productId}")
    public ResponseEntity<Map<String, Object>> addProductToList(
            @PathVariable String listId,
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.addProductToWishlist(listId, productId, email);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Producto añadido a la lista de deseos",
                IN_WISHLIST_KEY, true));
    }

    @DeleteMapping("/lists/{listId}/products/{productId}")
    public ResponseEntity<Map<String, Object>> removeProductFromList(
            @PathVariable String listId,
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.removeProductFromWishlist(listId, productId, email);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Producto eliminado de la lista de deseos",
                IN_WISHLIST_KEY, false));
    }

    @GetMapping
    public List<ProductResponse> getWishlist(
            @RequestParam(required = false) String listId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (listId != null && !listId.isBlank()) {
            return wishlistService.getWishlistProducts(listId, email);
        }
        return wishlistService.getUserWishlist(email);
    }

    @GetMapping("/ids")
    public List<String> getWishlistIds(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return wishlistService.getWishlistProductIds(email);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> addToWishlist(
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.addToWishlist(productId, email);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Producto añadido a la lista de deseos",
                IN_WISHLIST_KEY, true));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.removeFromWishlist(productId, email);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Producto eliminado de la lista de deseos",
                IN_WISHLIST_KEY, false));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkWishlist(
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        boolean inWishlist = wishlistService.isInWishlist(productId, email);
        return ResponseEntity.ok(Map.of(IN_WISHLIST_KEY, inWishlist));
    }
}
