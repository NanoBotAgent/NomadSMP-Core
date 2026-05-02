package com.nomadsmp.core;

import com.nomadsmp.core.commands.NomadCommand;
import com.nomadsmp.core.config.ConfigManager;
import com.nomadsmp.core.listeners.*;
import com.nomadsmp.core.modules.*;
import com.nomadsmp.core.utils.HomeStorage;
import com.nomadsmp.core.utils.StatsManager;
import com.nomadsmp.core.utils.BuffStateStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NomadCore extends JavaPlugin {

    private static NomadCore instance;
    private ConfigManager configManager;
    private HomeStorage homeStorage;
    private StatsManager statsManager;
    private BuffStateStorage buffStateStorage;

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

        // Initialize storage
        homeStorage = new HomeStorage(this);
        statsManager = new StatsManager(this);
        buffStateStorage = new BuffStateStorage(this);

        // Initialize modules
        nomadModule = new NomadModule(this);
        dailyBuffModule = new DailyBuffModule(this);
        progressionLockModule = new ProgressionLockModule(this);
        antiCheatModule = new AntiCheatModule(this);
        socialModule = new SocialModule(this);

        // Enable modules (each checks its own enabled flag)
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
        // Save buff state for persistence across restarts
        if (dailyBuffModule != null) {
            buffStateStorage.save(dailyBuffModule.getCurrentBuffIds());
            dailyBuffModule.disable();
        }
        // Save stats
        if (statsManager != null) statsManager.save();
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

    // ─── Notification helpers ───

    /** Broadcast a message to ALL online players (used for Buff of the Day changes). */
    public void broadcastAll(String message) {
        Bukkit.broadcastMessage(message);
    }

    /** Notify all online operators (used for non-buff config changes). */
    public void notifyOps(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("nomad.admin")) {
                player.sendMessage(message);
            }
        }
    }

    /** Notify only the operator who made the change (self-only). */
    public void notifySelf(Player operator, String message) {
        operator.sendMessage(message);
    }

    public static NomadCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public HomeStorage getHomeStorage() { return homeStorage; }
    public StatsManager getStatsManager() { return statsManager; }
    public BuffStateStorage getBuffStateStorage() { return buffStateStorage; }
    public NomadModule getNomadModule() { return nomadModule; }
    public DailyBuffModule getDailyBuffModule() { return dailyBuffModule; }
    public ProgressionLockModule getProgressionLockModule() { return progressionLockModule; }
    public AntiCheatModule getAntiCheatModule() { return antiCheatModule; }
    public SocialModule getSocialModule() { return socialModule; }
}
