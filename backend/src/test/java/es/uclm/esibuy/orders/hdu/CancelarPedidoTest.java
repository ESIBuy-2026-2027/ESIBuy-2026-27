package es.uclm.esibuy.orders.hdu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import es.uclm.esibuy.common.exception.PedidoNotFoundException;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderItem;
import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.orders.service.OrderService;
import es.uclm.esibuy.orders.service.PaymentGateway;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CancelarPedidoTest {

    private static final String CUSTOMER_ID = "cliente-1";
    private static final String ORDER_ID = "pedido-123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private Order order;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                userRepository,
                cartRepository,
                productRepository,
                orderRepository,
                mongoTemplate,
                paymentGateway);

        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(createCustomer()));
        when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(order));
    }

    private User createCustomer() {
        User customer = new User(
                "cliente@example.com",
                "Ana",
                "López",
                UserRole.CLIENT);
        customer.setClientProfile(new ClientProfile(null, CustomerType.STANDARD));
        return customer;
    }

    @Test
    void unClientePuedeCancelarSuPedidoPendientePagado() {
        // Arrange
        when(order.getStatus()).thenReturn(OrderStatus.PENDIENTE);
        when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.PAGADO);

        // Act
        orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID);

        // Assert
        verify(order).setStatus(OrderStatus.CANCELADO);
        verify(orderRepository).save(order);
    }

    @Test
    void noSePuedeCancelarUnPedidoEnviado() {
        // Arrange
        when(order.getStatus()).thenReturn(OrderStatus.ENVIADO);

        // Act and Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID));

        // Assert
        verify(orderRepository, never()).save(order);
        assertEquals(
                "El pedido ya no se puede cancelar en su estado actual",
                exception.getMessage());
    }

    @Test
    void alCancelarSeReponeElStockDeLosProductos() {
        // Arrange
        when(order.getItems()).thenReturn(List.of(createOrderItem()));
        when(order.getStatus()).thenReturn(OrderStatus.PENDIENTE);
        when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.PAGADO);

        Product product = org.mockito.Mockito.mock(Product.class);
        when(productRepository.findById("producto-1")).thenReturn(Optional.of(product));
        when(product.getStock()).thenReturn(3);

        // Act
        orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID);

        // Assert
        verify(product).setStock(5);
        verify(productRepository).save(product);
    }

    @Test
    void siNoSePuedeReponerElStockNoSeCancelaElPedido() {
        // Arrange
        when(order.getItems()).thenReturn(List.of(createOrderItem()));
        when(order.getStatus()).thenReturn(OrderStatus.PENDIENTE);
        when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.PAGADO);
        when(productRepository.findById("producto-1")).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID));

        // Assert
        verify(orderRepository, never()).save(order);
        verify(order, never()).setStatus(OrderStatus.CANCELADO);
    }

    @Test
    void unClienteNoPuedeCancelarUnPedidoDeOtroCliente() {
        // Arrange
        when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                PedidoNotFoundException.class,
                () -> orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID));

        // Assert
        verify(orderRepository, never()).save(order);
        verify(order, never()).setStatus(OrderStatus.CANCELADO);
    }

    @Test
    void noSePuedeCancelarUnPedidoInexistente() {
        // Arrange
        when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                PedidoNotFoundException.class,
                () -> orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID));

        // Assert
        verify(orderRepository, never()).save(order);
        verify(order, never()).setStatus(OrderStatus.CANCELADO);
    }

    @Test
    void noSePuedeCancelarUnPedidoConPagoPendiente() {
        // Arrange
        when(order.getStatus()).thenReturn(OrderStatus.PENDIENTE);
        when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.PENDIENTE);

        // Act and Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.cancelCustomerOrder(CUSTOMER_ID, ORDER_ID));

        // Assert
        assertEquals(
                "El pedido ya no se puede cancelar en su estado actual",
                exception.getMessage());
        verify(orderRepository, never()).save(order);
        verify(order, never()).setStatus(OrderStatus.CANCELADO);
    }

    private OrderItem createOrderItem() {
        return new OrderItem(
                "producto-1",
                "Producto de prueba",
                2,
                BigDecimal.TEN,
                BigDecimal.valueOf(20));
    }
}
