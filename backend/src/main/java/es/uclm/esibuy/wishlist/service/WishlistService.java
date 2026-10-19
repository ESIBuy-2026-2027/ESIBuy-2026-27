package es.uclm.esibuy.wishlist.service;

import java.util.ArrayList;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<ProductResponse> getUserWishlist(String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        List<WishlistItem> items = wishlistRepository.findAllByUserIdOrderByAddedAtDesc(user.getId());

        List<ProductResponse> products = new ArrayList<>();
        for (WishlistItem item : items) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            productOpt.ifPresent(p -> products.add(ProductResponse.from(p)));
        }
        return products;
    }

    public List<String> getWishlistProductIds(String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        return wishlistRepository.findAllByUserIdOrderByAddedAtDesc(user.getId()).stream()
                .map(WishlistItem::getProductId)
                .toList();
    }

    public void addToWishlist(String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Producto no encontrado");
        }
        if (!wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            wishlistRepository.save(new WishlistItem(user.getId(), productId));
        }
    }

    public void removeFromWishlist(String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public boolean isInWishlist(String productId, String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .map(user -> wishlistRepository.existsByUserIdAndProductId(user.getId(), productId))
                .orElse(false);
    }

    private User findClient(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Autenticación requerida");
        }
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
        if (user.getRole() != UserRole.CLIENT) {
            throw new AccessDeniedException("Solo los clientes pueden gestionar su lista de deseos");
        }
        return user;
    }
}

