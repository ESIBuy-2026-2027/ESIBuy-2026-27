package es.uclm.esibuy.wishlist.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import es.uclm.esibuy.common.exception.DuplicateWishlistException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.WishlistNotFoundException;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.wishlist.dto.WishlistDTO;
import es.uclm.esibuy.wishlist.model.Wishlist;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class WishlistService {

    public static final String DEFAULT_WISHLIST_NAME = "Mi lista de deseos";
    private static final String WISHLIST_NOT_FOUND_MESSAGE = "Lista de deseos no encontrada";

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

    public List<WishlistDTO> getUserWishlists(String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        if (lists.isEmpty()) {
            Wishlist defaultList = wishlistRepository.save(new Wishlist(user.getId(), DEFAULT_WISHLIST_NAME));
            lists = List.of(defaultList);
        }
        return lists.stream().map(WishlistDTO::from).toList();
    }

    public WishlistDTO createWishlist(String name, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        String trimmed = validateName(name);

        if (wishlistRepository.existsByUserIdAndNameIgnoreCase(user.getId(), trimmed)) {
            throw new DuplicateWishlistException("Ya tienes una lista de deseos con ese nombre");
        }

        Wishlist wishlist = new Wishlist(user.getId(), trimmed);
        Wishlist saved = wishlistRepository.save(wishlist);
        return WishlistDTO.from(saved);
    }

    public WishlistDTO updateWishlistName(String listId, String newName, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        String trimmed = validateName(newName);

        Wishlist wishlist = wishlistRepository.findByIdAndUserId(listId, user.getId())
                .orElseThrow(() -> new WishlistNotFoundException(WISHLIST_NOT_FOUND_MESSAGE));

        if (wishlistRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(user.getId(), trimmed, listId)) {
            throw new DuplicateWishlistException("Ya tienes una lista de deseos con ese nombre");
        }

        wishlist.setName(trimmed);
        Wishlist saved = wishlistRepository.save(wishlist);
        return WishlistDTO.from(saved);
    }

    public void deleteWishlist(String listId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        Wishlist wishlist = wishlistRepository.findByIdAndUserId(listId, user.getId())
                .orElseThrow(() -> new WishlistNotFoundException(WISHLIST_NOT_FOUND_MESSAGE));

        wishlistRepository.delete(wishlist);
    }

    public List<ProductResponse> getWishlistProducts(String listId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        Wishlist wishlist = wishlistRepository.findByIdAndUserId(listId, user.getId())
                .orElseThrow(() -> new WishlistNotFoundException(WISHLIST_NOT_FOUND_MESSAGE));

        return mapProducts(wishlist.getProductIds());
    }

    public List<ProductResponse> getUserWishlist(String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        if (lists.isEmpty()) {
            return List.of();
        }
        return mapProducts(lists.get(0).getProductIds());
    }

    public List<String> getWishlistProductIds(String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        Set<String> allIds = new LinkedHashSet<>();
        for (Wishlist list : lists) {
            allIds.addAll(list.getProductIds());
        }
        return new ArrayList<>(allIds);
    }

    public void addToWishlist(String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        validateProductExists(productId);

        List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        Wishlist target;
        if (lists.isEmpty()) {
            target = wishlistRepository.save(new Wishlist(user.getId(), DEFAULT_WISHLIST_NAME));
        } else {
            target = lists.get(0);
        }

        if (!target.getProductIds().contains(productId)) {
            target.getProductIds().add(productId);
            wishlistRepository.save(target);
        }
    }

    public void addProductToWishlist(String listId, String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        validateProductExists(productId);

        Wishlist wishlist = wishlistRepository.findByIdAndUserId(listId, user.getId())
                .orElseThrow(() -> new WishlistNotFoundException(WISHLIST_NOT_FOUND_MESSAGE));

        if (!wishlist.getProductIds().contains(productId)) {
            wishlist.getProductIds().add(productId);
            wishlistRepository.save(wishlist);
        }
    }

    public void removeFromWishlist(String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        boolean modified = false;
        for (Wishlist list : lists) {
            if (list.getProductIds().remove(productId)) {
                modified = true;
            }
        }
        if (modified) {
            wishlistRepository.saveAll(lists);
        }
    }

    public void removeProductFromWishlist(String listId, String productId, String authenticatedEmail) {
        User user = findClient(authenticatedEmail);
        Wishlist wishlist = wishlistRepository.findByIdAndUserId(listId, user.getId())
                .orElseThrow(() -> new WishlistNotFoundException(WISHLIST_NOT_FOUND_MESSAGE));

        if (wishlist.getProductIds().remove(productId)) {
            wishlistRepository.save(wishlist);
        }
    }

    public boolean isInWishlist(String productId, String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .map(user -> {
                    List<Wishlist> lists = wishlistRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
                    return lists.stream().anyMatch(w -> w.getProductIds().contains(productId));
                })
                .orElse(false);
    }

    private String validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la lista de deseos es obligatorio");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 50 caracteres");
        }
        return trimmed;
    }

    private void validateProductExists(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Producto no encontrado");
        }
    }

    private List<ProductResponse> mapProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<ProductResponse> products = new ArrayList<>();
        for (String pid : productIds) {
            Optional<Product> productOpt = productRepository.findById(pid);
            productOpt.ifPresent(p -> products.add(ProductResponse.from(p)));
        }
        return products;
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
