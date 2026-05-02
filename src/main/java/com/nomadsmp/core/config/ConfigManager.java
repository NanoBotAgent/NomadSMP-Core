package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.List;

public class ConfigManager {

    private final NomadCore plugin;
    private FileConfiguration config;

    // Cached values
    private boolean nomadSystemEnabled;
    private DayOfWeek migrateDay;
    private int migrateHour;
    private int houseRadius;
    private int borderHalfSize;
    private boolean safeLandCheck;

    private boolean dailyBuffsEnabled;
    private List<Integer> mondayBuffs, tuesdayBuffs, wednesdayBuffs, thursdayBuffs, fridayBuffs;
    private List<Integer> weekendPool;
    private boolean broadcastOnJoin;
    private String broadcastColor;

    private boolean progressionLockEnabled;
    private boolean endLocked;
    private int endUnlockDays;
    private boolean netheriteCraftingBanned;
    private boolean netheriteEquipPunish;
    private boolean blockGamemodeCommand;
    private boolean blockGiveCommand;

    private boolean antiCheatEnabled;
    private boolean blockSeedCommand;
    private boolean scrambleStructureSeeds;
    private boolean oreObfuscation;

    private boolean socialEnabled;
    private boolean worldBorder;
    private boolean disableTpa;
    private boolean disableWarp;
    private boolean disableHomeCommand;
    private boolean playerHeadDrop;
    private boolean noAdminOp;

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
        borderHalfSize = config.getInt("nomad-system.border-half-size", 250);
        safeLandCheck = config.getBoolean("nomad-system.safe-land-check", true);

        // Daily buffs
        dailyBuffsEnabled = config.getBoolean("daily-buffs.enabled", true);
        mondayBuffs = config.getIntegerList("daily-buffs.monday");
        tuesdayBuffs = config.getIntegerList("daily-buffs.tuesday");
        wednesdayBuffs = config.getIntegerList("daily-buffs.wednesday");
        thursdayBuffs = config.getIntegerList("daily-buffs.thursday");
        fridayBuffs = config.getIntegerList("daily-buffs.friday");
        weekendPool = config.getIntegerList("daily-buffs.weekend-pool");
        broadcastOnJoin = config.getBoolean("daily-buffs.broadcast-on-join", true);
        broadcastColor = config.getString("daily-buffs.broadcast-color", "\u00a76");

        // Progression lock
        progressionLockEnabled = config.getBoolean("progression-lock.enabled", true);
        endLocked = config.getBoolean("progression-lock.end-locked", true);
        endUnlockDays = config.getInt("progression-lock.end-unlock-days", 30);
        netheriteCraftingBanned = config.getBoolean("progression-lock.netherite-crafting-banned", true);
        netheriteEquipPunish = config.getBoolean("progression-lock.netherite-equip-punish", true);
        blockGamemodeCommand = config.getBoolean("progression-lock.block-gamemode-command", true);
        blockGiveCommand = config.getBoolean("progression-lock.block-give-command", true);

        // Anti-cheat
        antiCheatEnabled = config.getBoolean("anti-cheat.enabled", true);
        blockSeedCommand = config.getBoolean("anti-cheat.block-seed-command", true);
        scrambleStructureSeeds = config.getBoolean("anti-cheat.scramble-structure-seeds", true);
        oreObfuscation = config.getBoolean("anti-cheat.ore-obfuscation", true);

        // Social
        socialEnabled = config.getBoolean("social.enabled", true);
        worldBorder = config.getBoolean("social.world-border", true);
        disableTpa = config.getBoolean("social.disable-tpa", true);
        disableWarp = config.getBoolean("social.disable-warp", true);
        disableHomeCommand = config.getBoolean("social.disable-home-command", true);
        playerHeadDrop = config.getBoolean("social.player-head-drop", true);
        noAdminOp = config.getBoolean("social.no-admin-op", true);
    }

    // Getters — nomad system
    public boolean isNomadSystemEnabled() { return nomadSystemEnabled; }
    public DayOfWeek getMigrateDay() { return migrateDay; }
    public int getMigrateHour() { return migrateHour; }
    public int getHouseRadius() { return houseRadius; }
    public int getBorderHalfSize() { return borderHalfSize; }
    public boolean isSafeLandCheck() { return safeLandCheck; }

    // Getters — daily buffs
    public boolean isDailyBuffsEnabled() { return dailyBuffsEnabled; }
    public List<Integer> getBuffIdsForDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> mondayBuffs;
            case TUESDAY -> tuesdayBuffs;
            case WEDNESDAY -> wednesdayBuffs;
            case THURSDAY -> thursdayBuffs;
            case FRIDAY -> fridayBuffs;
            default -> List.of();
        };
    }
    public List<Integer> getWeekendPool() { return weekendPool; }
    public boolean isBroadcastOnJoin() { return broadcastOnJoin; }
    public String getBroadcastColor() { return broadcastColor; }

    // Getters — progression lock
    public boolean isProgressionLockEnabled() { return progressionLockEnabled; }
    public boolean isEndLocked() { return endLocked; }
    public int getEndUnlockDays() { return endUnlockDays; }
    public boolean isNetheriteCraftingBanned() { return netheriteCraftingBanned; }
    public boolean isNetheriteEquipPunish() { return netheriteEquipPunish; }
    public boolean isBlockGamemodeCommand() { return blockGamemodeCommand; }
    public boolean isBlockGiveCommand() { return blockGiveCommand; }

    // Getters — anti-cheat
    public boolean isAntiCheatEnabled() { return antiCheatEnabled; }
    public boolean isBlockSeedCommand() { return blockSeedCommand; }
    public boolean isScrambleStructureSeeds() { return scrambleStructureSeeds; }
    public boolean isOreObfuscation() { return oreObfuscation; }

    // Getters — social
    public boolean isSocialEnabled() { return socialEnabled; }
    public boolean isWorldBorder() { return worldBorder; }
    public boolean isDisableTpa() { return disableTpa; }
    public boolean isDisableWarp() { return disableWarp; }
    public boolean isDisableHomeCommand() { return disableHomeCommand; }
    public boolean isPlayerHeadDrop() { return playerHeadDrop; }
    public boolean isNoAdminOp() { return noAdminOp; }
}
