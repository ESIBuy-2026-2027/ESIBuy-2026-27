package es.uclm.esibuy.wishlist.controller;

import java.util.List;
import java.util.Map;

import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.wishlist.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private static final String IN_WISHLIST_KEY = "inWishlist";

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<ProductResponse> getWishlist(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
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
                "message", "Producto añadido a la lista de deseos",
                IN_WISHLIST_KEY, true));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @PathVariable String productId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        wishlistService.removeFromWishlist(productId, email);
        return ResponseEntity.ok(Map.of(
                "message", "Producto eliminado de la lista de deseos",
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
