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
    private static final String BUFFS = "daily-buffs.buffs.";

    // ─── Nomad system ───
    private boolean nomadSystemEnabled;
    private DayOfWeek migrateDay;
    private int migrateHour;
    private int houseRadius;
    private boolean safeLandCheck;
    private int minSafeY;
    private int migrationCheckMinutes;
    private int migrationWarningMinutes;

    // ─── Daily buffs ───
    private boolean dailyBuffsEnabled;
    private boolean broadcastOnJoin;
    private String broadcastColor;
    private int randomCount;
    private int rolloverCheckSeconds;
    private int durationHours;
    private BuffStacking stacking;
    private DayConfig[] dayConfigs;

    public enum BuffStacking { HIGHEST, ADDITIVE, REPLACE }

    // ─── Progression lock ───
    private boolean progressionLockEnabled;
    private boolean endLockEnabled;
    private int endUnlockDays;
    private String serverStartDate;
    private boolean netheriteCraftingBanEnabled;
    private boolean netheriteEquipPunishEnabled;
    private int netheritePunishDurationTicks;
    private int netheritePunishAmplifier;
    private boolean blockGamemodeCommandEnabled;
    private boolean blockGiveCommandEnabled;

    // ─── Anti-cheat ───
    private boolean antiCheatEnabled;
    private boolean blockSeedCommandEnabled;
    private boolean scrambleStructureSeedsEnabled;
    private boolean oreObfuscationEnabled;

    // ─── Social ───
    private boolean socialEnabled;
    private boolean worldBorderEnabled;
    private int worldBorderSize;
    private int worldBorderCenterX;
    private int worldBorderCenterZ;
    private double worldBorderDamageAmount;
    private int worldBorderWarningDistance;
    private boolean disableTpaEnabled;
    private boolean disableWarpEnabled;
    private boolean disableHomeCommandEnabled;
    private boolean playerHeadDropEnabled;
    private boolean noAdminOpEnabled;

    // ─── Per-buff section keys ───
    private static final String[] BUFF_KEYS = {
        "", "1-titanium", "2-power-miner", "3-roadrunner", "4-featherweight",
        "5-iron-lung", "6-pyro", "7-night-owl", "8-looter", "9-bountiful-harvest",
        "10-lucky-fisher", "11-magnet", "12-chef", "13-blacksmith", "14-timber",
        "15-vein-miner", "16-trophy-hunter", "17-dolphin", "18-gravity", "19-vampire",
        "20-xp-junkie", "21-merchant", "22-tank", "23-glass-cannon", "24-archer",
        "25-librarian", "26-ninja", "27-spider", "28-ender", "29-healthy",
        "30-medic", "31-sonic", "32-rich", "33-scavenger", "34-glowstick",
        "35-gardener", "36-snowman", "37-friendly", "38-slimy", "39-thor",
        "40-teleporter", "41-parachute", "42-unstoppable", "43-warrior", "44-inertia",
        "45-gravity-well", "46-alchemist", "47-builder", "48-double-jump", "49-whale",
        "50-pacifist", "51-leaf-cut"
    };

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

    // ─── Per-buff config helpers ───
    private String bkey(int id) { return (id >= 1 && id < BUFF_KEYS.length) ? BUFFS + BUFF_KEYS[id] + "." : BUFFS + id + "-unknown."; }
    public int buffInt(int id, String key, int def) { return config.getInt(bkey(id) + key, def); }
    public double buffDouble(int id, String key, double def) { return config.getDouble(bkey(id) + key, def); }
    public boolean buffBool(int id, String key, boolean def) { return config.getBoolean(bkey(id) + key, def); }
    public String buffString(int id, String key, String def) { return config.getString(bkey(id) + key, def); }
    public boolean isBuffEnabled(int id) { return buffBool(id, "enabled", true); }

    // ─── Convenience getters per buff ───
    public int getPowerMinerAmplifier() { return buffInt(2, "haste-amplifier", 0); }
    public double getRoadrunnerSpeed() { return buffDouble(3, "speed-modifier", 0.12); }
    public double getDolphinSpeed() { return buffDouble(17, "speed-modifier", 0.12); }
    public double getSonicSpeed() { return buffDouble(31, "speed-modifier", 0.4); }
    public double getHealthyHearts() { return buffDouble(29, "extra-hearts", 10.0); }
    public int getLooterMultiplier() { return buffInt(8, "drop-multiplier", 2); }
    public int getTimberMax() { return buffInt(14, "max-blocks", 50); }
    public int getVeinMinerMax() { return buffInt(15, "max-blocks", 32); }
    public int getMagnetRange() { return buffInt(11, "range", 5); }
    public double getMagnetStrength() { return buffDouble(11, "pull-strength", 0.5); }
    public double getVampireHeal() { return buffDouble(19, "heal-per-kill", 1.0); }
    public int getXpMultiplier() { return buffInt(20, "xp-multiplier", 2); }
    public double getGlassCannonDealt() { return buffDouble(23, "damage-dealt-multiplier", 2.0); }
    public double getGlassCannonTaken() { return buffDouble(23, "damage-taken-multiplier", 2.0); }
    public int getLibrarianCost() { return buffInt(25, "enchant-cost", 1); }
    public double getSpiderClimbVelocity() { return buffDouble(27, "climb-velocity", 0.3); }
    public long getEnderPearlWindowMs() { return buffInt(28, "pearl-fall-window-ms", 1000); }
    public double getRichNuggetChance() { return buffDouble(32, "nugget-chance", 0.1); }
    public double getScavengerChance() { return buffDouble(33, "drop-chance", 0.05); }
    public List<Material> getScavengerLoot() {
        return config.getStringList(bkey(33) + "loot").stream()
            .map(s -> { try { return Material.valueOf(s); } catch (Exception e) { return null; } })
            .filter(m -> m != null).collect(Collectors.toList());
    }
    public int getGardenerRadius() { return buffInt(35, "bone-meal-radius", 1); }
    public double getThorChance() { return buffDouble(39, "lightning-chance", 0.05); }
    public int getTeleporterDistance() { return buffInt(40, "distance", 5); }
    public long getTeleporterCooldownMs() { return buffInt(40, "cooldown-ms", 10000); }
    public double getInertiaThreshold() { return buffDouble(44, "velocity-threshold", 0.3); }
    public int getGravityWellRange() { return buffInt(45, "range", 5); }
    public double getGravityWellStrength() { return buffDouble(45, "pull-strength", 0.3); }
    public int getAlchemistMultiplier() { return buffInt(46, "duration-multiplier", 3); }
    public double getBuilderRefundChance() { return buffDouble(47, "refund-chance", 0.2); }
    public double getDoubleJumpVelocity() { return buffDouble(48, "velocity", 0.8); }
    public double getSlimyBounceMultiplier() { return buffDouble(38, "bounce-multiplier", 0.05); }
    public int getLeafCutMaxLeaves() { return buffInt(51, "max-leaves", 64); }

    // ─── Load all values ───
    private void loadValues() {
        nomadSystemEnabled = config.getBoolean("nomad-system.enabled", true);
        migrateDay = DayOfWeek.valueOf(config.getString("nomad-system.migrate-day", "MONDAY").toUpperCase());
        migrateHour = config.getInt("nomad-system.migrate-hour", 0);
        houseRadius = config.getInt("nomad-system.house-radius", 16);
        safeLandCheck = config.getBoolean("nomad-system.safe-land-check", true);
        minSafeY = config.getInt("nomad-system.min-safe-y", 60);
        migrationCheckMinutes = config.getInt("nomad-system.migration-check-minutes", 5);
        migrationWarningMinutes = config.getInt("nomad-system.migration-warning-minutes", 5);

        dailyBuffsEnabled = config.getBoolean("daily-buffs.enabled", true);
        broadcastOnJoin = config.getBoolean("daily-buffs.broadcast-on-join", true);
        broadcastColor = config.getString("daily-buffs.broadcast-color", "\u00a76");
        randomCount = config.getInt("daily-buffs.random-count", 1);
        rolloverCheckSeconds = config.getInt("daily-buffs.rollover-check-seconds", 60);
        durationHours = config.getInt("daily-buffs.duration-hours", 0);
        stacking = switch (config.getString("daily-buffs.stacking", "additive").toLowerCase()) {
            case "highest" -> BuffStacking.HIGHEST;
            case "replace" -> BuffStacking.REPLACE;
            default -> BuffStacking.ADDITIVE;
        };

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

        progressionLockEnabled = config.getBoolean("progression-lock.enabled", true);
        endLockEnabled = config.getBoolean("progression-lock.end-lock.enabled", true);
        endUnlockDays = config.getInt("progression-lock.end-lock.unlock-days", 30);
        serverStartDate = config.getString("progression-lock.end-lock.server-start-date", "2025-01-01");
        netheriteCraftingBanEnabled = config.getBoolean("progression-lock.netherite-crafting-ban.enabled", true);
        netheriteEquipPunishEnabled = config.getBoolean("progression-lock.netherite-equip-punish.enabled", true);
        netheritePunishDurationTicks = config.getInt("progression-lock.netherite-equip-punish.duration-ticks", 100);
        netheritePunishAmplifier = config.getInt("progression-lock.netherite-equip-punish.amplifier", 4);
        blockGamemodeCommandEnabled = config.getBoolean("progression-lock.block-gamemode-command.enabled", true);
        blockGiveCommandEnabled = config.getBoolean("progression-lock.block-give-command.enabled", true);

        antiCheatEnabled = config.getBoolean("anti-cheat.enabled", true);
        blockSeedCommandEnabled = config.getBoolean("anti-cheat.block-seed-command.enabled", true);
        scrambleStructureSeedsEnabled = config.getBoolean("anti-cheat.scramble-structure-seeds.enabled", true);
        oreObfuscationEnabled = config.getBoolean("anti-cheat.ore-obfuscation.enabled", true);

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
        disableTpaEnabled = config.getBoolean("social.disable-tpa.enabled", true);
        disableWarpEnabled = config.getBoolean("social.disable-warp.enabled", true);
        disableHomeCommandEnabled = config.getBoolean("social.disable-home-command.enabled", true);
        playerHeadDropEnabled = config.getBoolean("social.player-head-drop.enabled", true);
        noAdminOpEnabled = config.getBoolean("social.no-admin-op.enabled", true);
    }

    // ─── Getters: Nomad system ───
    public boolean isNomadSystemEnabled() { return nomadSystemEnabled; }
    public DayOfWeek getMigrateDay() { return migrateDay; }
    public int getMigrateHour() { return migrateHour; }
    public int getHouseRadius() { return houseRadius; }
    public boolean isSafeLandCheck() { return safeLandCheck; }
    public int getMinSafeY() { return minSafeY; }
    public int getMigrationCheckMinutes() { return migrationCheckMinutes; }
    public int getMigrationWarningMinutes() { return migrationWarningMinutes; }

    // ─── Getters: Daily buffs ───
    public boolean isDailyBuffsEnabled() { return dailyBuffsEnabled; }
    public boolean isBroadcastOnJoin() { return broadcastOnJoin; }
    public String getBroadcastColor() { return broadcastColor; }
    public int getRandomCount() { return randomCount; }
    public int getRolloverCheckSeconds() { return rolloverCheckSeconds; }
    public int getDurationHours() { return durationHours; }
    public BuffStacking getStacking() { return stacking; }
    public DayConfig getDayConfig(DayOfWeek day) {
        int idx = day.getValue();
        return dayConfigs[idx] != null ? dayConfigs[idx] : new DayConfig(BuffMode.OFF, List.of(), List.of());
    }

    // ─── Getters: Progression lock ───
    public boolean isProgressionLockEnabled() { return progressionLockEnabled; }
    public boolean isEndLockEnabled() { return endLockEnabled; }
    public int getEndUnlockDays() { return endUnlockDays; }
    public String getServerStartDate() { return serverStartDate; }
    public boolean isNetheriteCraftingBanEnabled() { return netheriteCraftingBanEnabled; }
    public boolean isNetheriteEquipPunishEnabled() { return netheriteEquipPunishEnabled; }
    public int getNetheritePunishDurationTicks() { return netheritePunishDurationTicks; }
    public int getNetheritePunishAmplifier() { return netheritePunishAmplifier; }
    public boolean isBlockGamemodeCommandEnabled() { return blockGamemodeCommandEnabled; }
    public boolean isBlockGiveCommandEnabled() { return blockGiveCommandEnabled; }
    public boolean isEndLocked() { return endLockEnabled; }
    public boolean isNetheriteCraftingBanned() { return netheriteCraftingBanEnabled; }
    public boolean isNetheriteEquipPunish() { return netheriteEquipPunishEnabled; }
    public boolean isBlockGamemodeCommand() { return blockGamemodeCommandEnabled; }
    public boolean isBlockGiveCommand() { return blockGiveCommandEnabled; }

    // ─── Getters: Anti-cheat ───
    public boolean isAntiCheatEnabled() { return antiCheatEnabled; }
    public boolean isBlockSeedCommandEnabled() { return blockSeedCommandEnabled; }
    public boolean isScrambleStructureSeedsEnabled() { return scrambleStructureSeedsEnabled; }
    public boolean isOreObfuscationEnabled() { return oreObfuscationEnabled; }
    public boolean isBlockSeedCommand() { return blockSeedCommandEnabled; }
    public boolean isScrambleStructureSeeds() { return scrambleStructureSeedsEnabled; }
    public boolean isOreObfuscation() { return oreObfuscationEnabled; }

    // ─── Getters: Social ───
    public boolean isSocialEnabled() { return socialEnabled; }
    public boolean isWorldBorderEnabled() { return worldBorderEnabled; }
    public int getWorldBorderSize() { return worldBorderSize; }
    public int getWorldBorderCenterX() { return worldBorderCenterX; }
    public int getWorldBorderCenterZ() { return worldBorderCenterZ; }
    public double getWorldBorderDamageAmount() { return worldBorderDamageAmount; }
    public int getWorldBorderWarningDistance() { return worldBorderWarningDistance; }
    public boolean isDisableTpaEnabled() { return disableTpaEnabled; }
    public boolean isDisableWarpEnabled() { return disableWarpEnabled; }
    public boolean isDisableHomeCommandEnabled() { return disableHomeCommandEnabled; }
    public boolean isPlayerHeadDropEnabled() { return playerHeadDropEnabled; }
    public boolean isNoAdminOpEnabled() { return noAdminOpEnabled; }
    public boolean isDisableTpa() { return disableTpaEnabled; }
    public boolean isDisableWarp() { return disableWarpEnabled; }
    public boolean isDisableHomeCommand() { return disableHomeCommandEnabled; }
    public boolean isPlayerHeadDrop() { return playerHeadDropEnabled; }
    public boolean isNoAdminOp() { return noAdminOpEnabled; }
    public int getBorderHalfSize() { return worldBorderSize / 2; }
}
