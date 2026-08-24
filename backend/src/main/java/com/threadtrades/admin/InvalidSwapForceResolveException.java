package com.threadtrades.admin;

public class InvalidSwapForceResolveException extends RuntimeException {

    public InvalidSwapForceResolveException() {
        super("Force-resolve target status must be ACCEPTED or REJECTED");
    }
}
