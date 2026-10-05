package com.earthpol.combattag.combat;

import com.earthpol.combattag.CombatTag;
import com.earthpol.earthpollib.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Sweeps expired combat tags and reports the end of a tag
 * Runs on the global region and hops to the owning entity thread before touching a player
 * A task only acts while the enable cycle that created it is still active
 */
final class CombatTagTask implements Runnable {

    private final CombatTagRuntime runtime;

    CombatTagTask(CombatTagRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void run() {
        // skip the whole sweep when this task belongs to a retired enable cycle
        if (!runtime.isActive()) {
            return;
        }
        for (UUID playerId : runtime.sweepExpired()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || runtime.isTagged(playerId)) {
                continue;
            }
            reportEnd(player, playerId);
        }
    }

    // runs on the owning entity thread and rechecks the tag state before reporting an end
    private void reportEnd(Player player, UUID playerId) {
        runtime.runForPlayer(player, () -> {
            if (runtime.isTagged(playerId)) {
                return;
            }
            Translations.send(CombatTag.getTranslationService(), player, "combat.ended");
        });
    }
}
