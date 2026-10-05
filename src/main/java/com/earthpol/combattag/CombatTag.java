package com.earthpol.combattag;

import com.earthpol.combattag.api.CombatTagApi;
import com.earthpol.combattag.combat.CombatHandler;
import com.earthpol.combattag.combat.CombatTagRuntime;
import com.earthpol.combattag.combat.actionbar.ActionBarTask;
import com.earthpol.combattag.combat.listener.CombatListener;
import com.earthpol.combattag.commands.CombatTagCommand;
import com.earthpol.combattag.placeholders.TaggedPlaceholder;
import com.earthpol.combattag.util.ReloadableConfig;
import com.earthpol.earthpollib.config.ReloadableConfigHandler;
import com.earthpol.earthpollib.translation.TranslationService;
import com.earthpol.earthpollib.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
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

    private ActionBarTask actionBarTask;

    @Override
    public void onEnable() {
        instance = this;
        translationService = new TranslationService(this, CombatTag.class);
        translationService.load();

        log.info(Translations.text(translationService, "plugin.startup.banner"));
        log.info(Translations.text(translationService, "plugin.startup.support-discord"));

        // config must load before any task or service starts
        try {
            configHandler =  new ReloadableConfigHandler<>(
                    this,
                    "config.yml",
                    ReloadableConfig.class
            );
        } catch (IOException e) {
            Bukkit.getLogger().severe(e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CombatTagRuntime runtime = CombatHandler.start(this);
        actionBarTask = new ActionBarTask(this, runtime);
        actionBarTask.start();
        setupListeners(runtime);
        setupCommands(runtime);
        new TaggedPlaceholder().register();
        getServer().getServicesManager().register(CombatTagApi.class, CombatHandler.api(), this, ServicePriority.Normal);
        log.info(Translations.text(translationService, "plugin.startup.enabled"));
    }


    private void setupListeners(CombatTagRuntime runtime){
        getServer().getPluginManager().registerEvents(new CombatListener(runtime, actionBarTask), this);
    }

    private void setupCommands(CombatTagRuntime runtime){
        Objects.requireNonNull(getCommand("combattag")).setExecutor(new CombatTagCommand(this, runtime));
    }

    @Override
    public void onDisable() {
        if (actionBarTask != null) {
            actionBarTask.stop();
            actionBarTask = null;
        }
        getServer().getServicesManager().unregisterAll(this);
        CombatHandler.stop();
    }
}
