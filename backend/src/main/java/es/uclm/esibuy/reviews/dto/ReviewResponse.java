package es.uclm.esibuy.reviews.dto;

import java.time.LocalDateTime;

import es.uclm.esibuy.reviews.model.Review;

public record ReviewResponse(
        int rating,
        String comment,
        LocalDateTime createdAt,
        String customerName) {

    public static ReviewResponse from(Review review, String customerName) {
        return new ReviewResponse(
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                customerName);
    }
}
