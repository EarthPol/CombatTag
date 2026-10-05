package com.earthpol.combattag.api;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Public combat tag API handed out through the Bukkit services manager
 *
 * Obtain the service with Bukkit.getServicesManager().load(CombatTagApi.class)
 * UUID queries are safe to call from any thread
 * Player overloads and mutations must run on the owning player thread
 * Every method throws IllegalStateException once the plugin is disabled
 */
public interface CombatTagApi {

    /**
     * Reports whether a player has an active combat tag
     * Safe to call from any thread
     *
     * @param playerId unique id of the player to check
     * @return true when a tag is active
     * @throws NullPointerException when playerId is null
     * @throws IllegalStateException when the plugin is disabled
     */
    boolean isTagged(UUID playerId);

    /**
     * Reports whether a player has an active combat tag
     * Must run on the owning player thread
     *
     * @param player player to check
     * @return true when a tag is active
     * @throws NullPointerException when player is null
     * @throws IllegalStateException when the plugin is disabled
     */
    default boolean isTagged(Player player) {
        Objects.requireNonNull(player, "player");
        return isTagged(player.getUniqueId());
    }

    /**
     * Time left on the combat tag of a player
     * Safe to call from any thread
     *
     * @param playerId unique id of the player to check
     * @return remaining time in milliseconds or zero when no tag is active
     * @throws NullPointerException when playerId is null
     * @throws IllegalStateException when the plugin is disabled
     */
    long getRemainingMillis(UUID playerId);

    /**
     * Time left on the combat tag of a player
     * Must run on the owning player thread
     *
     * @param player player to check
     * @return remaining time in milliseconds or zero when no tag is active
     * @throws NullPointerException when player is null
     * @throws IllegalStateException when the plugin is disabled
     */
    default long getRemainingMillis(Player player) {
        Objects.requireNonNull(player, "player");
        return getRemainingMillis(player.getUniqueId());
    }

    /**
     * Starts or refreshes the combat tag of a player for the default duration
     * Runs the same inventory message and flight handling as regular combat
     * Must run on the owning player thread
     *
     * @param player player to tag
     * @throws NullPointerException when player is null
     * @throws IllegalStateException when the plugin is disabled
     * @throws IllegalStateException when the caller does not own the player thread
     */
    void applyTag(Player player);

    /**
     * Removes the combat tag of a player without feedback messages
     * Must run on the owning player thread
     *
     * @param player player to untag
     * @throws NullPointerException when player is null
     * @throws IllegalStateException when the plugin is disabled
     * @throws IllegalStateException when the caller does not own the player thread
     */
    void removeTag(Player player);

    /**
     * Default combat tag duration in milliseconds
     * Safe to call from any thread
     *
     * @return duration applied by applyTag
     * @throws IllegalStateException when the plugin is disabled
     */
    long getTagDurationMillis();
}
