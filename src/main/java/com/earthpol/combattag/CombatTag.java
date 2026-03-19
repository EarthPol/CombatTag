package com.earthpol.combattag;

import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.combattag.combat.actionbar.ActionBarTask;
import com.earthpol.combattag.combat.listener.CombatListener;
import com.earthpol.combattag.commands.CombatTagCommand;
import com.earthpol.combattag.placeholders.TaggedPlaceholder;
import com.earthpol.combattag.util.ReloadableConfig;
import com.earthpol.earthPolLib.config.ReloadableConfigHandler;
import com.earthpol.earthPolLib.translation.TranslationService;
import com.earthpol.earthPolLib.translation.Translations;
import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;

import java.util.logging.Logger;

public final class CombatTag extends JavaPlugin {

    private static CombatTag instance;

    public static CombatTag getInstance(){
        return instance;
    }
    private static Logger log = Bukkit.getLogger();

    public static ReloadableConfigHandler<ReloadableConfig> configHandler;
    public static ReloadableConfigHandler<ReloadableConfig> getConfigHandler(){ return configHandler; }
    private static TranslationService translationService;
    public static TranslationService getTranslationService() { return translationService; }

    @Override
    public void onEnable() {
        instance = this;
        translationService = new TranslationService(this, CombatTag.class);
        translationService.load();

        log.info(Translations.text(translationService, "plugin.startup.banner"));
        log.info(Translations.text(translationService, "plugin.startup.support-discord"));
        setupListeners();
        setupCommands();
        runTasks();
        log.info(Translations.text(translationService, "plugin.startup.enabled"));
        new TaggedPlaceholder().register();

        try {
            configHandler =  new ReloadableConfigHandler<>(
                    this,
                    "config.yml",
                    ReloadableConfig.class
            );
        } catch (IOException e) {
            Bukkit.getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
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
