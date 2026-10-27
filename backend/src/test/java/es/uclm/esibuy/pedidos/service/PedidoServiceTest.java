package es.uclm.esibuy.pedidos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.mongodb.client.result.UpdateResult;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.common.exception.RecursoCheckoutNoEncontradoException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.pedidos.dto.CrearPedidoDTO;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.model.Pedido;
import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.model.PedidoItem;
import es.uclm.esibuy.pedidos.repository.PedidoRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import es.uclm.esibuy.products.model.ProductDetails;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

        private static final String CLIENTE_ID = "cliente-1";
        private static final String PRODUCTO_ID = "producto-1";
        private static final BigDecimal PRECIO = new BigDecimal("19.99");

        @Mock
        private UserRepository userRepository;
        @Mock
        private CarritoRepository carritoRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private PedidoRepository pedidoRepository;
        @Mock
        private MongoTemplate mongoTemplate;
        @Mock
        private PasarelaPago pasarelaPago;

        private PedidoService pedidoService;
        private Carrito carrito;

        @BeforeEach
        void setUp() {
                pedidoService = new PedidoService(
                                userRepository, carritoRepository, productRepository, pedidoRepository, mongoTemplate,
                                pasarelaPago);
                User cliente = new User("cliente@example.com", "Ana", "López", UserRole.CLIENT);
                ClientProfile profile = new ClientProfile(null, CustomerType.STANDARD);
                profile.setAddresses(List.of(new ShippingAddress(
                                "direccion-1", "Ana López", "Calle Mayor 1", null, "13001", "Ciudad Real",
                                "Ciudad Real", "600123456", true)));
                profile.setPaymentMethods(List.of(PaymentMethod.card(
                                "pago-1", "Visa", "Ana López", "VISA", "1234", 12, 2030, true)));
                cliente.setClientProfile(profile);
                lenient().when(userRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

                carrito = new Carrito(CLIENTE_ID);
                carrito.addOrIncrementItem(PRODUCTO_ID, "Auriculares", PRECIO, 2);
        }

        @Test
        void shouldChargeCreatePaidOrderDeductStockAndEmptyCart() {
                prepararCarritoYProducto(productoConStock(5));
                habilitarReservaStock();
                when(pasarelaPago.cobrar(any(), any())).thenReturn(true);
                Pedido guardado = org.mockito.Mockito.mock(Pedido.class);
                when(guardado.getId()).thenReturn("pedido-123");
                when(guardado.getPrecioTotal()).thenReturn(new BigDecimal("39.98"));
                when(guardado.getEstado()).thenReturn(PedidoEstado.PAGADO);
                when(guardado.getFechaCreacion()).thenReturn(java.time.LocalDateTime.now());
                when(pedidoRepository.save(any(Pedido.class))).thenReturn(guardado);

                PedidoConfirmadoDTO resultado = pedidoService.crearPedido(
                                CLIENTE_ID, new CrearPedidoDTO("direccion-1", "pago-1"));

                assertEquals("pedido-123", resultado.pedidoId());
                assertEquals(new BigDecimal("39.98"), resultado.precioTotal());
                assertEquals(PedidoEstado.PAGADO, resultado.estado());
                verify(mongoTemplate).findAndModify(
                                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                                eq(Product.class));
                verify(pedidoRepository).save(any(Pedido.class));
                verify(carritoRepository).save(carrito);
                assertEquals(List.of(), carrito.getItems());
                assertEquals(BigDecimal.ZERO, carrito.getPrecioTotal());
        }

        @Test
        void shouldAllowReturningFromShippedToPreparingWithSelectedReason() {
                Product producto = productoConStock(4);
                org.springframework.test.util.ReflectionTestUtils.setField(producto, "id", PRODUCTO_ID);
                org.springframework.test.util.ReflectionTestUtils.setField(producto, "sellerId", "seller-1");
                Pedido pedido = org.mockito.Mockito.mock(Pedido.class);
                when(pedido.getItems()).thenReturn(List.of(new PedidoItem(
                                PRODUCTO_ID,
                                "Auriculares",
                                1,
                                PRECIO,
                                PRECIO,
                                null)));
                when(pedidoRepository.findById("pedido-123")).thenReturn(Optional.of(pedido));
                when(productRepository.findAllBySellerId("seller-1")).thenReturn(List.of(producto));

                pedidoService.actualizarEstadoPedido(
                                "seller-1", "pedido-123", PedidoEstado.PREPARANDO, "Incidencia durante la preparación");

                verify(pedido).setEstado(PedidoEstado.PREPARANDO);
                verify(pedidoRepository).save(pedido);
        }

        @Test
        void shouldRejectReturningToPreviousStatusWithoutJustification() {
                Product producto = productoConStock(4);
                org.springframework.test.util.ReflectionTestUtils.setField(producto, "id", PRODUCTO_ID);
                org.springframework.test.util.ReflectionTestUtils.setField(producto, "sellerId", "seller-1");
                Pedido pedido = org.mockito.Mockito.mock(Pedido.class);
                when(pedido.getEstado()).thenReturn(PedidoEstado.ENVIADO);
                when(pedidoRepository.findById("pedido-123")).thenReturn(Optional.of(pedido));

                assertThrows(IllegalArgumentException.class, () -> pedidoService.actualizarEstadoPedido(
                                "seller-1", "pedido-123", PedidoEstado.PREPARANDO, null));

                verify(pedidoRepository, never()).save(any(Pedido.class));
        }

        @Test
        void shouldRejectStockChangedAfterCheckoutSummaryWithoutChargingOrSavingOrder() {
                Product primero = productoConStock(5);
                Product segundo = productoConStock(5);
                org.springframework.test.util.ReflectionTestUtils.setField(segundo, "id", "producto-2");
                prepararCarritoYProducto(primero);
                carrito.addOrIncrementItem("producto-2", "Ratón", PRECIO, 1);
                when(productRepository.findById("producto-2")).thenReturn(Optional.of(segundo));
                when(mongoTemplate.findAndModify(
                                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                                eq(Product.class)))
                                .thenReturn(primero)
                                .thenReturn(null);
                habilitarLiberacionStock();

                assertThrows(StockInsuficienteException.class, () -> pedidoService.crearPedido(
                                CLIENTE_ID, new CrearPedidoDTO("direccion-1", "pago-1")));

                verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
                verify(pasarelaPago, never()).cobrar(any(), any());
                verify(pedidoRepository, never()).save(any(Pedido.class));
                assertEquals(2, carrito.getItems().get(0).getCantidad());
                assertEquals(2, carrito.getItems().size());
        }

        @Test
        void shouldRestoreReservedStockAndKeepCartWhenPaymentFails() {
                prepararCarritoYProducto(productoConStock(5));
                habilitarReservaStock();
                habilitarLiberacionStock();
                when(pasarelaPago.cobrar(any(), any())).thenReturn(false);

                assertThrows(PagoFallidoException.class, () -> pedidoService.crearPedido(
                                CLIENTE_ID, new CrearPedidoDTO("direccion-1", "pago-1")));

                verify(pedidoRepository, never()).save(any(Pedido.class));
                verify(carritoRepository, never()).save(carrito);
                verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
                assertEquals(2, carrito.getItems().get(0).getCantidad());
        }

        @Test
        void shouldRejectInitiallyInsufficientStockWithoutReservingOrCharging() {
                prepararCarritoYProducto(productoConStock(1));

                assertThrows(StockInsuficienteException.class, () -> pedidoService.crearPedido(
                                CLIENTE_ID, new CrearPedidoDTO("direccion-1", "pago-1")));

                verify(mongoTemplate, never()).findAndModify(
                                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                                eq(Product.class));
                verify(pasarelaPago, never()).cobrar(any(), any());
        }

        @Test
        void shouldRejectAddressOrPaymentMethodNotOwnedByClient() {
                assertThrows(RecursoCheckoutNoEncontradoException.class, () -> pedidoService.crearPedido(
                                CLIENTE_ID, new CrearPedidoDTO("direccion-ajena", "pago-1")));
                verify(pasarelaPago, never()).cobrar(any(), any());
        }

        private void habilitarReservaStock() {
                Product producto = productoConStock(5);
                when(mongoTemplate.findAndModify(
                                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                                eq(Product.class)))
                                .thenReturn(producto);
        }

        private void prepararCarritoYProducto(Product producto) {
                when(carritoRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(carrito));
                when(productRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(producto));
        }

        private void habilitarLiberacionStock() {
                UpdateResult resultado = org.mockito.Mockito.mock(UpdateResult.class);
                when(resultado.wasAcknowledged()).thenReturn(true);
                when(resultado.getMatchedCount()).thenReturn(1L);
                when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                                .thenReturn(resultado);
        }

        private Product productoConStock(int stock) {
                Product producto = new Product(new ProductDetails(
                                "Auriculares",
                                List.of("categoria-1"),
                                stock,
                                "Audio",
                                PRECIO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                true,
                                "vendedor-1"));

                org.springframework.test.util.ReflectionTestUtils.setField(
                                producto, "id", PRODUCTO_ID);

                return producto;
        }
}
