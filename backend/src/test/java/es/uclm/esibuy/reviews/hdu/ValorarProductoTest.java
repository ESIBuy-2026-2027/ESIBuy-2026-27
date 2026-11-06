package es.uclm.esibuy.reviews.hdu;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;

import es.uclm.esibuy.reviews.model.Review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.function.Executable;

import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderItem;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.reviews.model.Review;
import es.uclm.esibuy.reviews.repository.ReviewRepository;
import es.uclm.esibuy.reviews.service.ReviewService;
import es.uclm.esibuy.reviews.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ValorarProductoTest {

        private static final String CUSTOMER_ID = "cliente-1";
        private static final String ORDER_ID = "pedido-123";
        private static final String PRODUCT_ID = "producto-1";
        private static final int RATING = 5;
        private static final String COMMENT = "Producto excelente";

        @Mock
        private OrderRepository orderRepository;

        @Mock
        private ReviewRepository reviewRepository;

        @Mock
        private Order order;

        private ReviewService reviewService;

        @BeforeEach
        void setUp() {
                reviewService = new ReviewService(
                                orderRepository,
                                reviewRepository);
        }

        @Test
        void unClientePuedeValorarUnProductoDeUnPedidoEntregado() {
                // Arrange
                prepararPedidoEntregadoConProducto();

                // Act
                Executable accion = () -> reviewService.createReview(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID,
                                RATING,
                                COMMENT);

                // Assert
                assertDoesNotThrow(accion);
        }

        private void prepararPedidoEntregadoConProducto() {
                when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                                .thenReturn(Optional.of(order));

                OrderItem orderItem = new OrderItem(
                                PRODUCT_ID,
                                "Producto de prueba",
                                1,
                                BigDecimal.TEN,
                                BigDecimal.TEN);

                when(order.getStatus()).thenReturn(OrderStatus.ENTREGADO);
                when(order.getItems()).thenReturn(List.of(orderItem));
        }

        @Test
        void unClienteNoPuedeValorarUnProductoQueNoHaComprado() {
                // Arrange
                prepararPedidoEntregadoConOtroProducto();

                // Act
                boolean puedeValorar = reviewService.canCustomerRateProduct(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID);

                // Assert
                assertFalse(puedeValorar);
        }

        private void prepararPedidoEntregadoConOtroProducto() {
                when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER_ID))
                                .thenReturn(Optional.of(order));

                OrderItem otroProducto = new OrderItem(
                                "otro-producto",
                                "Otro producto",
                                1,
                                BigDecimal.TEN,
                                BigDecimal.TEN);

                when(order.getStatus()).thenReturn(OrderStatus.ENTREGADO);
                when(order.getItems()).thenReturn(List.of(otroProducto));
        }

        @Test
        void unClientePuedeValorarUnProductoConUnaPuntuacionValida() {
                // Arrange
                int puntuacionValida = 5;

                // Act
                boolean resultado = reviewService.isValidRating(puntuacionValida);

                // Assert
                assertTrue(resultado);
        }

        @Test
        void unClientePuedeValorarUnProductoSinComentario() {
                // Arrange
                String comentario = null;

                // Act
                boolean esComentarioValido = reviewService.isValidComment(comentario);

                // Assert
                assertTrue(esComentarioValido);
        }

        @Test
        void unClienteNoPuedeValorarDosVecesElMismoProductoEnUnaCompra() {
                // Arrange
                prepararPedidoEntregadoConProducto();

                when(reviewRepository.existsByCustomerIdAndOrderIdAndProductId(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID))
                                .thenReturn(true);

                // Act
                boolean puedeCrearValoracion = reviewService.canCreateReview(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID);

                // Assert
                assertFalse(puedeCrearValoracion);
        }

        @Test
        void unClientePuedeGuardarUnaValoracion() {
                // Arrange
                Review review = prepararValoracion();

                // Act
                reviewService.saveReview(review);

                // Assert
                verify(reviewRepository).save(review);
        }

        private Review prepararValoracion() {
                return new Review(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID,
                                RATING,
                                COMMENT);
        }
}
