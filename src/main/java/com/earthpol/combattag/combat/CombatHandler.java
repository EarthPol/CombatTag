package com.earthpol.combattag.combat;

import com.earthpol.combattag.CombatTag;
import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatHandler {
    public static final long TAG_TIME = 45 * 1000;
    private static Map<UUID, Long> combatTags = new ConcurrentHashMap<>();

    static {
        FoliaTaskScheduler scheduler = new FoliaTaskScheduler(CombatTag.getInstance());
        scheduler.runAsyncRepeating(new CombatTagTask(combatTags), 10L, 10L);
    }

    public static void applyTag(Player player) {
        if (!isTagged(player)) {
            player.closeInventory(Reason.PLUGIN);
            player.sendMessage(ChatColor.RED + "You have been combat tagged for " + (TAG_TIME / 1000) + " seconds! Do not log out or you will get killed instantly.");
        }

        player.setFlying(false);
        combatTags.put(player.getUniqueId(), System.currentTimeMillis() + TAG_TIME);
    }

    public static void removeTag(Player player) {
        combatTags.remove(player.getUniqueId());
    }

    public static boolean isTagged(Player player) {
        return combatTags.containsKey(player.getUniqueId()) && combatTags.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    public static long getRemaining(Player player) {
        if (!combatTags.containsKey(player.getUniqueId()))
            return -1;

        return combatTags.get(player.getUniqueId()) - System.currentTimeMillis();
    }
}

class CombatTagTask implements Runnable {
    private final Map<UUID, Long> combatTags;

    public CombatTagTask(Map<UUID, Long> combatTags) {
        this.combatTags = combatTags;
    }

    @Override
    public void run() {
        Iterator<Map.Entry<UUID, Long>> iterator = combatTags.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();

            if (entry.getValue() > System.currentTimeMillis())
                continue;

            iterator.remove();

            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline())
                continue;

            player.sendMessage(ChatColor.GREEN + "You are no longer in combat.");
        }
    }
}