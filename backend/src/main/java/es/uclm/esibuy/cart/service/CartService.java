package es.uclm.esibuy.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import es.uclm.esibuy.cart.dto.AddCartItemDTO;
import es.uclm.esibuy.cart.dto.CartDTO;
import es.uclm.esibuy.cart.dto.CartItemDTO;
import es.uclm.esibuy.cart.dto.UpdateCartItemQuantityDTO;
import es.uclm.esibuy.cart.model.Cart;
import es.uclm.esibuy.cart.model.CartItem;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private static final String CUSTOMER_ID_REQUIRED = "El identificador del cliente es obligatorio";

    private static final String PRODUCT_ID_REQUIRED = "El identificador del producto es obligatorio";

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    @Autowired
    public CartService(
            ProductRepository productRepository,
            CartRepository cartRepository,
            UserRepository userRepository) {

        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    public CartService(
            ProductRepository productRepository,
            CartRepository cartRepository) {

        this(productRepository, cartRepository, null);
    }

    public CartDTO addItem(String customerId, AddCartItemDTO request) {
        validateRequest(customerId, request);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        validateProductAvailability(product);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new Cart(customerId));

        validateStock(cart, product, request.quantity());

        boolean isPremium = isPremiumCustomer(customerId);
        BigDecimal effectivePrice = calculateEffectivePrice(product, isPremium);

        cart.addOrIncrementItem(
                product.getId(),
                product.getName(),
                effectivePrice,
                request.quantity(),
                product.getImageUrl());

        return toCartDTO(cartRepository.save(cart));
    }

    public CartDTO getCart(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(CUSTOMER_ID_REQUIRED);
        }

        return cartRepository.findByCustomerId(customerId)
                .map(this::toCartDTO)
                .orElseGet(() -> toCartDTO(new Cart(customerId)));
    }

    public CartDTO updateItemQuantity(
            String customerId,
            String productId,
            UpdateCartItemQuantityDTO request) {

        validateUpdateRequest(customerId, productId, request);

        Cart cart = getExistingCart(customerId);
        CartItem item = findCartItem(cart, productId);

        if (request.quantity() > item.getQuantity()) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(ProductNotFoundException::new);

            validateProductAvailability(product);
            validateStock(product, request.quantity());
        }

        cart.updateItemQuantity(productId, request.quantity());

        return toCartDTO(cartRepository.save(cart));
    }

    public CartDTO removeItem(String customerId, String productId) {
        validateCustomerAndProduct(customerId, productId);

        Cart cart = getExistingCart(customerId);

        if (!cart.removeItem(productId)) {
            throw new ProductNotFoundException();
        }

        return toCartDTO(cartRepository.save(cart));
    }

    private void validateRequest(String customerId, AddCartItemDTO request) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(CUSTOMER_ID_REQUIRED);
        }

        if (request == null || request.productId() == null || request.productId().isBlank()) {
            throw new IllegalArgumentException(PRODUCT_ID_REQUIRED);
        }

        if (request.quantity() == null || request.quantity() < 1) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 1");
        }
    }

    private void validateUpdateRequest(
            String customerId,
            String productId,
            UpdateCartItemQuantityDTO request) {

        validateCustomerAndProduct(customerId, productId);

        if (request == null || request.quantity() == null || request.quantity() < 0) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 0");
        }
    }

    private void validateCustomerAndProduct(String customerId, String productId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(CUSTOMER_ID_REQUIRED);
        }

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(PRODUCT_ID_REQUIRED);
        }
    }

    private Cart getExistingCart(String customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private CartItem findCartItem(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(ProductNotFoundException::new);
    }

    private void validateProductAvailability(Product product) {
        if (!product.isAvailable()) {
            throw new ProductoNoDisponibleException();
        }
    }

    private void validateStock(Cart cart, Product product, int requestedQuantity) {
        int currentQuantity = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .mapToInt(CartItem::getQuantity)
                .findFirst()
                .orElse(0);

        validateStock(product, (long) currentQuantity + requestedQuantity);
    }

    private void validateStock(Product product, long requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new StockInsuficienteException();
        }
    }

    private CartDTO toCartDTO(Cart cart) {
        boolean isPremium = isPremiumCustomer(cart.getCustomerId());

        List<CartItemDTO> items = cart.getItems().stream()
                .map(item -> toCartItemDTO(item, isPremium))
                .toList();

        BigDecimal totalBeforeDiscount = items.stream()
                .map(item -> (item.originalPrice() != null
                        ? item.originalPrice()
                        : item.unitPrice())
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPrice = cart.getTotalPrice();

        BigDecimal totalDiscount = totalBeforeDiscount.compareTo(totalPrice) > 0
                ? totalBeforeDiscount.subtract(totalPrice)
                : BigDecimal.ZERO;

        return new CartDTO(
                cart.getId(),
                cart.getCustomerId(),
                items,
                totalPrice,
                totalBeforeDiscount,
                totalDiscount);
    }

    private CartItemDTO toCartItemDTO(CartItem item, boolean isPremium) {
        String imageUrl = item.getImageUrl();

        BigDecimal originalPrice = item.getUnitPrice();
        BigDecimal discount = BigDecimal.ZERO;

        boolean isPremiumDiscount = false;

        String sellerId = null;
        String sellerName = null;

        if (item.getProductId() != null) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);

            if (product != null) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = product.getImageUrl();
                }

                sellerId = product.getSellerId();
                sellerName = resolveSellerName(sellerId);

                originalPrice = product.getPrice();

                if (isPremium
                        && product.getPremiumDiscount() != null
                        && product.getPremiumDiscount().compareTo(BigDecimal.ZERO) > 0) {

                    discount = product.getPremiumDiscount();
                    isPremiumDiscount = true;

                } else if (product.getDiscount() != null
                        && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {

                    discount = product.getDiscount();
                }
            }
        }

        return new CartItemDTO(
                item.getProductId(),
                item.getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                imageUrl,
                sellerId,
                sellerName,
                originalPrice,
                discount,
                isPremiumDiscount);
    }

    private String resolveSellerName(String sellerId) {
        if (sellerId == null || sellerId.isBlank() || userRepository == null) {
            return null;
        }

        return userRepository.findById(sellerId)
                .map(user -> {
                    if (user.getSellerProfile() != null
                            && user.getSellerProfile().getBusinessName() != null
                            && !user.getSellerProfile().getBusinessName().isBlank()) {

                        return user.getSellerProfile().getBusinessName();
                    }

                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";

                    String fullName = (firstName + " " + lastName).trim();

                    return !fullName.isBlank()
                            ? fullName
                            : "Vendedor " + sellerId;
                })
                .orElse("Vendedor " + sellerId);
    }

    private boolean isPremiumCustomer(String customerId) {
        if (customerId == null || userRepository == null) {
            return false;
        }

        return userRepository.findById(customerId)
                .map(user -> user.getClientProfile() != null
                        && CustomerType.PREMIUM.equals(
                                user.getClientProfile().getCustomerType()))
                .orElse(false);
    }

    private BigDecimal calculateEffectivePrice(Product product, boolean isPremium) {
        BigDecimal price = product.getPrice();
        BigDecimal discount = BigDecimal.ZERO;

        if (isPremium
                && product.getPremiumDiscount() != null
                && product.getPremiumDiscount().compareTo(BigDecimal.ZERO) > 0) {

            discount = product.getPremiumDiscount();

        } else if (product.getDiscount() != null
                && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {

            discount = product.getDiscount();
        }

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factor = BigDecimal.valueOf(100)
                    .subtract(discount)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            return price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }
}