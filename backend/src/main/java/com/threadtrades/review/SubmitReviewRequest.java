package com.threadtrades.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * {@code rating} is nullable by design: the frontend's "skip" action submits
 * no rating at all, and {@link ReviewService} treats that as an automatic
 * 5-star review rather than rejecting the request. Bean Validation's
 * {@code @Min}/{@code @Max} only fire when the value is present.
 */
public record SubmitReviewRequest(@Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {
}
