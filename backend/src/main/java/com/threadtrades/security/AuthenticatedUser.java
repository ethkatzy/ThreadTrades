package com.threadtrades.security;

import com.threadtrades.user.Role;

/**
 * The principal set on the security context for a request bearing a valid JWT.
 * Controllers/services must derive the acting user from this, never from a
 * client-supplied id in the request body (AUDIT.md #10/#13).
 */
public record AuthenticatedUser(Long appUserId, String email, Role role) {
}
