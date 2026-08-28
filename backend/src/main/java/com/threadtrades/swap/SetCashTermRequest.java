package com.threadtrades.swap;

import jakarta.validation.constraints.Positive;

public record SetCashTermRequest(@Positive long amountPence, boolean iOwe) {
}
