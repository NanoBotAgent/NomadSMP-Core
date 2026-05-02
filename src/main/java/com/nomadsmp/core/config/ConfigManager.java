package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final NomadCore plugin;
    private FileConfiguration config;

    // Nomad system
    private boolean nomadSystemEnabled;
    private DayOfWeek migrateDay;
    private int migrateHour;
    private int houseRadius;
    private boolean safeLandCheck;

    // Daily buffs — per-day config
    private boolean dailyBuffsEnabled;
    private boolean broadcastOnJoin;
    private String broadcastColor;
    private int randomCount;
    private DayConfig[] dayConfigs; // Indexed by DayOfWeek ordinal (1=MON..7=SUN)

    // Progression lock
    private boolean progressionLockEnabled;
    private boolean endLocked;
    private int endUnlockDays;
    private String serverStartDate;
    private boolean netheriteCraftingBanned;
    private boolean netheriteEquipPunish;
    private boolean blockGamemodeCommand;
    private boolean blockGiveCommand;

    // Anti-cheat
    private boolean antiCheatEnabled;
    private boolean blockSeedCommand;
    private boolean scrambleStructureSeeds;
    private boolean oreObfuscation;

    // Social
    private boolean socialEnabled;
    private boolean worldBorderEnabled;
    private int worldBorderSize;
    private int worldBorderCenterX;
    private int worldBorderCenterZ;
    private double worldBorderDamageAmount;
    private int worldBorderWarningDistance;
    private boolean disableTpa;
    private boolean disableWarp;
    private boolean disableHomeCommand;
    private boolean playerHeadDrop;
    private boolean noAdminOp;

    /** Per-day buff configuration */
    public enum BuffMode { FIXED, RANDOM, OFF }

    public static class DayConfig {
        public final BuffMode mode;
        public final List<Integer> fixedBuffs;
        public final List<Integer> randomPool;

        public DayConfig(BuffMode mode, List<Integer> fixedBuffs, List<Integer> randomPool) {
            this.mode = mode;
            this.fixedBuffs = fixedBuffs;
            this.randomPool = randomPool;
        }
    }

    public ConfigManager(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = plugin.getConfig();
        loadValues();
    }

    private void loadValues() {
        // Nomad system
        nomadSystemEnabled = config.getBoolean("nomad-system.enabled", true);
        migrateDay = DayOfWeek.valueOf(config.getString("nomad-system.migrate-day", "MONDAY").toUpperCase());
        migrateHour = config.getInt("nomad-system.migrate-hour", 0);
        houseRadius = config.getInt("nomad-system.house-radius", 16);
        safeLandCheck = config.getBoolean("nomad-system.safe-land-check", true);

        // Daily buffs
        dailyBuffsEnabled = config.getBoolean("daily-buffs.enabled", true);
        broadcastOnJoin = config.getBoolean("daily-buffs.broadcast-on-join", true);
        broadcastColor = config.getString("daily-buffs.broadcast-color", "\u00a76");
        randomCount = config.getInt("daily-buffs.random-count", 1);

        // Per-day config (lowercase day names in YAML)
        dayConfigs = new DayConfig[8]; // 0 unused, 1-7 for MON-SUN
        String[] dayNames = {"", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (int i = 1; i <= 7; i++) {
            String dayKey = "daily-buffs." + dayNames[i];
            ConfigurationSection sec = config.getConfigurationSection(dayKey);
            if (sec != null) {
                BuffMode mode = switch (sec.getString("mode", "fixed").toLowerCase()) {
                    case "random" -> BuffMode.RANDOM;
                    case "off" -> BuffMode.OFF;
                    default -> BuffMode.FIXED;
                };
                List<Integer> buffs = sec.getIntegerList("buffs");
                List<Integer> pool = sec.getIntegerList("pool");
                dayConfigs[i] = new DayConfig(mode, buffs, pool);
            } else {
                // Fallback: try legacy format (monday: [2] as plain list)
                List<Integer> legacyBuffs = config.getIntegerList(dayKey);
                dayConfigs[i] = new DayConfig(legacyBuffs.isEmpty() ? BuffMode.OFF : BuffMode.FIXED, legacyBuffs, List.of());
            }
        }

        // Progression lock
        progressionLockEnabled = config.getBoolean("progression-lock.enabled", true);
        endLocked = config.getBoolean("progression-lock.end-locked", true);
        endUnlockDays = config.getInt("progression-lock.end-unlock-days", 30);
        serverStartDate = config.getString("progression-lock.server-start-date", "2025-01-01");
        netheriteCraftingBanned = config.getBoolean("progression-lock.netherite-crafting-banned", true);
        netheriteEquipPunish = config.getBoolean("progression-lock.netherite-equip-punish", true);
        blockGamemodeCommand = config.getBoolean("progression-lock.block-gamemode-command", true);
        blockGiveCommand = config.getBoolean("progression-lock.block-give-command", true);

        // Anti-cheat
        antiCheatEnabled = config.getBoolean("anti-cheat.enabled", true);
        blockSeedCommand = config.getBoolean("anti-cheat.block-seed-command", true);
        scrambleStructureSeeds = config.getBoolean("anti-cheat.scramble-structure-seeds", true);
        oreObfuscation = config.getBoolean("anti-cheat.ore-obfuscation", true);

        // Social — world border subsection
        socialEnabled = config.getBoolean("social.enabled", true);
        ConfigurationSection borderSec = config.getConfigurationSection("social.world-border");
        if (borderSec != null) {
            worldBorderEnabled = borderSec.getBoolean("enabled", true);
            worldBorderSize = borderSec.getInt("size", 500);
            worldBorderCenterX = borderSec.getInt("center-x", 0);
            worldBorderCenterZ = borderSec.getInt("center-z", 0);
            worldBorderDamageAmount = borderSec.getDouble("damage-amount", 0.5);
            worldBorderWarningDistance = borderSec.getInt("warning-distance", 10);
        } else {
            worldBorderEnabled = config.getBoolean("social.world-border", true);
            worldBorderSize = config.getInt("nomad-system.border-half-size", 250) * 2;
            worldBorderCenterX = 0;
            worldBorderCenterZ = 0;
            worldBorderDamageAmount = 0.5;
            worldBorderWarningDistance = 10;
        }
        disableTpa = config.getBoolean("social.disable-tpa", true);
        disableWarp = config.getBoolean("social.disable-warp", true);
        disableHomeCommand = config.getBoolean("social.disable-home-command", true);
        playerHeadDrop = config.getBoolean("social.player-head-drop", true);
        noAdminOp = config.getBoolean("social.no-admin-op", true);
    }

    // ─── Getters: Nomad system ───
    public boolean isNomadSystemEnabled() { return nomadSystemEnabled; }
    public DayOfWeek getMigrateDay() { return migrateDay; }
    public int getMigrateHour() { return migrateHour; }
    public int getHouseRadius() { return houseRadius; }
    public boolean isSafeLandCheck() { return safeLandCheck; }

    // ─── Getters: Daily buffs ───
    public boolean isDailyBuffsEnabled() { return dailyBuffsEnabled; }
    public boolean isBroadcastOnJoin() { return broadcastOnJoin; }
    public String getBroadcastColor() { return broadcastColor; }
    public int getRandomCount() { return randomCount; }

    public DayConfig getDayConfig(DayOfWeek day) {
        int idx = day.getValue(); // MON=1 .. SUN=7
        return dayConfigs[idx] != null ? dayConfigs[idx] : new DayConfig(BuffMode.OFF, List.of(), List.of());
    }

    // Legacy helper
    public List<Integer> getWeekendPool() {
        DayConfig sat = getDayConfig(DayOfWeek.SATURDAY);
        return sat.randomPool.isEmpty() ? sat.fixedBuffs : sat.randomPool;
    }

    // ─── Getters: Progression lock ───
    public boolean isProgressionLockEnabled() { return progressionLockEnabled; }
    public boolean isEndLocked() { return endLocked; }
    public int getEndUnlockDays() { return endUnlockDays; }
    public String getServerStartDate() { return serverStartDate; }
    public boolean isNetheriteCraftingBanned() { return netheriteCraftingBanned; }
    public boolean isNetheriteEquipPunish() { return netheriteEquipPunish; }
    public boolean isBlockGamemodeCommand() { return blockGamemodeCommand; }
    public boolean isBlockGiveCommand() { return blockGiveCommand; }

    // ─── Getters: Anti-cheat ───
    public boolean isAntiCheatEnabled() { return antiCheatEnabled; }
    public boolean isBlockSeedCommand() { return blockSeedCommand; }
    public boolean isScrambleStructureSeeds() { return scrambleStructureSeeds; }
    public boolean isOreObfuscation() { return oreObfuscation; }

    // ─── Getters: Social ───
    public boolean isSocialEnabled() { return socialEnabled; }
    public boolean isWorldBorderEnabled() { return worldBorderEnabled; }
    public int getWorldBorderSize() { return worldBorderSize; }
    public int getWorldBorderCenterX() { return worldBorderCenterX; }
    public int getWorldBorderCenterZ() { return worldBorderCenterZ; }
    public double getWorldBorderDamageAmount() { return worldBorderDamageAmount; }
    public int getWorldBorderWarningDistance() { return worldBorderWarningDistance; }
    public boolean isDisableTpa() { return disableTpa; }
    public boolean isDisableWarp() { return disableWarp; }
    public boolean isDisableHomeCommand() { return disableHomeCommand; }
    public boolean isPlayerHeadDrop() { return playerHeadDrop; }
    public boolean isNoAdminOp() { return noAdminOp; }

    // Legacy compat
    public int getBorderHalfSize() { return worldBorderSize / 2; }
}
