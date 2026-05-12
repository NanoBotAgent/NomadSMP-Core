package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Merges new default config keys into an existing user config.yml
 * without overwriting the user's customized values.
 *
 * Strategy:
 * 1. Load the JAR's default config.yml as the "template"
 * 2. Walk the template tree — for every key missing from the user's config,
 *    add it via config.set() (preserves comments in the existing file)
 * 3. Add a config-version key so we can detect when migration is needed
 * 4. Save via plugin.saveConfig() which uses Bukkit's safe writer
 */
public class ConfigMigrator {

    private final NomadCore plugin;
    private static final String VERSION_KEY = "config-version";

    public ConfigMigrator(NomadCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Run config migration. Returns true if any new keys were added.
     * This must be called AFTER saveDefaultConfig() and BEFORE configManager.load().
     */
    public boolean migrate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // First run — saveDefaultConfig already handled it
            return false;
        }

        // Load user's current config (already loaded by saveDefaultConfig/reloadConfig)
        FileConfiguration userConfig = plugin.getConfig();

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

        if (defaultVersion > 0 && userVersion >= defaultVersion) {
            // User config is up to date
            return false;
        }

        plugin.getLogger().info("Config migration: v" + userVersion + " -> v" + defaultVersion);

        // Merge: walk the default config tree and add missing keys
        boolean changed = mergeSection(userConfig, defaultConfig, "");

        if (changed) {
            userConfig.set(VERSION_KEY, defaultVersion);

            try {
                plugin.saveConfig();
                plugin.getLogger().info("Config migration complete — new keys added, existing values preserved.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save migrated config!", e);
            }
        }

        return changed;
    }

    /**
     * Recursively merge missing keys from defaults into user config.
     * Never overwrites existing user values.
     */
    private boolean mergeSection(FileConfiguration user, FileConfiguration defaults, String path) {
        boolean changed = false;

        for (String key : defaults.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (defaults.isConfigurationSection(key)) {
                if (!user.contains(key)) {
                    // Section entirely missing — copy all its values
                    copySection(user, defaults.getConfigurationSection(key), fullPath);
                    changed = true;
                } else if (user.isConfigurationSection(key)) {
                    // Both have the section — recurse to find missing sub-keys
                    // We need to create subsection views for recursion
                    changed |= mergeSubSection(user, defaults.getConfigurationSection(key), fullPath);
                }
            } else {
                // Scalar value
                if (!user.contains(key)) {
                    user.set(fullPath, defaults.get(key));
                    plugin.getLogger().fine("Config migration: added '" + fullPath + "' = " + defaults.get(key));
                    changed = true;
                }
            }
        }

        return changed;
    }

    /**
     * Merge a subsection by using full dotted paths.
     */
    private boolean mergeSubSection(FileConfiguration user, org.bukkit.configuration.ConfigurationSection section, String pathPrefix) {
        boolean changed = false;
        for (String key : section.getKeys(false)) {
            String fullPath = pathPrefix + "." + key;
            if (section.isConfigurationSection(key)) {
                if (!user.contains(fullPath)) {
                    copySection(user, section.getConfigurationSection(key), fullPath);
                    changed = true;
                } else {
                    changed |= mergeSubSection(user, section.getConfigurationSection(key), fullPath);
                }
            } else {
                if (!user.contains(fullPath)) {
                    user.set(fullPath, section.get(key));
                    plugin.getLogger().fine("Config migration: added '" + fullPath + "' = " + section.get(key));
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Deep-copy a section from defaults into the user config using full dotted paths.
     */
    private void copySection(FileConfiguration user, org.bukkit.configuration.ConfigurationSection source, String pathPrefix) {
        for (String key : source.getKeys(false)) {
            String fullPath = pathPrefix + "." + key;
            if (source.isConfigurationSection(key)) {
                copySection(user, source.getConfigurationSection(key), fullPath);
            } else {
                user.set(fullPath, source.get(key));
            }
        }
    }
}
