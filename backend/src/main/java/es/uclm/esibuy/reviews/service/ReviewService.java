package es.uclm.esibuy.reviews.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.reviews.model.Review;
import es.uclm.esibuy.reviews.repository.ReviewRepository;

@Service
public class ReviewService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    public ReviewService(
            OrderRepository orderRepository,
            ReviewRepository reviewRepository) {

        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.reviewRepository = Objects.requireNonNull(reviewRepository);
    }

    public void createReview(
            String customerId,
            String orderId,
            String productId,
            int rating,
            String comment) {

        var order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pedido no pertenece al cliente"));

        if (order.getStatus() != OrderStatus.ENTREGADO) {
            throw new IllegalArgumentException(
                    "El pedido todavía no ha sido entregado");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "La puntuación debe estar entre 1 y 5");
        }

        if (!orderContainsProduct(order, productId)) {
            throw new IllegalArgumentException(
                    "El cliente no ha comprado este producto");
        }
    }

    public boolean canCustomerRateProduct(
            String customerId,
            String orderId,
            String productId) {

        var order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pedido no pertenece al cliente"));

        if (order.getStatus() != OrderStatus.ENTREGADO) {
            return false;
        }

        return orderContainsProduct(order, productId);
    }

    public boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public boolean isValidComment(String comment) {
        return comment == null || comment.isEmpty();
    }

    public boolean canCreateReview(
            String customerId,
            String orderId,
            String productId) {

        var order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pedido no pertenece al cliente"));

        if (order.getStatus() != OrderStatus.ENTREGADO) {
            return false;
        }

        if (!orderContainsProduct(order, productId)) {
            return false;
        }

        return !reviewRepository.existsByCustomerIdAndOrderIdAndProductId(
                customerId,
                orderId,
                productId);
    }

    private boolean orderContainsProduct(Order order, String productId) {
        return order.getItems().stream()
                .anyMatch(item -> item.productId().equals(productId));
    }

    public void saveReview(Review review) {
        reviewRepository.save(review);
    }
}
