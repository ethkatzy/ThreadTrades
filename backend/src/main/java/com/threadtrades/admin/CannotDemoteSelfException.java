package com.threadtrades.admin;

public class CannotDemoteSelfException extends RuntimeException {

    public CannotDemoteSelfException() {
        super("You cannot remove your own admin role");
    }
}
