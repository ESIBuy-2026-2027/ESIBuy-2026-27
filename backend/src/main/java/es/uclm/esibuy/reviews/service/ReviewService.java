package es.uclm.esibuy.reviews.service;

import java.util.List;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.reviews.dto.ReviewResponse;
import es.uclm.esibuy.reviews.dto.ReviewSummaryResponse;
import es.uclm.esibuy.reviews.dto.ReviewedPurchaseResponse;
import es.uclm.esibuy.reviews.model.Review;
import es.uclm.esibuy.reviews.repository.ReviewRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class ReviewService {

    private static final String DUPLICATE_REVIEW_MESSAGE =
            "El cliente ya ha valorado este producto en este pedido";

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(
            OrderRepository orderRepository,
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {

        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.reviewRepository = Objects.requireNonNull(reviewRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
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

        if (!isValidRating(rating)) {
            throw new IllegalArgumentException(
                    "La puntuación debe estar entre 1 y 5");
        }

        if (!orderContainsProduct(order, productId)) {
            throw new IllegalArgumentException(
                    "El cliente no ha comprado este producto");
        }

        if (reviewRepository.existsByCustomerIdAndOrderIdAndProductId(
                customerId,
                orderId,
                productId)) {
            throw new IllegalArgumentException(DUPLICATE_REVIEW_MESSAGE);
        }

        try {
            saveReview(new Review(customerId, orderId, productId, rating, comment));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException(DUPLICATE_REVIEW_MESSAGE, exception);
        }

        productRepository.findById(productId).ifPresent(product -> updateProductRating(product, rating));
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

        return orderContainsProduct(order, productId)
                && !reviewRepository.existsByCustomerIdAndOrderIdAndProductId(
                        customerId,
                        orderId,
                        productId);
    }

    public boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public boolean canCreateReview(
            String customerId,
            String orderId,
            String productId) {
        return canCustomerRateProduct(customerId, orderId, productId);
    }

    private boolean orderContainsProduct(Order order, String productId) {
        return order.getItems().stream()
                .anyMatch(item -> item.productId().equals(productId));
    }

    public void saveReview(Review review) {
        reviewRepository.save(review);
    }

    public ReviewSummaryResponse getProductReviewSummary(String productId) {
        List<Review> reviews = reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
        Double averageRating = reviews.isEmpty()
                ? null
                : reviews.stream().mapToInt(Review::getRating).average().orElseThrow();
        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(this::toReviewResponse)
                .toList();

        return new ReviewSummaryResponse(averageRating, reviews.size(), reviewResponses);
    }

    private ReviewResponse toReviewResponse(Review review) {
        String customerName = userRepository.findById(review.getCustomerId())
                .map(this::formatCustomerName)
                .orElse("Cliente");
        return ReviewResponse.from(review, customerName);
    }

    private String formatCustomerName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    public List<ReviewedPurchaseResponse> getReviewedPurchases(String customerId) {
        return reviewRepository.findAllByCustomerId(customerId).stream()
                .map(review -> new ReviewedPurchaseResponse(
                        review.getOrderId(),
                        review.getProductId()))
                .toList();
    }

    private void updateProductRating(Product product, int rating) {
        product.addRating(rating);
        productRepository.save(product);
    }
}
