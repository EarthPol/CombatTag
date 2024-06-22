package com.earthpol.combattag.combat.bossbar;

import com.earthpol.combattag.combat.CombatHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarTask extends BukkitRunnable {

    private static Map<UUID, BossBar> bossBarMap = new ConcurrentHashMap<>();

    @Override
    public void run() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (CombatHandler.isTagged(online)) {
                BossBar bossBar = bossBarMap.get(online.getUniqueId());
                if (bossBar == null) {
                    bossBar = Bukkit.createBossBar(ChatColor.RED + ChatColor.BOLD.toString() + "Combat Tag" + ChatColor.GRAY + ": ", BarColor.RED, BarStyle.SOLID);
                    bossBar.addPlayer(online);

                    bossBarMap.put(online.getUniqueId(), bossBar);
                }

                update(online);

                if (!bossBar.isVisible())
                    bossBar.setVisible(true);
            } else if (bossBarMap.containsKey(online.getUniqueId())) {
                BossBar bossBar = bossBarMap.get(online.getUniqueId());
                if (bossBar.isVisible())
                    bossBar.setVisible(false);
            }
        }
    }

    public static void update(Player player) {
        BossBar bossBar = bossBarMap.get(player.getUniqueId());
        if (bossBar == null)
            return;

        long remaining = CombatHandler.getRemaining(player);
        if (remaining < 0)
            return;

        bossBar.setTitle(ChatColor.RED + ChatColor.BOLD.toString() + "Combat Tag" + ChatColor.GRAY + ": " + ChatColor.RED + (remaining / 1000) + "s");
        bossBar.setProgress((double) remaining / CombatHandler.TAG_TIME);
    }

    public static void remove(Player player) {
        bossBarMap.remove(player.getUniqueId());
    }
}
