package es.uclm.esibuy.reviews.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.reviews.dto.ReviewRequest;
import es.uclm.esibuy.reviews.dto.ReviewSummaryResponse;
import es.uclm.esibuy.reviews.dto.ReviewedPurchaseResponse;
import es.uclm.esibuy.reviews.service.ReviewService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final String REVIEW_CREATED_MESSAGE = "Valoración creada correctamente";

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        reviewService.createReview(
                principal.id(),
                request.orderId(),
                request.productId(),
                request.rating(),
                request.comment());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(REVIEW_CREATED_MESSAGE);
    }

    @GetMapping("/product/{productId}")
    public ReviewSummaryResponse getProductReviews(@PathVariable String productId) {
        return reviewService.getProductReviewSummary(productId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ReviewedPurchaseResponse> getReviewedPurchases(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return reviewService.getReviewedPurchases(principal.id());
    }
}
