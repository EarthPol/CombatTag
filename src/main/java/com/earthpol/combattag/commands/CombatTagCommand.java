package com.earthpol.combattag.commands;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.earthPolLib.config.ReloadableConfigHandler;
import com.earthpol.earthPolLib.translation.TranslationService;
import com.earthpol.earthPolLib.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CombatTagCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        TranslationService translationService = CombatTag.getTranslationService();

        if (!sender.hasPermission("earthpol.command.combattag")) {
            Translations.sendPrefixed(translationService, sender, "commands.errors.no-permission");
            return true;
        }

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
                Translations.sendPrefixed(
                    CombatTag.getTranslationService(),
                    sender,
                    "commands.errors.incorrect-usage",
                    "/combattag help"
                );
                break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        TranslationService translationService = CombatTag.getTranslationService();
        Translations.sendPrefixed(translationService, sender, "commands.help.header");
        Translations.sendPrefixed(translationService, sender, "commands.help.tag");
        Translations.sendPrefixed(translationService, sender, "commands.help.untag");
        Translations.sendPrefixed(translationService, sender, "commands.help.reload");
    }

    private void parseTagCommand(CommandSender sender, String[] args) {
        Player target;
        TranslationService translationService = CombatTag.getTranslationService();

        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                Translations.sendPrefixed(
                    translationService,
                    sender,
                    "commands.errors.incorrect-usage",
                    "/combattag tag <username>"
                );
                return;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                Translations.sendPrefixed(translationService, sender, "commands.errors.player-not-found");
                return;
            }
        }

        CombatHandler.applyTag(target);
        Translations.sendPrefixed(translationService, sender, "commands.tag.success", target.getName());
    }

    private void parseUntagCommand(CommandSender sender, String[] args) {
        Player target = null;
        TranslationService translationService = CombatTag.getTranslationService();

        if (args.length < 2) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                Translations.sendPrefixed(
                    translationService,
                    sender,
                    "commands.errors.incorrect-usage",
                    "/combattag untag <username>"
                );
                return;
            }
        } else if (Objects.equals(args[1], "*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                target = p;
                if (CombatHandler.isTagged(target)) {
                    CombatHandler.removeTag(target);
                    Translations.sendPrefixed(translationService, sender, "commands.untag.success", target.getName());
                }

            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                Translations.sendPrefixed(translationService, sender, "commands.errors.player-not-found");
                return;
            }
        }

        assert target != null;
        if (!CombatHandler.isTagged(target)) {
            Translations.sendPrefixed(translationService, sender, "commands.errors.player-not-tagged");
            return;
        }

        CombatHandler.removeTag(target);
        Translations.sendPrefixed(translationService, sender, "commands.untag.success", target.getName());

    }

    public static void reload(ReloadableConfigHandler reloadableConfigHandler, CommandSender sender) {
        TranslationService translationService = CombatTag.getTranslationService();
        try{
            reloadableConfigHandler.reload();
            Bukkit.getLogger().info(Translations.raw(translationService, "plugin.reload.success-console"));
            Translations.sendPrefixed(translationService, sender, "commands.reload.success");
        }catch(Exception ex){
            Bukkit.getLogger().severe(Translations.raw(translationService, "plugin.reload.failed-console", ex.toString()));
            Translations.sendPrefixed(translationService, sender, "commands.reload.failed");
        }

    }

}
