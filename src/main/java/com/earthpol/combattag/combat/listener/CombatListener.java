package com.earthpol.combattag.combat.listener;

//import com.gmail.goosius.siegewar.SiegeController;
//import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
import com.google.common.collect.ImmutableSet;
import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.combattag.combat.bossbar.BossBarTask;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

public class CombatListener implements Listener {
    private Set<UUID> deathsForLoggingOut = new HashSet<>();
    private Random random = new Random();


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player damagee = (Player) event.getEntity();
        Player damager;

        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (!(shooter instanceof Player))
                return;

            damager = (Player) shooter;
        } else {
            return;
        }

        if (damager.equals(damagee))
            return;

        CombatHandler.applyTag(damagee);
        CombatHandler.applyTag(damager);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        BossBarTask.remove(player);

        if (!CombatHandler.isTagged(player))
            return;

        CombatHandler.removeTag(player);
        if(event.getReason().equals(PlayerQuitEvent.QuitReason.KICKED) || event.getReason().equals(PlayerQuitEvent.QuitReason.ERRONEOUS_STATE) || event.getReason().equals(PlayerQuitEvent.QuitReason.TIMED_OUT) )
            return;

        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
        if(townBlock != null && townBlock.getType() == TownBlockType.ARENA && townBlock.hasTown())
            return;

        deathsForLoggingOut.add(player.getUniqueId());
        player.setHealth(0.0);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (deathsForLoggingOut.contains(player.getUniqueId())) {
            deathsForLoggingOut.remove(player.getUniqueId());
            if(Objects.equals(GameRule.SHOW_DEATH_MESSAGES, true)) {
                event.deathMessage(Component.text(player.getName() + " was killed for logging out in combat."));
            }
        }

        if (!CombatHandler.isTagged(player))
            return;

        CombatHandler.removeTag(player);
    }

    private static final Set<String> WHITELISTED_COMMANDS = ImmutableSet.of("tc", "nc", "g", "ally", "msg", "r", "reply", "tell", "pm", "mod", "admin", "combattag", "lc");
    private static final Set<String> BLACKLISTED_COMMANDS = ImmutableSet.of("tfly", "townyflight:tfly", "townyfly", "townyflight", "townyflight:townyfly", "townyflight:townyflight", "sit");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event){
        Player player = event.getPlayer();
        if(CombatHandler.isTagged(player) && !player.hasPermission("polaris.command.combattag")) {
            String message = event.getMessage();
            message = message.replaceFirst("/", "");

            for (String value : WHITELISTED_COMMANDS) {
                if (message.equalsIgnoreCase(value) || message.toLowerCase().startsWith(value + " "))
                    return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can't use that command while being in combat.");
        }
        //Remove for non-siegewar:
        /* } else if(!CombatHandler.isTagged(player) && SiegeWarDistanceUtil.isLocationInActiveSiegeZone(player.getLocation())){
            String message = event.getMessage();
            message = message.replaceFirst("/", "");

            for (String value : BLACKLISTED_COMMANDS) {
                if (message.equalsIgnoreCase(value)){
                    event.setCancelled(true);
                    player.setFlying(false);
                    player.sendMessage(ChatColor.RED + "You can't use that command in a Siege Zone!");
                    return;
                }
            }
        }
        */
    }


    // Prevent claim hopping
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvP(TownyPlayerDamagePlayerEvent event) {
        if (!event.isCancelled())
            return;

        TownyWorld world = TownyAPI.getInstance().getTownyWorld(event.getVictimPlayer().getWorld().getName());
        Player attacker = event.getAttackingPlayer();
        Player victim = event.getVictimPlayer();

        assert world != null;
        if (!world.isFriendlyFireEnabled() && CombatUtil.isAlly(attacker.getName(), victim.getName()))
            return;

        if (!CombatHandler.isTagged(victim))
            return;

        event.setCancelled(false);
    }

    @EventHandler
    public void onArmorDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();

        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
        if(townBlock == null || townBlock.getType() != TownBlockType.ARENA)
            return;

        //Disable for non-siegewar
        //if(!townBlock.hasTown() || SiegeController.hasSiege(Objects.requireNonNull(TownyAPI.getInstance().getTown(player.getLocation()))))
        //    return;

        ItemStack item = event.getItem();
        if(!item.getType().toString().endsWith("HELMET") && !item.getType().toString().endsWith("CHESTPLATE") && !item.getType().toString().endsWith("LEGGINGS") && !item.getType().toString().endsWith("BOOTS"))
            return;

        if (10 < random.nextInt(100))
            return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onRiptide(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!CombatHandler.isTagged(player))
            return;

        if (!player.isRiptiding())
            return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "The riptide enchantment is disabled while being in combat.");
    }

}