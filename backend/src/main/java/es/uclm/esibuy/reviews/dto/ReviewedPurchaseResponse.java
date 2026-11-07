package es.uclm.esibuy.reviews.dto;

public record ReviewedPurchaseResponse(
        String orderId,
        String productId) {
}
