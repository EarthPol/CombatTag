package com.earthpol.combattag.commands;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.earthPolLib.config.ReloadableConfigHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Logger;

public class CombatTagCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length < 1) {
            showHelp(sender);
            return true;
        }


        switch (args[0]) {
            case "help":
                showHelp(sender);
                break;
            case "tag":
                parseTagCommand(sender, args);
                break;
            case "untag":
                parseUntagCommand(sender, args);
                break;
            case "reload":
                reload(CombatTag.getConfigHandler(), sender);
                break;
            default:
                sender.sendMessage("§7[§bCombatTag§7]: §eIncorrect Usage: §e/combattag help");
                break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(
                "§7[§bCombatTag§7]: §eHelp Command",
                "§7[§bCombatTag§7]: §e/combattag tag <username>",
                "§7[§bCombatTag§7]: §e/combattag untag <username>",
                "§7[§bCombatTag§7]: §e/combattag reload"
        );
    }

    private void parseTagCommand(CommandSender sender, String[] args) {
        Player target;

        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§7[§bCombatTag§7]: §eIncorrect Usage: §e/combattag tag <username>");
                return;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§7[§bCombatTag§7]: §cPlayer doesn't exist.");
                return;
            }
        }

        CombatHandler.applyTag(target);
        sender.sendMessage("§7[§bCombatTag§7]: §e" + target.getName() + " has been tagged.");
    }

    private void parseUntagCommand(CommandSender sender, String[] args) {
        Player target = null;

        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§7[§bCombatTag§7]: §eIncorrect Usage: §e/combattag untag <username>");
                return;
            }
        } else if (Objects.equals(args[1], "*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                target = p;
                if (CombatHandler.isTagged(target)) {
                    CombatHandler.removeTag(target);
                    sender.sendMessage("§7[§bCombatTag§7]: §e" + target.getName() + " has been untagged.");
                }

            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§7[§bCombatTag§7]: §cPlayer doesn't exist.");
                return;
            }
        }

        assert target != null;
        if (!CombatHandler.isTagged(target)) {
            sender.sendMessage("§7[§bCombatTag§7]: §cPlayer is not combat tagged.");
            return;
        }

        CombatHandler.removeTag(target);
        sender.sendMessage("§7[§bCombatTag§7]: §e" + target.getName() + " has been untagged.");

    }

    public static void reload(ReloadableConfigHandler reloadableConfigHandler, CommandSender sender) {
        try{
            reloadableConfigHandler.reload();
            Bukkit.getLogger().info("CombatTag has reloaded.");
            sender.sendMessage("§6CombatTag §fhas reloaded");
        }catch(Exception ex){
            Bukkit.getLogger().severe("Reload command failed!" + ex);
            sender.sendMessage("§4CombatTag reload failed. Check console for more detail.");
        }

    }

}
