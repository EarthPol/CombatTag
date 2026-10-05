package com.earthpol.combattag.commands;

import com.earthpol.combattag.CombatTag;
import com.earthpol.combattag.combat.CombatTagRuntime;
import com.earthpol.earthpollib.config.ReloadableConfigHandler;
import com.earthpol.earthpollib.translation.TranslationService;
import com.earthpol.earthpollib.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CombatTagCommand implements CommandExecutor {

    private final Plugin plugin;
    private final CombatTagRuntime runtime;

    public CombatTagCommand(Plugin plugin, CombatTagRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        TranslationService translationService = CombatTag.getTranslationService();
        FeedbackRoute feedback = feedbackRoute(sender);

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
                parseTagCommand(feedback, args);
                break;
            case "untag":
                parseUntagCommand(feedback, args);
                break;
            case "reload":
                reload(CombatTag.getConfigHandler(), sender);
                break;
            default:
                Translations.sendPrefixed(
                    CombatTag.getTranslationService(),
                    sender,
                    "commands.errors.incorrect-usage",
                    (Object) "/combattag help"
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

    private void parseTagCommand(FeedbackRoute feedback, String[] args) {
        CommandSender sender = feedback.sender();

        if (args.length < 2) {
            if (sender instanceof Player player) {
                tagPlayer(feedback, player);
            } else {
                Translations.sendPrefixed(
                    CombatTag.getTranslationService(),
                    sender,
                    "commands.errors.incorrect-usage",
                    (Object) "/combattag tag <username>"
                );
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Translations.sendPrefixed(CombatTag.getTranslationService(), sender, "commands.errors.player-not-found");
            return;
        }

        tagPlayer(feedback, target);
    }

    private void parseUntagCommand(FeedbackRoute feedback, String[] args) {
        CommandSender sender = feedback.sender();

        if (args.length < 2) {
            if (sender instanceof Player player) {
                untagPlayer(feedback, player);
            } else {
                Translations.sendPrefixed(
                    CombatTag.getTranslationService(),
                    sender,
                    "commands.errors.incorrect-usage",
                    (Object) "/combattag untag <username>"
                );
            }
            return;
        }

        if (Objects.equals(args[1], "*")) {
            untagAllPlayers(feedback);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Translations.sendPrefixed(CombatTag.getTranslationService(), sender, "commands.errors.player-not-found");
            return;
        }

        untagPlayer(feedback, target);
    }

    private void tagPlayer(FeedbackRoute feedback, Player target) {
        runtime.runForPlayer(target, () -> {
            runtime.applyTag(target);
            feedback.send("commands.tag.success", target.getName());
        });
    }

    private void untagPlayer(FeedbackRoute feedback, Player target) {
        runtime.runForPlayer(target, () -> {
            if (!runtime.isTagged(target.getUniqueId())) {
                feedback.send("commands.errors.player-not-tagged");
                return;
            }
            runtime.removeTag(target);
            feedback.send("commands.untag.success", target.getName());
        });
    }

    private void untagAllPlayers(FeedbackRoute feedback) {
        // enumerate on the global region then touch each player on the owning thread
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> {
            if (!runtime.isActive()) {
                return;
            }
            for (Player target : Bukkit.getOnlinePlayers()) {
                runtime.runForPlayer(target, () -> {
                    if (!runtime.isTagged(target.getUniqueId())) {
                        return;
                    }
                    runtime.removeTag(target);
                    feedback.send("commands.untag.success", target.getName());
                });
            }
        });
    }

    private FeedbackRoute feedbackRoute(CommandSender sender) {
        CommandSender source = deliverySource(sender);
        if (source instanceof Player player) {
            return new FeedbackRoute(sender, player, null);
        }
        if (source instanceof BlockCommandSender block) {
            // captured at invocation so remote callbacks never read the block
            return new FeedbackRoute(sender, null, block.getBlock().getLocation());
        }
        return new FeedbackRoute(sender, null, null);
    }

    // proxied senders forward their output to the caller, nested proxies keep unwrapping
    private static CommandSender deliverySource(CommandSender sender) {
        CommandSender source = sender;
        while (source instanceof ProxiedCommandSender proxied) {
            CommandSender caller = proxied.getCaller();
            if (caller == null || caller == source) {
                return source;
            }
            source = caller;
        }
        return source;
    }

    // Delivers feedback on the thread that owns the sender captured at invocation time
    private final class FeedbackRoute {

        private final CommandSender sender;
        private final Player player;
        private final Location blockLocation;

        private FeedbackRoute(CommandSender sender, Player player, Location blockLocation) {
            this.sender = sender;
            this.player = player;
            this.blockLocation = blockLocation;
        }

        private CommandSender sender() {
            return sender;
        }

        private void send(String key, Object... args) {
            if (player != null) {
                runtime.runForPlayer(player, () -> sendNow(key, args));
                return;
            }
            if (blockLocation != null) {
                Bukkit.getRegionScheduler().run(plugin, blockLocation, scheduled -> {
                    if (!runtime.isActive()) {
                        return;
                    }
                    sendNow(key, args);
                });
                return;
            }
            if (Bukkit.isGlobalTickThread()) {
                sendNow(key, args);
                return;
            }
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> {
                if (!runtime.isActive()) {
                    return;
                }
                sendNow(key, args);
            });
        }

        private void sendNow(String key, Object... args) {
            Translations.sendPrefixed(CombatTag.getTranslationService(), sender, key, args);
        }
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
