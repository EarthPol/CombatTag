package com.earthpol.combattag;

import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.combattag.combat.actionbar.ActionBarTask;
import com.earthpol.combattag.combat.listener.CombatListener;
import com.earthpol.combattag.commands.CombatTagCommand;
import com.earthpol.combattag.placeholders.TaggedPlaceholder;
import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import java.util.logging.Logger;

public final class CombatTag extends JavaPlugin {

    private static CombatTag instance;

    public static CombatTag getInstance(){
        return instance;
    }
    private static Logger log = Bukkit.getLogger();

    @Override
    public void onEnable() {
        instance = this;
        log.info("§e======= §aCombatTag §e=======");
        log.info("§eSupport Discord: §ahttps://discord.gg/epmc");
        setupListeners();
        setupCommands();
        runTasks();
        log.info("§e CombatTag has been §aenabled§e.");
        new TaggedPlaceholder().register();
    }

    private void setupListeners(){
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
    }

    private void setupCommands(){
        Objects.requireNonNull(getCommand("combattag")).setExecutor(new CombatTagCommand());
    }

    private void runTasks(){
        FoliaTaskScheduler scheduler = new FoliaTaskScheduler(this);
        //scheduler.runAsyncRepeating(new BossBarTask(), 10L, 10L);
        scheduler.runAsyncRepeating(new ActionBarTask(), 10L, 10L);
    }

    @Override
    public void onDisable() {
        for (Player p: this.getServer().getOnlinePlayers()) {
            if (CombatHandler.isTagged(p)){
                CombatHandler.removeTag(p);
            }
        }
    }
}
