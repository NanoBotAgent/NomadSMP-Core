package com.nomadsmp.core.utils;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks lightweight server stats for /nomad stats.
 * Persisted to stats.yml.
 */
public class StatsManager {

    private final NomadCore plugin;
    private final File file;
    private final YamlConfiguration yaml;

    private final AtomicLong totalJoins = new AtomicLong();
    private final AtomicLong buffActivations = new AtomicLong();
    private final AtomicLong migrationCount = new AtomicLong();
    private final AtomicLong timberUses = new AtomicLong();
    private final AtomicLong veinMinerUses = new AtomicLong();
    private long serverStartMillis;

    public StatsManager(NomadCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        loadFromYaml();
        this.serverStartMillis = System.currentTimeMillis();
    }

    private void loadFromYaml() {
        totalJoins.set(yaml.getLong("total-joins", 0));
        buffActivations.set(yaml.getLong("buff-activations", 0));
        migrationCount.set(yaml.getLong("migration-count", 0));
        timberUses.set(yaml.getLong("timber-uses", 0));
        veinMinerUses.set(yaml.getLong("vein-miner-uses", 0));
    }

    public void save() {
        yaml.set("total-joins", totalJoins.get());
        yaml.set("buff-activations", buffActivations.get());
        yaml.set("migration-count", migrationCount.get());
        yaml.set("timber-uses", timberUses.get());
        yaml.set("vein-miner-uses", veinMinerUses.get());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats.yml: " + e.getMessage());
        }
    }

    // ─── Incrementers ───
    public void recordJoin() { totalJoins.incrementAndGet(); }
    public void recordBuffActivation() { buffActivations.incrementAndGet(); }
    public void recordMigration() { migrationCount.incrementAndGet(); }
    public void recordTimberUse() { timberUses.incrementAndGet(); }
    public void recordVeinMinerUse() { veinMinerUses.incrementAndGet(); }

    // ─── Getters ───
    public long getTotalJoins() { return totalJoins.get(); }
    public long getBuffActivations() { return buffActivations.get(); }
    public long getMigrationCount() { return migrationCount.get(); }
    public long getTimberUses() { return timberUses.get(); }
    public long getVeinMinerUses() { return veinMinerUses.get(); }
    public long getUptimeSeconds() { return (System.currentTimeMillis() - serverStartMillis) / 1000; }
}
