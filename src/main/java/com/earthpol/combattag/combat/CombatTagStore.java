package com.earthpol.combattag.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Shared combat tag state used by the plugin and by the public API
 * Entries hold absolute expiry timestamps in milliseconds
 * Queries are safe to call from any thread while mutations belong on the owning player thread
 */
final class CombatTagStore {

    private final Map<UUID, Long> expiryByPlayer = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long durationMillis;

    CombatTagStore(LongSupplier clock, long durationMillis) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.durationMillis = durationMillis;
    }

    long getDurationMillis() {
        return durationMillis;
    }

    boolean isTagged(UUID playerId) {
        Long expiry = expiryByPlayer.get(playerId);
        return expiry != null && expiry > clock.getAsLong();
    }

    long getRemainingMillis(UUID playerId) {
        Long expiry = expiryByPlayer.get(playerId);
        if (expiry == null) {
            return -1L;
        }
        return expiry - clock.getAsLong();
    }

    boolean apply(UUID playerId) {
        long now = clock.getAsLong();
        Long previous = expiryByPlayer.put(playerId, now + durationMillis);
        return previous == null || previous <= now;
    }

    boolean remove(UUID playerId) {
        return expiryByPlayer.remove(playerId) != null;
    }

    List<UUID> sweepExpired() {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : expiryByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            Long expiry = entry.getValue();
            if (expiry == null || expiry > clock.getAsLong()) {
                continue;
            }
            // compare and remove so a tag refreshed during the sweep is never dropped
            if (expiryByPlayer.remove(playerId, expiry)) {
                expired.add(playerId);
            }
        }
        return expired;
    }

    void clear() {
        expiryByPlayer.clear();
    }
}
