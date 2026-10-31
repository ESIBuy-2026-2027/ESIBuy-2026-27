
package es.uclm.esibuy.orders.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.mongodb.client.result.UpdateResult;
import es.uclm.esibuy.cart.model.Cart;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.common.exception.PedidoNotFoundException;
import es.uclm.esibuy.common.exception.RecursoCheckoutNoEncontradoException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.orders.dto.CreateOrderDTO;
import es.uclm.esibuy.orders.dto.OrderConfirmationDTO;
import es.uclm.esibuy.orders.dto.OrderStatusHistoryDTO;
import es.uclm.esibuy.orders.dto.SellerOrderDTO;
import es.uclm.esibuy.orders.dto.ShippingTrackingDTO;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderItem;
import es.uclm.esibuy.orders.model.OrderPaymentMethod;
import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderShippingAddress;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.products.model.ProductDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        private static final String CUSTOMER_ID = "cliente-1";
        private static final String PRODUCT_ID = "producto-1";
        private static final BigDecimal PRICE = new BigDecimal("19.99");

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

        private OrderService orderService;
        private Cart cart;

        @BeforeEach
        void setUp() {
                orderService = new OrderService(
                                userRepository,
                                cartRepository,
                                productRepository,
                                orderRepository,
                                mongoTemplate,
                                paymentGateway);

                User customer = new User(
                                "cliente@example.com",
                                "Ana",
                                "López",
                                UserRole.CLIENT);

                ClientProfile profile = new ClientProfile(null, CustomerType.STANDARD);

                profile.setAddresses(List.of(new ShippingAddress(
                                "direccion-1",
                                "Ana López",
                                "Calle Mayor 1",
                                null,
                                "13001",
                                "Ciudad Real",
                                "Ciudad Real",
                                "600123456",
                                true)));

                profile.setPaymentMethods(List.of(PaymentMethod.card(
                                "pago-1",
                                "Visa",
                                "Ana López",
                                "VISA",
                                "1234",
                                12,
                                2030,
                                true)));

                customer.setClientProfile(profile);

                lenient().when(userRepository.findById(CUSTOMER_ID))
                                .thenReturn(Optional.of(customer));

                cart = new Cart(CUSTOMER_ID);
                cart.addOrIncrementItem(PRODUCT_ID, "Auriculares", PRICE, 2);
        }

        @Test
        void shouldChargeCreatePaidPendingOrderDeductStockAndEmptyCart() {
                prepareCartAndProduct(productWithStock(5));
                enableStockReservation();

                when(paymentGateway.charge(any(), any())).thenReturn(true);

                Order savedOrder = org.mockito.Mockito.mock(Order.class);

                when(savedOrder.getId()).thenReturn("pedido-123");
                when(savedOrder.getTotalPrice()).thenReturn(new BigDecimal("39.98"));
                when(savedOrder.getStatus()).thenReturn(OrderStatus.PENDIENTE);
                when(savedOrder.getPaymentStatus()).thenReturn(OrderPaymentStatus.PAGADO);
                when(savedOrder.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());

                when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

                OrderConfirmationDTO result = orderService.createOrder(
                                CUSTOMER_ID,
                                new CreateOrderDTO("direccion-1", "pago-1"));

                assertEquals("pedido-123", result.orderId());
                assertEquals(new BigDecimal("39.98"), result.totalPrice());
                assertEquals(OrderStatus.PENDIENTE, result.status());
                assertEquals(OrderPaymentStatus.PAGADO, result.paymentStatus());

                verify(mongoTemplate).findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(Product.class));

                ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

                verify(orderRepository).save(orderCaptor.capture());

                assertEquals(OrderStatus.PENDIENTE, orderCaptor.getValue().getStatus());
                assertEquals(OrderPaymentStatus.PAGADO, orderCaptor.getValue().getPaymentStatus());

                verify(cartRepository).save(cart);

                assertEquals(List.of(), cart.getItems());
                assertEquals(BigDecimal.ZERO, cart.getTotalPrice());
        }

        @Test
        void shouldAllowReturningFromShippedToPreparingWithSelectedReason() {
                Product product = productWithStock(4);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                product, "id", PRODUCT_ID);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                product, "sellerId", "seller-1");

                Order order = org.mockito.Mockito.mock(Order.class);

                when(order.getItems()).thenReturn(List.of(new OrderItem(
                                PRODUCT_ID,
                                "Auriculares",
                                1,
                                PRICE,
                                PRICE,
                                null)));

                when(orderRepository.findById("pedido-123"))
                                .thenReturn(Optional.of(order));

                when(productRepository.findAllBySellerId("seller-1"))
                                .thenReturn(List.of(product));

                orderService.updateOrderStatus(
                                "seller-1",
                                "pedido-123",
                                OrderStatus.PREPARANDO,
                                "Incidencia durante la preparación");

                verify(order).setStatus(OrderStatus.PREPARANDO);
                verify(orderRepository).save(order);
        }

        @Test
        void shouldReturnChronologicalTrackingHistoryOnlyForTheOwningClient() {
                Order order = new Order(
                                CUSTOMER_ID,
                                List.of(),
                                BigDecimal.ZERO,
                                new OrderShippingAddress(
                                                "direccion-1",
                                                "Ana López",
                                                "Calle Mayor 1",
                                                null,
                                                "13001",
                                                "Ciudad Real",
                                                "Ciudad Real",
                                                "600123456"),
                                new OrderPaymentMethod("pago-1", null, "1234"),
                                OrderStatus.PAGADO);

                order.setStatus(OrderStatus.PREPARANDO);
                order.setStatus(OrderStatus.ENVIADO);
                order.setStatus(OrderStatus.ENVIADO);

                when(orderRepository.findByIdAndCustomerId("pedido-123", CUSTOMER_ID))
                                .thenReturn(Optional.of(order));

                ShippingTrackingDTO tracking = orderService.getShippingStatusHistory(
                                CUSTOMER_ID,
                                "pedido-123");

                assertEquals(
                                List.of(
                                                OrderStatus.PAGADO,
                                                OrderStatus.PREPARANDO,
                                                OrderStatus.ENVIADO),
                                tracking.statusHistory().stream()
                                                .map(OrderStatusHistoryDTO::status)
                                                .toList());

                assertEquals(order.getEstimatedDeliveryDate(), tracking.estimatedDeliveryDate());

                verify(orderRepository).findByIdAndCustomerId("pedido-123", CUSTOMER_ID);
        }

        @Test
        void shouldClearEstimatedDeliveryDateWhenShipmentReturnsToPreparing() {
                Order order = new Order(
                                CUSTOMER_ID,
                                List.of(),
                                BigDecimal.ZERO,
                                new OrderShippingAddress(
                                                "direccion-1",
                                                "Ana López",
                                                "Calle Mayor 1",
                                                null,
                                                "13001",
                                                "Ciudad Real",
                                                "Ciudad Real",
                                                "600123456"),
                                new OrderPaymentMethod("pago-1", null, "1234"),
                                OrderStatus.PREPARANDO);

                order.setStatus(OrderStatus.ENVIADO);

                assertNotNull(order.getEstimatedDeliveryDate());

                order.setStatus(OrderStatus.PREPARANDO);

                assertNull(order.getEstimatedDeliveryDate());
        }

        @Test
        void shouldHideTrackingHistoryWhenOrderDoesNotBelongToTheClient() {
                when(orderRepository.findByIdAndCustomerId("pedido-123", CUSTOMER_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PedidoNotFoundException.class,
                                () -> orderService.getShippingStatusHistory(CUSTOMER_ID, "pedido-123"));
        }

        @Test
        void shouldRejectReturningToPreviousStatusWithoutJustification() {
                Product product = productWithStock(4);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                product, "id", PRODUCT_ID);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                product, "sellerId", "seller-1");

                Order order = org.mockito.Mockito.mock(Order.class);

                when(order.getStatus()).thenReturn(OrderStatus.ENVIADO);

                when(orderRepository.findById("pedido-123"))
                                .thenReturn(Optional.of(order));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.updateOrderStatus(
                                                "seller-1",
                                                "pedido-123",
                                                OrderStatus.PREPARANDO,
                                                null));

                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void shouldRejectStockChangedAfterCheckoutSummaryWithoutChargingOrSavingOrder() {
                Product firstProduct = productWithStock(5);
                Product secondProduct = productWithStock(5);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                secondProduct, "id", "producto-2");

                prepareCartAndProduct(firstProduct);

                cart.addOrIncrementItem("producto-2", "Ratón", PRICE, 1);

                when(productRepository.findById("producto-2"))
                                .thenReturn(Optional.of(secondProduct));

                when(mongoTemplate.findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(Product.class)))
                                .thenReturn(firstProduct)
                                .thenReturn(null);

                enableStockRelease();

                assertThrows(
                                StockInsuficienteException.class,
                                () -> orderService.createOrder(
                                                CUSTOMER_ID,
                                                new CreateOrderDTO("direccion-1", "pago-1")));

                verify(mongoTemplate).updateFirst(
                                any(Query.class),
                                any(Update.class),
                                eq(Product.class));

                verify(paymentGateway, never()).charge(any(), any());
                verify(orderRepository, never()).save(any(Order.class));

                assertEquals(2, cart.getItems().get(0).getQuantity());
                assertEquals(2, cart.getItems().size());
        }

        @Test
        void shouldRestoreReservedStockAndKeepCartWhenPaymentFails() {
                prepareCartAndProduct(productWithStock(5));

                enableStockReservation();
                enableStockRelease();

                when(paymentGateway.charge(any(), any())).thenReturn(false);

                assertThrows(
                                PagoFallidoException.class,
                                () -> orderService.createOrder(
                                                CUSTOMER_ID,
                                                new CreateOrderDTO("direccion-1", "pago-1")));

                verify(orderRepository, never()).save(any(Order.class));
                verify(cartRepository, never()).save(cart);

                verify(mongoTemplate).updateFirst(
                                any(Query.class),
                                any(Update.class),
                                eq(Product.class));

                assertEquals(2, cart.getItems().get(0).getQuantity());
        }

        @Test
        void shouldRejectInitiallyInsufficientStockWithoutReservingOrCharging() {
                prepareCartAndProduct(productWithStock(1));

                assertThrows(
                                StockInsuficienteException.class,
                                () -> orderService.createOrder(
                                                CUSTOMER_ID,
                                                new CreateOrderDTO("direccion-1", "pago-1")));

                verify(mongoTemplate, never()).findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(Product.class));

                verify(paymentGateway, never()).charge(any(), any());
        }

        @Test
        void shouldRejectOrderWhenCartContainsProductsFromMultipleSellers() {
                cart.addOrIncrementItem(
                                "producto-2",
                                "Teclado",
                                new BigDecimal("49.99"),
                                1);

                Product firstProduct = productWithStock(5);

                Product secondProduct = new Product(new ProductDetails(
                                "Teclado",
                                List.of("categoria-1"),
                                5,
                                "Periférico",
                                new BigDecimal("49.99"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                true,
                                "vendedor-2"));

                org.springframework.test.util.ReflectionTestUtils.setField(
                                secondProduct, "id", "producto-2");

                when(cartRepository.findByCustomerId(CUSTOMER_ID))
                                .thenReturn(Optional.of(cart));

                when(productRepository.findById(PRODUCT_ID))
                                .thenReturn(Optional.of(firstProduct));

                when(productRepository.findById("producto-2"))
                                .thenReturn(Optional.of(secondProduct));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.createOrder(
                                                CUSTOMER_ID,
                                                new CreateOrderDTO("direccion-1", "pago-1")));

                assertEquals(
                                "No se puede realizar un pedido con productos de diferentes vendedores. "
                                                + "Debes realizar un pedido independiente por cada vendedor.",
                                exception.getMessage());

                verify(orderRepository, never()).save(any(Order.class));
                verify(paymentGateway, never()).charge(any(), any());
        }

        @Test
        void shouldRejectAddressOrPaymentMethodNotOwnedByClient() {
                assertThrows(
                                RecursoCheckoutNoEncontradoException.class,
                                () -> orderService.createOrder(
                                                CUSTOMER_ID,
                                                new CreateOrderDTO("direccion-ajena", "pago-1")));

                verify(paymentGateway, never()).charge(any(), any());
        }

        private void enableStockReservation() {
                Product product = productWithStock(5);

                when(mongoTemplate.findAndModify(
                                any(Query.class),
                                any(Update.class),
                                any(FindAndModifyOptions.class),
                                eq(Product.class)))
                                .thenReturn(product);
        }

        private void prepareCartAndProduct(Product product) {
                when(cartRepository.findByCustomerId(CUSTOMER_ID))
                                .thenReturn(Optional.of(cart));

                when(productRepository.findById(PRODUCT_ID))
                                .thenReturn(Optional.of(product));
        }

        private void enableStockRelease() {
                UpdateResult result = org.mockito.Mockito.mock(UpdateResult.class);

                when(result.wasAcknowledged()).thenReturn(true);
                when(result.getMatchedCount()).thenReturn(1L);

                when(mongoTemplate.updateFirst(
                                any(Query.class),
                                any(Update.class),
                                eq(Product.class)))
                                .thenReturn(result);
        }

        private Product productWithStock(int stock) {
                Product product = new Product(new ProductDetails(
                                "Auriculares",
                                List.of("categoria-1"),
                                stock,
                                "Audio",
                                PRICE,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                true,
                                "vendedor-1"));

                org.springframework.test.util.ReflectionTestUtils.setField(
                                product, "id", PRODUCT_ID);

                return product;
        }

        @Test
        void shouldKeepSellerOrderHistoryAfterSellerProductsAreDeleted() {
                String sellerId = "seller-1";

                OrderItem historicalItem = new OrderItem(
                                PRODUCT_ID,
                                "Auriculares",
                                2,
                                PRICE,
                                new BigDecimal("39.98"),
                                "https://example.com/auriculares.jpg",
                                sellerId);

                Order order = new Order(
                                CUSTOMER_ID,
                                List.of(historicalItem),
                                new BigDecimal("39.98"),
                                new OrderShippingAddress(
                                                "direccion-1",
                                                "Ana López",
                                                "Calle Mayor 1",
                                                null,
                                                "13001",
                                                "Ciudad Real",
                                                "Ciudad Real",
                                                "600123456"),
                                new OrderPaymentMethod("pago-1", null, "1234"),
                                OrderStatus.ENVIADO);

                org.springframework.test.util.ReflectionTestUtils.setField(
                                order, "id", "pedido-historico-1");

                when(productRepository.findAllBySellerId(sellerId))
                                .thenReturn(List.of());

                when(orderRepository.findAll())
                                .thenReturn(List.of(order));

                List<SellerOrderDTO> result = orderService.getSellerOrders(sellerId);

                assertEquals(1, result.size());
                assertEquals("pedido-historico-1", result.get(0).orderId());
                assertEquals(OrderStatus.ENVIADO, result.get(0).status());

                assertEquals(1, result.get(0).items().size());
                assertEquals(PRODUCT_ID, result.get(0).items().get(0).productId());
                assertEquals("Auriculares", result.get(0).items().get(0).name());

                assertEquals(new BigDecimal("39.98"), result.get(0).total());

                verify(productRepository).findAllBySellerId(sellerId);
                verify(orderRepository).findAll();
        }

        @Test
        void shouldAllowSellerToUpdateHistoricalOrderAfterProductsAreDeleted() {
                String sellerId = "seller-1";

                OrderItem historicalItem = new OrderItem(
                                PRODUCT_ID,
                                "Auriculares",
                                2,
                                PRICE,
                                new BigDecimal("39.98"),
                                "https://example.com/auriculares.jpg",
                                sellerId);

                Order order = new Order(
                                CUSTOMER_ID,
                                List.of(historicalItem),
                                new BigDecimal("39.98"),
                                new OrderShippingAddress(
                                                "direccion-1",
                                                "Ana López",
                                                "Calle Mayor 1",
                                                null,
                                                "13001",
                                                "Ciudad Real",
                                                "Ciudad Real",
                                                "600123456"),
                                new OrderPaymentMethod("pago-1", null, "1234"),
                                OrderStatus.PREPARANDO);

                when(orderRepository.findById("pedido-historico-1"))
                                .thenReturn(Optional.of(order));

                // El vendedor ya no tiene productos asociados.
                when(productRepository.findAllBySellerId(sellerId))
                                .thenReturn(List.of());

                orderService.updateOrderStatus(
                                sellerId,
                                "pedido-historico-1",
                                OrderStatus.ENVIADO,
                                null);

                assertEquals(OrderStatus.ENVIADO, order.getStatus());

                verify(orderRepository).save(order);
        }

        @Test
        void shouldHideDeletedCustomerEmailInSellerOrderHistory() {
                String sellerId = "seller-1";

                User deletedCustomer = new User(
                                "cliente.eliminado@esibuy.local",
                                "Cliente",
                                "eliminado",
                                UserRole.CLIENT);

                OrderItem historicalItem = new OrderItem(
                                PRODUCT_ID,
                                "Auriculares",
                                1,
                                PRICE,
                                PRICE,
                                "https://example.com/auriculares.jpg",
                                sellerId);

                Order order = new Order(
                                CUSTOMER_ID,
                                List.of(historicalItem),
                                PRICE,
                                new OrderShippingAddress(
                                                "direccion-1",
                                                "Ana López",
                                                "Calle Mayor 1",
                                                null,
                                                "13001",
                                                "Ciudad Real",
                                                "Ciudad Real",
                                                "600123456"),
                                new OrderPaymentMethod("pago-1", null, "1234"),
                                OrderStatus.ENTREGADO);

                // Simula los datos que quedan después de eliminar la cuenta del cliente.
                order.anonymizeCustomerData();

                org.springframework.test.util.ReflectionTestUtils.setField(
                                order, "id", "pedido-historico-2");
                when(productRepository.findAllBySellerId(sellerId))
                                .thenReturn(List.of());

                when(orderRepository.findAll())
                                .thenReturn(List.of(order));

                org.mockito.Mockito.doReturn(Optional.of(deletedCustomer))
                                .when(userRepository).findById(CUSTOMER_ID);

                List<SellerOrderDTO> result = orderService.getSellerOrders(sellerId);

                assertEquals(1, result.size());
                assertEquals("Cliente eliminado", result.get(0).customerName());
                assertNull(result.get(0).customerEmail());

                assertEquals(1, result.get(0).items().size());
                assertEquals(PRICE, result.get(0).total());
        }

}
