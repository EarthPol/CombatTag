package com.earthpol.combattag.combat;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Handle for one plugin enable cycle
 * Tasks and scheduled callbacks capture this so work from a retired cycle stays inert
 * UUID queries are safe to call from any thread
 * Player mutations must run on the owning player thread
 */
public final class CombatTagRuntime {

    private final Plugin plugin;
    private final CombatTagStore store;
    private final CombatTagApiProvider provider;

    CombatTagRuntime(Plugin plugin, CombatTagStore store, CombatTagApiProvider provider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.store = Objects.requireNonNull(store, "store");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * Reports whether this enable cycle is still the running one
     *
     * @return true between start and stop of this cycle
     */
    public boolean isActive() {
        return provider.isActive();
    }

    /**
     * Reports whether a player has an active combat tag
     *
     * @param playerId unique id of the player to check
     * @return true when a tag is active
     */
    public boolean isTagged(UUID playerId) {
        return store.isTagged(Objects.requireNonNull(playerId, "playerId"));
    }

    /**
     * Remaining combat tag time of a player or a negative value when untagged
     *
     * @param playerId unique id of the player to check
     * @return remaining time in milliseconds
     */
    public long getRemainingMillis(UUID playerId) {
        return store.getRemainingMillis(Objects.requireNonNull(playerId, "playerId"));
    }

    /**
     * Default combat tag duration in milliseconds
     *
     * @return duration applied by every fresh tag
     */
    public long getDurationMillis() {
        return store.getDurationMillis();
    }

    /**
     * Starts or refreshes a combat tag and applies the usual player feedback
     * Must run on the owning player thread
     *
     * @param player player to tag
     */
    public void applyTag(Player player) {
        CombatHandler.applyTag(store, Objects.requireNonNull(player, "player"));
    }

    /**
     * Removes a combat tag without feedback messages
     * Must run on the owning player thread
     *
     * @param player player to untag
     */
    public void removeTag(Player player) {
        CombatHandler.removeTag(store, Objects.requireNonNull(player, "player"));
    }

    /**
     * Runs the action on the owning player thread while this cycle is still active
     * The action is skipped when the cycle stopped or the player is gone
     *
     * @param player player that owns the thread
     * @param action work to run on that thread
     */
    public void runForPlayer(Player player, Runnable action) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        if (!isActive()) {
            return;
        }
        if (CombatHandler.isOwningThread(player)) {
            if (!player.isOnline()) {
                return;
            }
            action.run();
            return;
        }
        player.getScheduler().run(plugin, scheduled -> {
            if (!isActive() || !player.isOnline()) {
                return;
            }
            action.run();
        }, null);
    }

    List<UUID> sweepExpired() {
        return store.sweepExpired();
    }
}
