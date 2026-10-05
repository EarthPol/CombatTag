package com.earthpol.combattag.combat.actionbar;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.combat.CombatTagRuntime;
import com.earthpol.earthpollib.translation.Translations;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global ticker that renders combat tag time to the action bar
 * The ticker runs on the global region and updates every player on their own entity thread
 * Shows a red '|' bar that shrinks with time, the elapsed half goes yellow,
 * then turns white near the end. Example: "|||||||||||||||||||||||||| 45.0s"
 */
public final class ActionBarTask {

    // Visual controls
    private static final int SEGMENTS = 30;        // number of '|' characters
    private static final String RED    = "§c";
    private static final String YELLOW = "§e";
    private static final String WHITE  = "§f";
    private static final String GRAY   = "§7";
    private static final String RESET  = "§r";

    private final Plugin plugin;
    private final CombatTagRuntime runtime;
    private final Map<UUID, String> lastSent = new ConcurrentHashMap<>();

    private ScheduledTask handle;

    public ActionBarTask(Plugin plugin, CombatTagRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    /** Starts the repeating ticker for this enable cycle */
    public void start() {
        handle = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduled -> run(), 10L, 10L);
    }

    /** Cancels the ticker and drops every cached render value */
    public void stop() {
        ScheduledTask current = handle;
        handle = null;
        if (current != null) {
            current.cancel();
        }
        lastSent.clear();
    }

    public void run() {
        // Global tick. Never touch Player directly here, hop to the player region first.
        if (!runtime.isActive()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            runtime.runForPlayer(player, () -> tickPlayer(player));
        }
    }

    /** One-player update on that player region thread. */
    private void tickPlayer(Player player) {
        if (!runtime.isActive() || !player.isOnline()) return;

        long remaining = runtime.getRemainingMillis(player.getUniqueId());
        if (remaining <= 0) {
            // Stop sending, leave the bar alone so other plugins can write their own state
            lastSent.remove(player.getUniqueId());
            return;
        }

        long duration = runtime.getDurationMillis();
        double fraction = Math.max(0.0, Math.min(1.0, (double) remaining / (double) duration));
        String bar = buildBar(fraction);
        String secs = Translations.raw(CombatTag.getTranslationService(), player, "ui.actionbar.seconds", remaining / 1000);
        String legacy = bar + " " + GRAY + secs + RESET;

        // Only send if different from what we last sent
        UUID id = player.getUniqueId();
        String prev = lastSent.get(id);
        if (!legacy.equals(prev)) {
            sendActionBar(player, LegacyComponentSerializer.legacySection().deserialize(legacy));
            lastSent.put(id, legacy);
        }
    }

    /** Drops the cached render value of one player */
    public void forget(Player player) {
        if (player == null) return;
        lastSent.remove(player.getUniqueId());
    }

    private static void sendActionBar(Player p, Component c) {
        try {
            p.sendActionBar(c);
        } catch (NoSuchMethodError ignored) {
            p.sendActionBar(LegacyComponentSerializer.legacySection().serialize(c));
        }
    }

    /**
     * Color logic to match your spec:
     * - Full time: all segments red.
     * - Half time: remaining half red, elapsed half yellow.
     * - Near end: elapsed half turns white.
     */
    private static String buildBar(double fractionRemaining) {
        int remaining = (int) Math.round(fractionRemaining * SEGMENTS);
        if (remaining < 0) remaining = 0;
        if (remaining > SEGMENTS) remaining = SEGMENTS;

        int elapsed = SEGMENTS - remaining;
        String elapsedColor = (fractionRemaining >= 0.5) ? YELLOW : WHITE;

        StringBuilder sb = new StringBuilder(SEGMENTS * 3);
        for (int i = 0; i < remaining; i++) sb.append(RED).append('|');
        for (int i = 0; i < elapsed;   i++) sb.append(elapsedColor).append('|');
        return sb.append(RESET).toString();
    }
}
