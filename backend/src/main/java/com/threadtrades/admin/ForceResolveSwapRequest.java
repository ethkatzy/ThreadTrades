package com.threadtrades.admin;

import com.threadtrades.swap.SwapStatus;
import jakarta.validation.constraints.NotNull;

public record ForceResolveSwapRequest(@NotNull SwapStatus status) {
}
