package com.threadtrades.ws;

/** Key under which the WebSocket handshake interceptor stashes the authenticated user. */
final class WsSessionAttributes {

    static final String AUTHENTICATED_USER = "authenticatedUser";

    private WsSessionAttributes() {
    }
}
