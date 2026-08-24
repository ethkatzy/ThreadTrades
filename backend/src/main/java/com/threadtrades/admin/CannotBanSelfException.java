package com.threadtrades.admin;

public class CannotBanSelfException extends RuntimeException {

    public CannotBanSelfException() {
        super("You cannot ban your own account");
    }
}
