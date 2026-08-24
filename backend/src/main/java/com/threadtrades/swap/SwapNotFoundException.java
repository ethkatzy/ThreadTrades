package com.threadtrades.swap;

public class SwapNotFoundException extends RuntimeException {

    public SwapNotFoundException(Long id) {
        super("Swap not found: " + id);
    }
}
