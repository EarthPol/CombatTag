package com.earthpol.combattag.combat;

import com.earthpol.combattag.api.CombatTagApi;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service provider behind the public combat tag API
 * Queries delegate to the shared store and mutations delegate to the combat handler
 * A provider instance serves exactly one plugin enable cycle
 */
final class CombatTagApiProvider implements CombatTagApi {

    private final CombatTagStore store;
    private final AtomicBoolean activated = new AtomicBoolean();
    private volatile boolean active;

    CombatTagApiProvider(CombatTagStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    void activate() {
        if (!activated.compareAndSet(false, true)) {
            throw new IllegalStateException("CombatTagApi provider cannot be activated twice");
        }
        active = true;
    }

    void deactivate() {
        active = false;
    }

    boolean isActive() {
        return active;
    }

    @Override
    public boolean isTagged(UUID playerId) {
        requireActive();
        return store.isTagged(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public long getRemainingMillis(UUID playerId) {
        requireActive();
        long remaining = store.getRemainingMillis(Objects.requireNonNull(playerId, "playerId"));
        return remaining < 0L ? 0L : remaining;
    }

    @Override
    public void applyTag(Player player) {
        requireActive();
        Objects.requireNonNull(player, "player");
        requireOwningThread(player);
        CombatHandler.applyTag(store, player);
    }

    @Override
    public void removeTag(Player player) {
        requireActive();
        Objects.requireNonNull(player, "player");
        requireOwningThread(player);
        CombatHandler.removeTag(store, player);
    }

    @Override
    public long getTagDurationMillis() {
        requireActive();
        return store.getDurationMillis();
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("CombatTagApi is not active");
        }
    }

    private static void requireOwningThread(Player player) {
        if (CombatHandler.isOwningThread(player)) {
            return;
        }
        throw new IllegalStateException("CombatTagApi mutations must run on the owning player thread");
    }
}
