package com.earthpol.combattag.combat.actionbar;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.combat.CombatHandler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Global ticker mirroring BossBarTask, but rendering to the action bar.
 * Folia-safe: all player interaction is scheduled on each player's entity thread.
 *
 * Shows: a red '|' bar that shrinks with time, the elapsed half goes yellow,
 * then turns white near the end. Example: "|||||||||||||||||||||||||| 45.0s"
 */
public final class ActionBarTask extends BukkitRunnable {

    // Visual controls
    private static final int SEGMENTS = 30;        // number of '|' characters
    private static final String RED    = "§c";
    private static final String YELLOW = "§e";
    private static final String WHITE  = "§f";
    private static final String GRAY   = "§7";
    private static final String RESET  = "§r";

    @Override
    public void run() {
        // Global tick. Never touch Player directly here; hop to the player's region first.
        for (Player p : Bukkit.getOnlinePlayers()) {
            final Player player = p;
            player.getScheduler().run(
                    CombatTag.getInstance(),
                    (ScheduledTask scheduled) -> tickPlayer(player),
                    null
            );
        }
    }

    /** One-player update on that player's region thread. */
    private static void tickPlayer(Player player) {
        if (!player.isOnline()) {
            clearOnce(player);
            return;
        }

        long remaining = CombatHandler.getRemaining(player);
        if (remaining <= 0) {
            clearOnce(player);
            return;
        }

        double fraction = Math.max(0.0, Math.min(1.0, (double) remaining / (double) CombatHandler.TAG_TIME));
        String bar = buildBar(fraction);
        String secs = (remaining / 1000) + "s";

        String legacy = bar + " " + GRAY + secs + RESET;
        sendActionBar(player, LegacyComponentSerializer.legacySection().deserialize(legacy));
    }

    /** Mirrors BossBarTask.remove(...) call sites. Clears once on the player's region thread. */
    public static void remove(Player player) {
        if (player == null) return;
        player.getScheduler().run(
                CombatTag.getInstance(),
                (ScheduledTask scheduled) -> clearOnce(player),
                null
        );
    }

    private static void clearOnce(Player p) {
        try { p.sendActionBar(Component.empty()); } catch (Throwable ignored) {}
    }

    private static void sendActionBar(Player p, Component c) {
        try { p.sendActionBar(c); }
        catch (NoSuchMethodError ignored) {
            // Very old API fallback path
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
