package com.threadtrades.security;

import com.threadtrades.user.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Short-lived in-memory cache of the two {@code AppUser} fields
 * {@code JwtAuthenticationFilter} needs on every authenticated request (the
 * banned flag and role), so most requests skip the per-request DB round trip
 * -- see ROADMAP.md §3. {@code AdminUserService} explicitly invalidates a
 * user's entry on ban/unban/promote/demote, so those actions still take
 * effect on the very next request exactly as before; the TTL is just a bound
 * on staleness for correctness in case some other path ever changes these
 * fields without going through that invalidation.
 *
 * <p>Per-JVM-instance only, not shared across processes -- fine for this
 * app's single-instance deployment (see ROADMAP.md §2), but would need a
 * shared store (e.g. Redis) if it ever ran with more than one instance.
 */
@Component
public class AppUserAuthCache {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    public Optional<Entry> get(Long appUserId) {
        Entry entry = entries.get(appUserId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public Entry put(Long appUserId, boolean banned, Role role) {
        Entry entry = new Entry(banned, role, Instant.now().plus(TTL));
        entries.put(appUserId, entry);
        return entry;
    }

    public void invalidate(Long appUserId) {
        entries.remove(appUserId);
    }

    public record Entry(boolean banned, Role role, Instant expiresAt) {
    }
}
