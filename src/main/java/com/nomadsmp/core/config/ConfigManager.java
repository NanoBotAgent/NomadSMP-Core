package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final NomadCore plugin;
    private FileConfiguration config;
    private String bs = "daily-buffs.buff-settings."; // prefix shorthand

    // Nomad system
    private boolean nomadSystemEnabled;
    private DayOfWeek migrateDay;
    private int migrateHour;
    private int houseRadius;
    private boolean safeLandCheck;
    private int minSafeY;
    private int migrationCheckMinutes;
    private int migrationWarningMinutes;

    // Daily buffs
    private boolean dailyBuffsEnabled;
    private boolean broadcastOnJoin;
    private String broadcastColor;
    private int randomCount;
    private int rolloverCheckSeconds;
    private DayConfig[] dayConfigs;

    // Progression lock
    private boolean progressionLockEnabled;
    private boolean endLocked;
    private int endUnlockDays;
    private String serverStartDate;
    private boolean netheriteCraftingBanned;
    private boolean netheriteEquipPunish;
    private int netheritePunishDurationTicks;
    private int netheritePunishAmplifier;
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

    public enum BuffMode { FIXED, RANDOM, OFF }

    public static class DayConfig {
        public final BuffMode mode;
        public final List<Integer> fixedBuffs;
        public final List<Integer> randomPool;
        public DayConfig(BuffMode mode, List<Integer> fixedBuffs, List<Integer> randomPool) {
            this.mode = mode; this.fixedBuffs = fixedBuffs; this.randomPool = randomPool;
        }
    }

    public ConfigManager(NomadCore plugin) { this.plugin = plugin; }

    public void load() { config = plugin.getConfig(); loadValues(); }

    private void loadValues() {
        // Nomad system
        nomadSystemEnabled = config.getBoolean("nomad-system.enabled", true);
        migrateDay = DayOfWeek.valueOf(config.getString("nomad-system.migrate-day", "MONDAY").toUpperCase());
        migrateHour = config.getInt("nomad-system.migrate-hour", 0);
        houseRadius = config.getInt("nomad-system.house-radius", 16);
        safeLandCheck = config.getBoolean("nomad-system.safe-land-check", true);
        minSafeY = config.getInt("nomad-system.min-safe-y", 60);
        migrationCheckMinutes = config.getInt("nomad-system.migration-check-minutes", 5);
        migrationWarningMinutes = config.getInt("nomad-system.migration-warning-minutes", 5);

        // Daily buffs
        dailyBuffsEnabled = config.getBoolean("daily-buffs.enabled", true);
        broadcastOnJoin = config.getBoolean("daily-buffs.broadcast-on-join", true);
        broadcastColor = config.getString("daily-buffs.broadcast-color", "\u00a76");
        randomCount = config.getInt("daily-buffs.random-count", 1);
        rolloverCheckSeconds = config.getInt("daily-buffs.rollover-check-seconds", 60);

        dayConfigs = new DayConfig[8];
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
                dayConfigs[i] = new DayConfig(mode, sec.getIntegerList("buffs"), sec.getIntegerList("pool"));
            } else {
                List<Integer> legacy = config.getIntegerList(dayKey);
                dayConfigs[i] = new DayConfig(legacy.isEmpty() ? BuffMode.OFF : BuffMode.FIXED, legacy, List.of());
            }
        }

        // Progression lock
        progressionLockEnabled = config.getBoolean("progression-lock.enabled", true);
        endLocked = config.getBoolean("progression-lock.end-locked", true);
        endUnlockDays = config.getInt("progression-lock.end-unlock-days", 30);
        serverStartDate = config.getString("progression-lock.server-start-date", "2025-01-01");
        netheriteCraftingBanned = config.getBoolean("progression-lock.netherite-crafting-banned", true);
        netheriteEquipPunish = config.getBoolean("progression-lock.netherite-equip-punish", true);
        netheritePunishDurationTicks = config.getInt("progression-lock.netherite-punish-duration-ticks", 100);
        netheritePunishAmplifier = config.getInt("progression-lock.netherite-punish-amplifier", 4);
        blockGamemodeCommand = config.getBoolean("progression-lock.block-gamemode-command", true);
        blockGiveCommand = config.getBoolean("progression-lock.block-give-command", true);

        // Anti-cheat
        antiCheatEnabled = config.getBoolean("anti-cheat.enabled", true);
        blockSeedCommand = config.getBoolean("anti-cheat.block-seed-command", true);
        scrambleStructureSeeds = config.getBoolean("anti-cheat.scramble-structure-seeds", true);
        oreObfuscation = config.getBoolean("anti-cheat.ore-obfuscation", true);

        // Social
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
            worldBorderEnabled = true; worldBorderSize = 500;
            worldBorderCenterX = 0; worldBorderCenterZ = 0;
            worldBorderDamageAmount = 0.5; worldBorderWarningDistance = 10;
        }
        disableTpa = config.getBoolean("social.disable-tpa", true);
        disableWarp = config.getBoolean("social.disable-warp", true);
        disableHomeCommand = config.getBoolean("social.disable-home-command", true);
        playerHeadDrop = config.getBoolean("social.player-head-drop", true);
        noAdminOp = config.getBoolean("social.no-admin-op", true);
    }

    // ─── Generic buff-setting getters ───
    public int getBuffInt(String key, int def) { return config.getInt(bs + key, def); }
    public double getBuffDouble(String key, double def) { return config.getDouble(bs + key, def); }
    public boolean getBuffBool(String key, boolean def) { return config.getBoolean(bs + key, def); }
    public String getBuffString(String key, String def) { return config.getString(bs + key, def); }

    // ─── Convenience getters for every buff setting ───
    public int getPowerMinerAmplifier() { return getBuffInt("power-miner-haste-amplifier", 0); }
    public double getRoadrunnerSpeed() { return getBuffDouble("roadrunner-speed-modifier", 0.12); }
    public double getDolphinSpeed() { return getBuffDouble("dolphin-speed-modifier", 0.12); }
    public double getSonicSpeed() { return getBuffDouble("sonic-speed-modifier", 0.4); }
    public double getHealthyHearts() { return getBuffDouble("healthy-extra-hearts", 10.0); }
    public int getLooterMultiplier() { return getBuffInt("looter-drop-multiplier", 2); }
    public int getTimberMax() { return getBuffInt("timber-max-blocks", 50); }
    public int getVeinMinerMax() { return getBuffInt("vein-miner-max-blocks", 32); }
    public int getMagnetRange() { return getBuffInt("magnet-range", 5); }
    public double getMagnetStrength() { return getBuffDouble("magnet-strength", 0.5); }
    public double getVampireHeal() { return getBuffDouble("vampire-heal-amount", 1.0); }
    public int getXpMultiplier() { return getBuffInt("xp-junkie-multiplier", 2); }
    public double getGlassCannonDealt() { return getBuffDouble("glass-cannon-damage-multiplier", 2.0); }
    public double getGlassCannonTaken() { return getBuffDouble("glass-cannon-damage-taken-multiplier", 2.0); }
    public int getLibrarianCost() { return getBuffInt("librarian-enchant-cost", 1); }
    public double getSpiderClimbVelocity() { return getBuffDouble("spider-climb-velocity", 0.3); }
    public long getEnderPearlWindowMs() { return getBuffInt("ender-pearl-window-ms", 1000); }
    public double getRichNuggetChance() { return getBuffDouble("rich-nugget-chance", 0.1); }
    public double getScavengerChance() { return getBuffDouble("scavenger-chance", 0.05); }
    public List<Material> getScavengerLoot() {
        return config.getStringList(bs + "scavenger-loot").stream()
            .map(s -> { try { return Material.valueOf(s); } catch (Exception e) { return null; } })
            .filter(m -> m != null).collect(Collectors.toList());
    }
    public int getGardenerRadius() { return getBuffInt("gardener-radius", 1); }
    public double getThorChance() { return getBuffDouble("thor-lightning-chance", 0.05); }
    public int getTeleporterDistance() { return getBuffInt("teleporter-distance", 5); }
    public long getTeleporterCooldownMs() { return getBuffInt("teleporter-cooldown-ms", 10000); }
    public double getInertiaThreshold() { return getBuffDouble("inertia-velocity-threshold", 0.3); }
    public int getGravityWellRange() { return getBuffInt("gravity-well-range", 5); }
    public double getGravityWellStrength() { return getBuffDouble("gravity-well-strength", 0.3); }
    public int getAlchemistMultiplier() { return getBuffInt("alchemist-duration-multiplier", 3); }
    public double getBuilderRefundChance() { return getBuffDouble("builder-refund-chance", 0.2); }
    public double getDoubleJumpVelocity() { return getBuffDouble("double-jump-velocity", 0.8); }
    public double getSlimyBounceMultiplier() { return getBuffDouble("slimy-bounce-multiplier", 0.05); }

    // ─── Nomad system getters ───
    public boolean isNomadSystemEnabled() { return nomadSystemEnabled; }
    public DayOfWeek getMigrateDay() { return migrateDay; }
    public int getMigrateHour() { return migrateHour; }
    public int getHouseRadius() { return houseRadius; }
    public boolean isSafeLandCheck() { return safeLandCheck; }
    public int getMinSafeY() { return minSafeY; }
    public int getMigrationCheckMinutes() { return migrationCheckMinutes; }
    public int getMigrationWarningMinutes() { return migrationWarningMinutes; }

    // ─── Daily buff getters ───
    public boolean isDailyBuffsEnabled() { return dailyBuffsEnabled; }
    public boolean isBroadcastOnJoin() { return broadcastOnJoin; }
    public String getBroadcastColor() { return broadcastColor; }
    public int getRandomCount() { return randomCount; }
    public int getRolloverCheckSeconds() { return rolloverCheckSeconds; }
    public DayConfig getDayConfig(DayOfWeek day) {
        int idx = day.getValue();
        return dayConfigs[idx] != null ? dayConfigs[idx] : new DayConfig(BuffMode.OFF, List.of(), List.of());
    }
    public List<Integer> getWeekendPool() {
        DayConfig sat = getDayConfig(DayOfWeek.SATURDAY);
        return sat.randomPool.isEmpty() ? sat.fixedBuffs : sat.randomPool;
    }

    // ─── Progression lock getters ───
    public boolean isProgressionLockEnabled() { return progressionLockEnabled; }
    public boolean isEndLocked() { return endLocked; }
    public int getEndUnlockDays() { return endUnlockDays; }
    public String getServerStartDate() { return serverStartDate; }
    public boolean isNetheriteCraftingBanned() { return netheriteCraftingBanned; }
    public boolean isNetheriteEquipPunish() { return netheriteEquipPunish; }
    public int getNetheritePunishDurationTicks() { return netheritePunishDurationTicks; }
    public int getNetheritePunishAmplifier() { return netheritePunishAmplifier; }
    public boolean isBlockGamemodeCommand() { return blockGamemodeCommand; }
    public boolean isBlockGiveCommand() { return blockGiveCommand; }

    // ─── Anti-cheat getters ───
    public boolean isAntiCheatEnabled() { return antiCheatEnabled; }
    public boolean isBlockSeedCommand() { return blockSeedCommand; }
    public boolean isScrambleStructureSeeds() { return scrambleStructureSeeds; }
    public boolean isOreObfuscation() { return oreObfuscation; }

    // ─── Social getters ───
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
    public int getBorderHalfSize() { return worldBorderSize / 2; }
}
