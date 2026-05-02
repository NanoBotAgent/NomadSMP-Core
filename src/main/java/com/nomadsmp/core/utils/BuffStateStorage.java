package com.nomadsmp.core.utils;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists active buff state to buff-state.yml so it survives server restarts.
 * Stores the active buff IDs and the timestamp when they were applied.
 */
public class BuffStateStorage {

    private final NomadCore plugin;
    private final File file;
    private YamlConfiguration yaml;

    public BuffStateStorage(NomadCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "buff-state.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    /** Save the current buff state. */
    public void save(List<Integer> buffIds) {
        yaml.set("buff-ids", buffIds);
        yaml.set("applied-at", LocalDateTime.now().toString());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save buff-state.yml: " + e.getMessage());
        }
    }

    /** Load persisted buff IDs. Returns empty list if expired or missing. */
    public List<Integer> load() {
        if (!file.exists()) return List.of();
        yaml = YamlConfiguration.loadConfiguration(file);

        List<Integer> ids = yaml.getIntegerList("buff-ids");
        if (ids.isEmpty()) return List.of();

        // Check duration expiration
        String appliedAtStr = yaml.getString("applied-at", "");
        if (!appliedAtStr.isEmpty()) {
            try {
                LocalDateTime appliedAt = LocalDateTime.parse(appliedAtStr);
                int durationHours = plugin.getConfigManager().getDurationHours();
                if (durationHours > 0) {
                    long hoursSince = ChronoUnit.HOURS.between(appliedAt, LocalDateTime.now());
                    if (hoursSince >= durationHours) {
                        plugin.getLogger().info("Persisted buffs expired (" + hoursSince + "h >= " + durationHours + "h), clearing.");
                        return List.of();
                    }
                }
                // Check if it's a new day (midnight rollover)
                LocalDateTime now = LocalDateTime.now();
                if (appliedAt.toLocalDate().isBefore(now.toLocalDate())) {
                    plugin.getLogger().info("Persisted buffs are from a previous day, clearing.");
                    return List.of();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid applied-at timestamp in buff-state.yml, clearing.");
                return List.of();
            }
        }

        return ids;
    }

    /** Get the timestamp when buffs were applied, or null if not available. */
    public LocalDateTime getAppliedAt() {
        if (!file.exists()) return null;
        yaml = YamlConfiguration.loadConfiguration(file);
        String str = yaml.getString("applied-at", "");
        if (str.isEmpty()) return null;
        try {
            return LocalDateTime.parse(str);
        } catch (Exception e) {
            return null;
        }
    }
}
