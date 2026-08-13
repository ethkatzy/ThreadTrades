package com.threadtrades.ws;

import org.springframework.messaging.MessagingException;

public class StompAuthenticationException extends MessagingException {

    public StompAuthenticationException(String message) {
        super(message);
    }
}
