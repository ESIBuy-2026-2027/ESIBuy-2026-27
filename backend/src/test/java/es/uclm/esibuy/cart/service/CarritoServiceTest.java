package es.uclm.esibuy.cart.service;

import java.math.BigDecimal;
import java.util.Optional;

import es.uclm.esibuy.cart.dto.ActualizarCantidadItemDTO;
import es.uclm.esibuy.cart.dto.AnadirItemCarritoDTO;
import es.uclm.esibuy.cart.dto.CarritoDTO;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.model.ItemCarrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
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
class CarritoServiceTest {

    private static final String CLIENTE_ID = "cliente-1";
    private static final String PRODUCTO_ID = "producto-1";
    private static final String PRODUCTO_NOMBRE = "Auriculares";
    private static final BigDecimal PRECIO = new BigDecimal("19.99");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CarritoRepository carritoRepository;

    private CarritoService carritoService;

    @BeforeEach
    void setUp() {
        carritoService = new CarritoService(productRepository, carritoRepository);
    }

    @Test
    void shouldAddNewProductWhenAvailableStockIsSufficient() {
        Product product = availableProduct(5);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 2));

        assertEquals(CLIENTE_ID, result.clienteId());
        assertEquals(1, result.items().size());
        assertEquals(2, result.items().get(0).cantidad());
        assertEquals(new BigDecimal("39.98"), result.items().get(0).subtotal());
        assertEquals("https://example.com/auriculares.jpg", result.items().get(0).imageUrl());
        assertEquals(new BigDecimal("39.98"), result.precioTotal());
        verify(carritoRepository).save(any(Carrito.class));
    }

    @Test
    void shouldAccumulateQuantityForProductAlreadyInCart() {
        Product product = availableProduct(5);
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 2);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 3));

        assertEquals(1, result.items().size());
        assertEquals(5, result.items().get(0).cantidad());
        assertEquals(new BigDecimal("99.95"), result.items().get(0).subtotal());
        assertEquals(new BigDecimal("99.95"), result.precioTotal());
    }

    @Test
    void shouldRejectAccumulatedQuantityThatExceedsAvailableStock() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 3);
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(PRODUCTO_ID);
        when(product.getStock()).thenReturn(5);
        when(product.isAvailable()).thenReturn(true);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));

        assertThrows(
                IllegalStateException.class,
                () -> carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 3)));
    }

    @Test
    void shouldRejectQuantityLessThanOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 0)));
    }

    @Test
    void shouldRejectMissingProduct() {
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 1)));
    }

    @Test
    void shouldRejectInactiveProduct() {
        Product product = mock(Product.class);
        when(product.isAvailable()).thenReturn(false);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));

        assertThrows(
                IllegalStateException.class,
                () -> carritoService.anadirItem(CLIENTE_ID, new AnadirItemCarritoDTO(PRODUCTO_ID, 1)));
    }

    @Test
    void shouldIncreaseItemQuantityWhenStockIsSufficient() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 2);
        Product product = productWithStock(5);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.actualizarCantidadItem(
                CLIENTE_ID, PRODUCTO_ID, new ActualizarCantidadItemDTO(4));

        assertEquals(4, result.items().get(0).cantidad());
        assertEquals(new BigDecimal("79.96"), result.items().get(0).subtotal());
        assertEquals(new BigDecimal("79.96"), result.precioTotal());
        verify(carritoRepository).save(existingCart);
    }

    @Test
    void shouldRejectUpdatedQuantityThatExceedsAvailableStock() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 2);
        Product product = productWithStock(4);
        when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(product));
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));

        assertThrows(
                StockInsuficienteException.class,
                () -> carritoService.actualizarCantidadItem(
                        CLIENTE_ID, PRODUCTO_ID, new ActualizarCantidadItemDTO(5)));
    }

    @Test
    void shouldReduceItemQuantityAndRecalculateCartTotal() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 3);
        existingCart.addOrIncrementItem("producto-2", "Teclado", new BigDecimal("30.00"), 1);
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.actualizarCantidadItem(
                CLIENTE_ID, PRODUCTO_ID, new ActualizarCantidadItemDTO(1));

        assertEquals(1, result.items().get(0).cantidad());
        assertEquals(new BigDecimal("19.99"), result.items().get(0).subtotal());
        assertEquals(new BigDecimal("49.99"), result.precioTotal());
    }

    @Test
    void shouldRemoveItemAndSetTotalToZeroWhenQuantityIsZero() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 2);
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.actualizarCantidadItem(
                CLIENTE_ID, PRODUCTO_ID, new ActualizarCantidadItemDTO(0));

        assertEquals(0, result.items().size());
        assertEquals(BigDecimal.ZERO, result.precioTotal());
        verify(carritoRepository).save(existingCart);
    }

    @Test
    void shouldDeleteExistingItemAndRecalculateCartTotal() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem(PRODUCTO_ID, PRODUCTO_NOMBRE, PRECIO, 2);
        existingCart.addOrIncrementItem("producto-2", "Teclado", new BigDecimal("30.00"), 1);
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarritoDTO result = carritoService.eliminarItem(CLIENTE_ID, PRODUCTO_ID);

        assertEquals(1, result.items().size());
        assertEquals("producto-2", result.items().get(0).productoId());
        assertEquals(new BigDecimal("30.00"), result.items().get(0).subtotal());
        assertEquals(new BigDecimal("30.00"), result.precioTotal());
    }

    @Test
    void shouldRejectDeletingItemThatIsNotInCart() {
        Carrito existingCart = new Carrito(CLIENTE_ID);
        existingCart.addOrIncrementItem("producto-2", "Teclado", new BigDecimal("30.00"), 1);
        when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(existingCart));

        assertThrows(
                ProductNotFoundException.class,
                () -> carritoService.eliminarItem(CLIENTE_ID, PRODUCTO_ID));
    }

    private Product availableProduct(int stock) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(PRODUCTO_ID);
        when(product.getName()).thenReturn(PRODUCTO_NOMBRE);
        when(product.getPrice()).thenReturn(PRECIO);
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
