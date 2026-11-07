package es.uclm.esibuy.reviews.dto;

import java.util.List;

public record ReviewSummaryResponse(
        Double averageRating,
        int reviewCount,
        List<ReviewResponse> reviews) {

    public ReviewSummaryResponse {
        reviews = List.copyOf(reviews);
    }
}
