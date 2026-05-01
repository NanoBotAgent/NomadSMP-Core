package com.nomadsmp.core;

import com.nomadsmp.core.config.ConfigManager;
import com.nomadsmp.core.modules.*;
import com.nomadsmp.core.listeners.*;
import com.nomadsmp.core.commands.NomadCommand;
import com.nomadsmp.core.utils.HomeStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NomadCore extends JavaPlugin {

    private static NomadCore instance;
    private ConfigManager configManager;
    private HomeStorage homeStorage;
    private NomadModule nomadModule;
    private DailyBuffModule dailyBuffModule;
    private ProgressionLockModule progressionLockModule;
    private AntiCheatModule antiCheatModule;
    private SocialModule socialModule;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        homeStorage = new HomeStorage(this);

        nomadModule = new NomadModule(this);
        dailyBuffModule = new DailyBuffModule(this);
        progressionLockModule = new ProgressionLockModule(this);
        antiCheatModule = new AntiCheatModule(this);
        socialModule = new SocialModule(this);

        if (configManager.isNomadEnabled()) nomadModule.enable();
        if (configManager.isDailyBuffsEnabled()) dailyBuffModule.enable();
        if (configManager.isProgressionLockEnabled()) progressionLockModule.enable();
        if (configManager.isAntiCheatEnabled()) antiCheatModule.enable();
        if (configManager.isSocialEnabled()) socialModule.enable();

        getServer().getPluginManager().registerEvents(new BuffListeners(this), this);
        getServer().getPluginManager().registerEvents(new ProgressionListeners(this), this);
        getServer().getPluginManager().registerEvents(new AntiCheatListeners(this), this);
        getServer().getPluginManager().registerEvents(new SocialListeners(this), this);

        if (getCommand("nomad") != null) {
            getCommand("nomad").setExecutor(new NomadCommand(this));
        }
        getLogger().info("NomadSMP-Core enabled!");
    }

    @Override
    public void onDisable() {
        if (nomadModule != null) nomadModule.disable();
        if (dailyBuffModule != null) dailyBuffModule.disable();
        if (homeStorage != null) homeStorage.save();
        getLogger().info("NomadSMP-Core disabled.");
    }

    public static NomadCore inst() { return instance; }
    public ConfigManager cfg() { return configManager; }
    public HomeStorage homes() { return homeStorage; }
    public NomadModule nomad() { return nomadModule; }
    public DailyBuffModule buffs() { return dailyBuffModule; }
    public ProgressionLockModule prog() { return progressionLockModule; }
    public AntiCheatModule anticheat() { return antiCheatModule; }
    public SocialModule social() { return socialModule; }
}
