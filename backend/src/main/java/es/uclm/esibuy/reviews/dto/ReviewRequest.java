package es.uclm.esibuy.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
        @NotBlank String orderId,
        @NotBlank String productId,
        @Min(1) @Max(5)
        int rating,
        String comment) {
}
