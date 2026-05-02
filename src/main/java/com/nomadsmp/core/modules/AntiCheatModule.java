package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Random;

public class AntiCheatModule {

    private final NomadCore plugin;

    public AntiCheatModule(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        var config = plugin.getConfigManager();

        if (config.isOreObfuscationEnabled()) {
            configureOreObfuscation();
        }

        if (config.isScrambleStructureSeedsEnabled()) {
            scrambleStructureSeeds();
        }

        plugin.getLogger().info("Anti-cheat module enabled.");
    }

    private void configureOreObfuscation() {
        try {
            File paperConfig = new File("config/paper-world-defaults.yml");
            if (!paperConfig.exists()) {
                plugin.getLogger().warning("paper-world-defaults.yml not found, skipping ore obfuscation config.");
                return;
            }
            plugin.getLogger().info("Ore obfuscation: ensure paper-world-defaults.yml has anticheat.anti-xray.enabled=true and engine-mode=2");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to configure ore obfuscation: " + e.getMessage());
        }
    }

    private void scrambleStructureSeeds() {
        try {
            File paperConfig = new File("config/paper-world-defaults.yml");
            if (!paperConfig.exists()) {
                plugin.getLogger().warning("paper-world-defaults.yml not found, skipping seed scrambling.");
                return;
            }
            plugin.getLogger().info("Structure seed scrambling: ensure feature-seeds are randomized in paper-world-defaults.yml");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to scramble structure seeds: " + e.getMessage());
        }
    }
}
