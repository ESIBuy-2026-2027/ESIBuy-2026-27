package es.uclm.esibuy.auth.passwordreset.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * Limitador sencillo para desarrollo y despliegues de una sola instancia.
 * Para producción con varias instancias debe sustituirse por un limitador compartido.
 */
@Component
public class PasswordResetRateLimiter {

    private static final int MAX_PER_IP_PER_MINUTE = 3;
    private static final int MAX_PER_EMAIL_PER_DAY = 5;
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);
    private static final Duration EMAIL_WINDOW = Duration.ofDays(1);

    private final ConcurrentMap<String, Deque<Instant>> ipRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Deque<Instant>> emailRequests = new ConcurrentHashMap<>();

    /**
     * Devuelve false cuando se debe ignorar la petición, pero el endpoint responderá
     * exactamente igual para no revelar si el email existe o ha alcanzado su límite.
     */
    public boolean allow(String ipAddress, String normalizedEmail) {
        Instant now = Instant.now();
        boolean ipAllowed = recordIfAllowed(
                ipRequests, ipAddress == null ? "unknown" : ipAddress,
                now, IP_WINDOW, MAX_PER_IP_PER_MINUTE);
        boolean emailAllowed = recordIfAllowed(
                emailRequests, normalizedEmail,
                now, EMAIL_WINDOW, MAX_PER_EMAIL_PER_DAY);
        return ipAllowed && emailAllowed;
    }

    private boolean recordIfAllowed(
            ConcurrentMap<String, Deque<Instant>> store,
            String key,
            Instant now,
            Duration window,
            int limit) {

        Deque<Instant> queue = store.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            Instant cutoff = now.minus(window);
            while (!queue.isEmpty() && !queue.peekFirst().isAfter(cutoff)) {
                queue.removeFirst();
            }
            if (queue.size() >= limit) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
