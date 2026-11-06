package es.uclm.esibuy.reviews.dto;

public record ReviewRequest(
        String orderId,
        String productId,
        int rating,
        String comment) {
}
