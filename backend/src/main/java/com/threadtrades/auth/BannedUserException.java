package com.threadtrades.auth;

public class BannedUserException extends RuntimeException {

    public BannedUserException() {
        super("This account has been banned");
    }
}
