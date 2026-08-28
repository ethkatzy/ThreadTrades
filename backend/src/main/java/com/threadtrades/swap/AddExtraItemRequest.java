package com.threadtrades.swap;

import jakarta.validation.constraints.NotNull;

public record AddExtraItemRequest(@NotNull Long clothingItemId) {
}
