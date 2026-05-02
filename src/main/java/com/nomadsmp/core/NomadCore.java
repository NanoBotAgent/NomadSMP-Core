package com.nomadsmp.core;

import com.nomadsmp.core.commands.NomadCommand;
import com.nomadsmp.core.config.ConfigManager;
import com.nomadsmp.core.listeners.*;
import com.nomadsmp.core.modules.*;
import com.nomadsmp.core.utils.HomeStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NomadCore extends JavaPlugin {

    private static NomadCore instance;
    private ConfigManager configManager;
    private HomeStorage homeStorage;

    // Modules
    private NomadModule nomadModule;
    private DailyBuffModule dailyBuffModule;
    private ProgressionLockModule progressionLockModule;
    private AntiCheatModule antiCheatModule;
    private SocialModule socialModule;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        // Initialize home storage
        homeStorage = new HomeStorage(this);

        // Initialize modules
        nomadModule = new NomadModule(this);
        dailyBuffModule = new DailyBuffModule(this);
        progressionLockModule = new ProgressionLockModule(this);
        antiCheatModule = new AntiCheatModule(this);
        socialModule = new SocialModule(this);

        // Enable modules
        if (configManager.isNomadSystemEnabled()) {
            nomadModule.enable();
        }
        if (configManager.isDailyBuffsEnabled()) {
            dailyBuffModule.enable();
        }
        if (configManager.isProgressionLockEnabled()) {
            progressionLockModule.enable();
        }
        if (configManager.isAntiCheatEnabled()) {
            antiCheatModule.enable();
        }
        if (configManager.isSocialEnabled()) {
            socialModule.enable();
        }

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new BuffListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new ProgressionListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new AntiCheatListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new SocialListeners(this), this);

        // Register commands
        if (getCommand("nomad") != null) {
            getCommand("nomad").setExecutor(new NomadCommand(this));
        }

        getLogger().info("NomadSMP-Core enabled! All modules loaded.");
    }

    @Override
    public void onDisable() {
        if (dailyBuffModule != null) dailyBuffModule.disable();
        if (nomadModule != null) nomadModule.disable();
        getLogger().info("NomadSMP-Core disabled.");
    }

    public void reload() {
        reloadConfig();
        configManager.load();
        // Re-apply border, re-schedule tasks
        if (socialModule != null) socialModule.enable();
        if (dailyBuffModule != null) dailyBuffModule.updateDailyBuff();
        getLogger().info("NomadSMP-Core config reloaded.");
    }

    public static NomadCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public HomeStorage getHomeStorage() { return homeStorage; }
    public NomadModule getNomadModule() { return nomadModule; }
    public DailyBuffModule getDailyBuffModule() { return dailyBuffModule; }
    public ProgressionLockModule getProgressionLockModule() { return progressionLockModule; }
    public AntiCheatModule getAntiCheatModule() { return antiCheatModule; }
    public SocialModule getSocialModule() { return socialModule; }
}
