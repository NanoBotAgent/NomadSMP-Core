package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

/**
 * Merges new default config keys into an existing user config.yml
 * without overwriting the user's customized values.
 *
 * Strategy:
 * 1. Load the user's existing config.yml from disk
 * 2. Load the JAR's default config.yml as the "template"
 * 3. Walk the template tree — for every key missing from the user's config,
 *    copy the default value in
 * 4. Add a config-version header so we can detect when migration is needed
 * 5. Save the merged result back to disk
 */
public class ConfigMigrator {

    private final NomadCore plugin;
    private static final String VERSION_KEY = "config-version";

    public ConfigMigrator(NomadCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Run config migration. Returns true if any new keys were added.
     */
    public boolean migrate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // First run — saveDefaultConfig will handle it
            return false;
        }

        // Load user's current config
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        // Load JAR defaults as template
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            plugin.getLogger().warning("No default config.yml found in JAR — skipping migration");
            return false;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
            new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        int userVersion = userConfig.getInt(VERSION_KEY, 0);
        int defaultVersion = defaultConfig.getInt(VERSION_KEY, 0);

        if (userVersion >= defaultVersion && defaultVersion > 0) {
            // User config is up to date
            return false;
        }

        plugin.getLogger().info("Config migration: v" + userVersion + " -> v" + defaultVersion);

        // Merge: walk the default config tree and add missing keys
        boolean changed = mergeSection(userConfig, defaultConfig, "");

        if (changed) {
            // Preserve the user's values — only add missing keys
            userConfig.set(VERSION_KEY, defaultVersion);

            // Backup the old config before saving
            backupConfig(configFile);

            try {
                userConfig.save(configFile);
                plugin.getLogger().info("Config migration complete — new keys added, existing values preserved.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save migrated config!", e);
            }
        }

        return changed;
    }

    /**
     * Recursively merge missing keys from defaults into user config.
     * Never overwrites existing user values.
     */
    private boolean mergeSection(ConfigurationSection user, ConfigurationSection defaults, String path) {
        boolean changed = false;

        for (String key : defaults.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (defaults.isConfigurationSection(key)) {
                // Nested section — recurse
                if (!user.contains(key)) {
                    // Section entirely missing — copy it
                    copySection(user, defaults.getConfigurationSection(key), key);
                    changed = true;
                } else if (user.isConfigurationSection(key)) {
                    // Both have the section — recurse to find missing sub-keys
                    changed |= mergeSection(
                        user.getConfigurationSection(key),
                        defaults.getConfigurationSection(key),
                        fullPath
                    );
                }
                // If user has a scalar at this key but default has a section,
                // don't overwrite the user's value
            } else {
                // Scalar value
                if (!user.contains(key)) {
                    user.set(key, defaults.get(key));
                    plugin.getLogger().fine("Config migration: added missing key '" + fullPath + "' = " + defaults.get(key));
                    changed = true;
                }
                // If key exists in user config, keep the user's value
            }
        }

        return changed;
    }

    /**
     * Deep-copy a section from defaults into the user config.
     */
    private void copySection(ConfigurationSection user, ConfigurationSection source, String path) {
        for (String key : source.getKeys(false)) {
            String childPath = path + "." + key;
            if (source.isConfigurationSection(key)) {
                copySection(user, source.getConfigurationSection(key), childPath);
            } else {
                user.set(childPath, source.get(key));
            }
        }
    }

    /**
     * Create a backup of the old config before migration writes changes.
     */
    private void backupConfig(File configFile) {
        File backup = new File(plugin.getDataFolder(), "config.yml.bak");
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Old config backed up to config.yml.bak");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to backup config.yml", e);
        }
    }
}
