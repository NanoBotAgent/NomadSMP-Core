package com.nomadsmp.core.config;

import com.nomadsmp.core.NomadCore;
import org.bukkit.configuration.file.FileConfiguration;
import java.time.DayOfWeek;
import java.util.List;

public class ConfigManager {
    private final NomadCore plugin;
    public ConfigManager(NomadCore plugin) { this.plugin = plugin; }
    private FileConfiguration c() { return plugin.getConfig(); }

    public boolean isNomadEnabled() { return c().getBoolean("nomad-system.enabled", true); }
    public DayOfWeek getMigrateDay() { return DayOfWeek.valueOf(c().getString("nomad-system.migrate-day", "MONDAY").toUpperCase()); }
    public int getMigrateHour() { return c().getInt("nomad-system.migrate-hour", 0); }
    public int getHouseRadius() { return c().getInt("nomad-system.house-radius", 16); }
    public int getBorderHalfSize() { return c().getInt("nomad-system.border-half-size", 250); }
    public boolean isSafeLandCheck() { return c().getBoolean("nomad-system.safe-land-check", true); }
    public boolean isDailyBuffsEnabled() { return c().getBoolean("daily-buffs.enabled", true); }
    public List<Integer> getBuffIdsForDay(DayOfWeek day) { return c().getIntegerList("daily-buffs." + day.name().toLowerCase()); }
    public List<Integer> getWeekendPool() { return c().getIntegerList("daily-buffs.weekend-pool"); }
    public boolean isBroadcastOnJoin() { return c().getBoolean("daily-buffs.broadcast-on-join", true); }
    public String getBroadcastColor() { return c().getString("daily-buffs.broadcast-color", "\u00a76"); }
    public boolean isProgressionLockEnabled() { return c().getBoolean("progression-lock.enabled", true); }
    public boolean isEndLocked() { return c().getBoolean("progression-lock.end-locked", true); }
    public int getEndUnlockDays() { return c().getInt("progression-lock.end-unlock-days", 30); }
    public boolean isNetheriteCraftingBanned() { return c().getBoolean("progression-lock.netherite-crafting-banned", true); }
    public boolean isNetheriteEquipPunish() { return c().getBoolean("progression-lock.netherite-equip-punish", true); }
    public boolean isBlockGamemodeCommand() { return c().getBoolean("progression-lock.block-gamemode-command", true); }
    public boolean isBlockGiveCommand() { return c().getBoolean("progression-lock.block-give-command", true); }
    public String getServerStartDate() { return c().getString("progression-lock.server-start-date", "2025-01-01"); }
    public boolean isAntiCheatEnabled() { return c().getBoolean("anti-cheat.enabled", true); }
    public boolean isBlockSeedCommand() { return c().getBoolean("anti-cheat.block-seed-command", true); }
    public boolean isScrambleStructureSeeds() { return c().getBoolean("anti-cheat.scramble-structure-seeds", true); }
    public boolean isOreObfuscation() { return c().getBoolean("anti-cheat.ore-obfuscation", true); }
    public boolean isSocialEnabled() { return c().getBoolean("social.enabled", true); }
    public boolean isWorldBorder() { return c().getBoolean("social.world-border", true); }
    public boolean isDisableTpa() { return c().getBoolean("social.disable-tpa", true); }
    public boolean isDisableWarp() { return c().getBoolean("social.disable-warp", true); }
    public boolean isDisableHomeCommand() { return c().getBoolean("social.disable-home-command", true); }
    public boolean isPlayerHeadDrop() { return c().getBoolean("social.player-head-drop", true); }
    public boolean isNoAdminOp() { return c().getBoolean("social.no-admin-op", true); }
    public void reload() { plugin.reloadConfig(); }
}
