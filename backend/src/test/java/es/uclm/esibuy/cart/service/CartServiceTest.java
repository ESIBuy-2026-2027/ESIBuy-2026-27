package es.uclm.esibuy.cart.service;

import java.math.BigDecimal;
import java.util.Optional;

import es.uclm.esibuy.cart.dto.AddCartItemDTO;
import es.uclm.esibuy.cart.dto.CartDTO;
import es.uclm.esibuy.cart.dto.UpdateCartItemQuantityDTO;
import es.uclm.esibuy.cart.model.Cart;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

        private static final String CUSTOMER_ID = "cliente-1";
        private static final String PRODUCT_ID = "producto-1";
        private static final String PRODUCT_NAME = "Auriculares";
        private static final BigDecimal PRICE = new BigDecimal("19.99");

        @Mock
        private ProductRepository productRepository;

        @Mock
        private CartRepository cartRepository;

        private CartService cartService;

        @BeforeEach
        void setUp() {
                cartService = new CartService(productRepository, cartRepository);
        }

        @Test
        void shouldAddNewProductWhenAvailableStockIsSufficient() {
                Product product = availableProduct(5);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.addItem(
                                CUSTOMER_ID,
                                new AddCartItemDTO(PRODUCT_ID, 2));

                assertEquals(CUSTOMER_ID, result.customerId());
                assertEquals(1, result.items().size());
                assertEquals(2, result.items().get(0).quantity());
                assertEquals(new BigDecimal("39.98"), result.items().get(0).subtotal());
                assertEquals("https://example.com/auriculares.jpg", result.items().get(0).imageUrl());
                assertEquals(new BigDecimal("39.98"), result.totalPrice());

                verify(cartRepository).save(any(Cart.class));
        }

        @Test
        void shouldAccumulateQuantityForProductAlreadyInCart() {
                Product product = availableProduct(5);
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 2);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.addItem(
                                CUSTOMER_ID,
                                new AddCartItemDTO(PRODUCT_ID, 3));

                assertEquals(1, result.items().size());
                assertEquals(5, result.items().get(0).quantity());
                assertEquals(new BigDecimal("99.95"), result.items().get(0).subtotal());
                assertEquals(new BigDecimal("99.95"), result.totalPrice());
        }

        @Test
        void shouldRejectAccumulatedQuantityThatExceedsAvailableStock() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 3);

                Product product = mock(Product.class);
                when(product.getId()).thenReturn(PRODUCT_ID);
                when(product.getStock()).thenReturn(5);
                when(product.isAvailable()).thenReturn(true);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));

                assertThrows(
                                StockInsuficienteException.class,
                                () -> cartService.addItem(
                                                CUSTOMER_ID,
                                                new AddCartItemDTO(PRODUCT_ID, 3)));
        }

        @Test
        void shouldRejectQuantityLessThanOne() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> cartService.addItem(
                                                CUSTOMER_ID,
                                                new AddCartItemDTO(PRODUCT_ID, 0)));
        }

        @Test
        void shouldRejectMissingProduct() {
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

                assertThrows(
                                ProductNotFoundException.class,
                                () -> cartService.addItem(
                                                CUSTOMER_ID,
                                                new AddCartItemDTO(PRODUCT_ID, 1)));
        }

        @Test
        void shouldRejectInactiveProduct() {
                Product product = mock(Product.class);
                when(product.isAvailable()).thenReturn(false);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

                assertThrows(
                                ProductoNoDisponibleException.class,
                                () -> cartService.addItem(
                                                CUSTOMER_ID,
                                                new AddCartItemDTO(PRODUCT_ID, 1)));
        }

        @Test
        void shouldIncreaseItemQuantityWhenStockIsSufficient() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 2);

                Product product = productWithStock(5);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.updateItemQuantity(
                                CUSTOMER_ID,
                                PRODUCT_ID,
                                new UpdateCartItemQuantityDTO(4));

                assertEquals(4, result.items().get(0).quantity());
                assertEquals(new BigDecimal("79.96"), result.items().get(0).subtotal());
                assertEquals(new BigDecimal("79.96"), result.totalPrice());

                verify(cartRepository).save(existingCart);
        }

        @Test
        void shouldRejectUpdatedQuantityThatExceedsAvailableStock() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 2);

                Product product = productWithStock(4);

                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));

                assertThrows(
                                StockInsuficienteException.class,
                                () -> cartService.updateItemQuantity(
                                                CUSTOMER_ID,
                                                PRODUCT_ID,
                                                new UpdateCartItemQuantityDTO(5)));
        }

        @Test
        void shouldReduceItemQuantityAndRecalculateCartTotal() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 3);
                existingCart.addOrIncrementItem(
                                "producto-2",
                                "Teclado",
                                new BigDecimal("30.00"),
                                1);

                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.updateItemQuantity(
                                CUSTOMER_ID,
                                PRODUCT_ID,
                                new UpdateCartItemQuantityDTO(1));

                assertEquals(1, result.items().get(0).quantity());
                assertEquals(new BigDecimal("19.99"), result.items().get(0).subtotal());
                assertEquals(new BigDecimal("49.99"), result.totalPrice());
        }

        @Test
        void shouldRemoveItemAndSetTotalToZeroWhenQuantityIsZero() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 2);

                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.updateItemQuantity(
                                CUSTOMER_ID,
                                PRODUCT_ID,
                                new UpdateCartItemQuantityDTO(0));

                assertEquals(0, result.items().size());
                assertEquals(BigDecimal.ZERO, result.totalPrice());

                verify(cartRepository).save(existingCart);
        }

        @Test
        void shouldDeleteExistingItemAndRecalculateCartTotal() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(PRODUCT_ID, PRODUCT_NAME, PRICE, 2);
                existingCart.addOrIncrementItem(
                                "producto-2",
                                "Teclado",
                                new BigDecimal("30.00"),
                                1);

                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));
                when(cartRepository.save(any(Cart.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CartDTO result = cartService.removeItem(CUSTOMER_ID, PRODUCT_ID);

                assertEquals(1, result.items().size());
                assertEquals("producto-2", result.items().get(0).productId());
                assertEquals(new BigDecimal("30.00"), result.items().get(0).subtotal());
                assertEquals(new BigDecimal("30.00"), result.totalPrice());
        }

        @Test
        void shouldRejectDeletingItemThatIsNotInCart() {
                Cart existingCart = new Cart(CUSTOMER_ID);
                existingCart.addOrIncrementItem(
                                "producto-2",
                                "Teclado",
                                new BigDecimal("30.00"),
                                1);

                when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existingCart));

                assertThrows(
                                ProductNotFoundException.class,
                                () -> cartService.removeItem(CUSTOMER_ID, PRODUCT_ID));
        }

        private Product availableProduct(int stock) {
                Product product = mock(Product.class);

                when(product.getId()).thenReturn(PRODUCT_ID);
                when(product.getName()).thenReturn(PRODUCT_NAME);
                when(product.getPrice()).thenReturn(PRICE);
                when(product.getStock()).thenReturn(stock);
                when(product.isAvailable()).thenReturn(true);
                when(product.getImageUrl()).thenReturn("https://example.com/auriculares.jpg");

                return product;
        }

        private Product productWithStock(int stock) {
                Product product = mock(Product.class);

                when(product.getStock()).thenReturn(stock);
                when(product.isAvailable()).thenReturn(true);

                return product;
        }
}