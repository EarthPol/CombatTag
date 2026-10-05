package com.earthpol.combattag.combat;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.api.CombatTagApi;
import com.earthpol.earthpollib.translation.Translations;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared combat tag state and lifecycle
 * UUID queries are safe to call from any thread
 * Player overloads and mutations must run on the owning player thread
 * The sweep task only runs between start and stop
 */
public class CombatHandler {
    /** Default combat tag duration in milliseconds */
    public static final long TAG_TIME = 45 * 1000;

    private static final boolean FOLIA =
            ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));

    private static volatile CombatTagStore store = newStore();

    private static volatile boolean running;
    private static volatile ScheduledTask sweepTask;
    private static volatile CombatTagApiProvider apiProvider;
    private static volatile CombatTagRuntime runtime;

    /**
     * Utility class without instances
     */
    private CombatHandler() {
    }

    private static CombatTagStore newStore() {
        return new CombatTagStore(System::currentTimeMillis, TAG_TIME);
    }

    /**
     * Starts the sweep task and opens the public API for the next enable cycle
     * Must run on the main thread while the plugin enables
     *
     * @param plugin owning plugin of the sweep task
     * @return handle bound to the started enable cycle
     * @throws NullPointerException when plugin is null
     */
    public static CombatTagRuntime start(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        CombatTagRuntime current = runtime;
        if (running && current != null) {
            return current;
        }

        // each enable cycle owns one store so retired tasks and providers stay isolated
        CombatTagStore generation = newStore();
        store = generation;
        CombatTagApiProvider provider = new CombatTagApiProvider(generation);
        provider.activate();
        CombatTagRuntime created = new CombatTagRuntime(plugin, generation, provider);
        CombatTagTask sweep = new CombatTagTask(created);
        // global region keeps the ten tick period and hands every player effect to its own thread
        ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduled -> sweep.run(), 10L, 10L);

        runtime = created;
        sweepTask = task;
        apiProvider = provider;
        running = true;
        return created;
    }

    /**
     * Cancels the sweep task closes the public API and clears every tag
     */
    public static void stop() {
        running = false;

        ScheduledTask task = sweepTask;
        sweepTask = null;
        if (task != null) {
            task.cancel();
        }

        CombatTagApiProvider provider = apiProvider;
        apiProvider = null;
        if (provider != null) {
            provider.deactivate();
        }

        runtime = null;

        CombatTagStore retired = store;
        retired.clear();
        store = newStore();
    }

    /**
     * Reports whether the sweep task is currently running
     *
     * @return true between start and stop
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Reports whether the current thread is allowed to mutate the given player
     * Paper allows any main thread context while Folia requires the owning region thread
     *
     * @param player player to check
     * @return true when the caller owns the player thread
     * @throws NullPointerException when player is null
     */
    public static boolean isOwningThread(Player player) {
        Objects.requireNonNull(player, "player");
        if (FOLIA) {
            return Bukkit.isOwnedByCurrentRegion(player);
        }
        return Bukkit.isPrimaryThread();
    }

    /**
     * Returns the provider that is registered with the services manager
     *
     * @return active API provider
     * @throws IllegalStateException when the handler is not running
     */
    public static CombatTagApi api() {
        CombatTagApiProvider provider = apiProvider;
        if (provider == null) {
            throw new IllegalStateException("CombatTag API is only available between start and stop");
        }
        return provider;
    }

    /**
     * Starts or refreshes a combat tag and applies the usual player feedback
     * Must run on the owning player thread
     *
     * @param player player to tag
     * @throws NullPointerException when player is null
     */
    public static void applyTag(Player player) {
        applyTag(store, player);
    }

    static void applyTag(CombatTagStore store, Player player) {
        Objects.requireNonNull(player, "player");
        boolean freshTag = store.apply(player.getUniqueId());
        if (freshTag) {
            player.closeInventory(Reason.PLUGIN);
            Translations.send(CombatTag.getTranslationService(), player, "combat.tagged", TAG_TIME / 1000);
        }

        player.setFlying(false);
    }

    /**
     * Removes a combat tag without feedback messages
     * Must run on the owning player thread
     *
     * @param player player to untag
     * @throws NullPointerException when player is null
     */
    public static void removeTag(Player player) {
        removeTag(store, player);
    }

    static void removeTag(CombatTagStore store, Player player) {
        Objects.requireNonNull(player, "player");
        store.remove(player.getUniqueId());
    }

    /**
     * Reports whether a player has an active combat tag
     *
     * @param player player to check
     * @return true when a tag is active
     * @throws NullPointerException when player is null
     */
    public static boolean isTagged(Player player) {
        Objects.requireNonNull(player, "player");
        return isTagged(player.getUniqueId());
    }

    /**
     * Reports whether a player has an active combat tag
     *
     * @param playerId unique id of the player to check
     * @return true when a tag is active
     * @throws NullPointerException when playerId is null
     */
    public static boolean isTagged(UUID playerId) {
        return store.isTagged(Objects.requireNonNull(playerId, "playerId"));
    }

    /**
     * Remaining combat tag time of a player or a negative value when untagged
     *
     * @param player player to check
     * @return remaining time in milliseconds
     * @throws NullPointerException when player is null
     */
    public static long getRemaining(Player player) {
        Objects.requireNonNull(player, "player");
        return getRemaining(player.getUniqueId());
    }

    /**
     * Remaining combat tag time of a player or a negative value when untagged
     *
     * @param playerId unique id of the player to check
     * @return remaining time in milliseconds
     * @throws NullPointerException when playerId is null
     */
    public static long getRemaining(UUID playerId) {
        return store.getRemainingMillis(Objects.requireNonNull(playerId, "playerId"));
    }

    /**
     * Default combat tag duration in milliseconds
     *
     * @return duration applied by every fresh tag
     */
    public static long getTagDurationMillis() {
        return store.getDurationMillis();
    }
}
