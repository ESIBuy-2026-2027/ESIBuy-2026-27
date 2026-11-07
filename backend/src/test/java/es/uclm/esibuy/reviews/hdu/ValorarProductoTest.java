package es.uclm.esibuy.reviews.hdu;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderItem;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.model.ProductDetails;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.reviews.dto.ReviewSummaryResponse;
import es.uclm.esibuy.reviews.model.Review;
import es.uclm.esibuy.reviews.repository.ReviewRepository;
import es.uclm.esibuy.reviews.service.ReviewService;

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
        private ProductRepository productRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private Order order;

        private ReviewService reviewService;

        @BeforeEach
        void setUp() {
                reviewService = new ReviewService(
                                orderRepository,
                                reviewRepository,
                                productRepository,
                                userRepository);
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
                verify(reviewRepository).save(argThat(review ->
                                review.getCustomerId().equals(CUSTOMER_ID)
                                                && review.getOrderId().equals(ORDER_ID)
                                                && review.getProductId().equals(PRODUCT_ID)
                                                && review.getRating() == RATING
                                                && review.getComment().equals(COMMENT)));
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
                prepararPedidoEntregadoConProducto();

                // Act
                reviewService.createReview(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID,
                                RATING,
                                null);

                // Assert
                verify(reviewRepository).save(argThat(review -> review.getComment() == null));
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

        @Test
        void unClienteNoPuedeCrearDosValoracionesParaElMismoProductoEnLaCompra() {
                prepararPedidoEntregadoConProducto();
                when(reviewRepository.existsByCustomerIdAndOrderIdAndProductId(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID))
                                .thenReturn(true);

                assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(
                                CUSTOMER_ID,
                                ORDER_ID,
                                PRODUCT_ID,
                                RATING,
                                COMMENT));

                verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any(Review.class));
        }

        @Test
        void rechazaUnDuplicadoDetectadoPorElIndiceUnicoDeMongo() {
                prepararPedidoEntregadoConProducto();
                when(reviewRepository.save(org.mockito.ArgumentMatchers.any(Review.class)))
                                .thenThrow(new DuplicateKeyException("duplicate review"));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> reviewService.createReview(
                                                CUSTOMER_ID,
                                                ORDER_ID,
                                                PRODUCT_ID,
                                                RATING,
                                                COMMENT));

                assertTrue(exception.getMessage().contains("ya ha valorado"));
                verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any(Product.class));
        }

        @Test
        void alGuardarValoracionActualizaElPromedioYElContadorDelProducto() {
                prepararPedidoEntregadoConProducto();
                Product product = new Product(new ProductDetails(
                                "Producto de prueba",
                                List.of(),
                                1,
                                null,
                                BigDecimal.TEN,
                                null,
                                null,
                                null,
                                true,
                                "vendedor-1"));
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

                reviewService.createReview(CUSTOMER_ID, ORDER_ID, PRODUCT_ID, RATING, COMMENT);

                assertEquals(5.0, product.getRating());
                assertEquals(1, product.getReviewCount());
                verify(productRepository).save(product);

                product.addRating(3);

                assertEquals(4.0, product.getRating());
                assertEquals(2, product.getReviewCount());
        }

        @Test
        void devuelveElPromedioElTotalYLasValoracionesDelProducto() {
                Review firstReview = new Review(CUSTOMER_ID, ORDER_ID, PRODUCT_ID, 4, "Correcto");
                Review secondReview = new Review("cliente-2", "pedido-2", PRODUCT_ID, 2, "Regular");
                when(reviewRepository.findAllByProductIdOrderByCreatedAtDesc(PRODUCT_ID))
                                .thenReturn(List.of(firstReview, secondReview));

                ReviewSummaryResponse summary = reviewService.getProductReviewSummary(PRODUCT_ID);

                assertEquals(3.0, summary.averageRating());
                assertEquals(2, summary.reviewCount());
                assertEquals(2, summary.reviews().size());
        }

        @Test
        void productoSinValoracionesNoTienePromedioNiResenas() {
                when(reviewRepository.findAllByProductIdOrderByCreatedAtDesc(PRODUCT_ID))
                                .thenReturn(List.of());

                ReviewSummaryResponse summary = reviewService.getProductReviewSummary(PRODUCT_ID);

                assertNull(summary.averageRating());
                assertEquals(0, summary.reviewCount());
                assertTrue(summary.reviews().isEmpty());
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
