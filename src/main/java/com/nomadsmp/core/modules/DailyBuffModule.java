package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DailyBuffModule {

    private final NomadCore plugin;
    private List<Integer> currentBuffIds = new ArrayList<>();
    private int taskId = -1;
    private int durationTaskId = -1;
    private LocalDateTime appliedAt;

    public static final String[] BUFF_NAMES = {
        "", "Titanium", "Power Miner", "Roadrunner", "Featherweight", "Iron Lung",
        "Pyro", "Night Owl", "Looter", "Bountiful Harvest", "Lucky Fisher",
        "Magnet", "Chef", "Blacksmith", "Timber", "Vein Miner",
        "Trophy Hunter", "Dolphin", "Gravity", "Vampire", "XP Junkie",
        "Merchant", "Tank", "Glass Cannon", "Archer", "Librarian",
        "Ninja", "Spider", "Ender", "Healthy", "Medic",
        "Sonic", "Rich", "Scavenger", "Glowstick", "Gardener",
        "Snowman", "Friendly", "Slimy", "Thor", "Teleporter",
        "Parachute", "Unstoppable", "Warrior", "Inertia", "Gravity Well",
        "Alchemist", "Builder", "Double Jump", "Whale", "Pacifist"
    };

    public static final String[] BUFF_DESCS = {
        "", "No tool durability loss", "Haste I", "Speed boost", "Slow Falling",
        "Water Breathing", "Fire Resistance", "Night Vision", "2x mob drops",
        "Double crop growth", "Instant fishing", "Auto-pickup items", "No hunger",
        "Auto-smelt ores", "Tree feller", "Vein miner",
        "Mob head drops", "Swim speed + Dolphin Grace", "Jump Boost II",
        "Heal on kill", "2x XP", "50% villager discounts", "Resistance I",
        "2x damage dealt & taken", "Free arrows", "1-level enchanting",
        "Invisibility + silent", "Wall climb when sneaking", "No pearl damage",
        "+5 extra hearts", "Regen II", "Speed III", "Gold nugget drops",
        "Bonus grass loot", "Glowing effect", "3x3 bone meal",
        "Leave snow trail + fire immune", "Peaceful mob attraction",
        "Bouncy fall + launch", "5% lightning on hit", "5-block sneak teleport",
        "Slow Falling", "Wither/Slow/Blind immune", "Strength I",
        "Knockback immune", "Pull mobs toward you", "3x potion duration",
        "20% block refund", "Double jump in survival", "Infinite oxygen",
        "Swords deal 0 damage + Regen IV"
    };

    public DailyBuffModule(NomadCore plugin) { this.plugin = plugin; }

    public void enable() {
        // Try to restore persisted buff state first
        List<Integer> persisted = plugin.getBuffStateStorage().load();
        if (!persisted.isEmpty()) {
            currentBuffIds = persisted;
            appliedAt = plugin.getBuffStateStorage().getAppliedAt();
            plugin.getLogger().info("Restored persisted buffs: " + currentBuffIds);
            // Re-apply to online players (in case of reload)
            Bukkit.getOnlinePlayers().forEach(p -> BuffApplier.apply(p, currentBuffIds, plugin));
            plugin.getStatsManager().recordBuffActivation();
        } else {
            updateDailyBuff();
        }

        // Rollover check
        long checkTicks = plugin.getConfigManager().getRolloverCheckSeconds() * 20L;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var now = java.time.LocalTime.now();
            if (now.getHour() == 0 && now.getMinute() == 0) updateDailyBuff();
        }, checkTicks, checkTicks).getTaskId();

        // Duration timeout check
        scheduleDurationExpiry();
    }

    public void disable() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        if (durationTaskId != -1) Bukkit.getScheduler().cancelTask(durationTaskId);
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
    }

    private void scheduleDurationExpiry() {
        if (durationTaskId != -1) Bukkit.getScheduler().cancelTask(durationTaskId);
        int durationHours = plugin.getConfigManager().getDurationHours();
        if (durationHours <= 0 || appliedAt == null) return; // 0 = all day

        long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), appliedAt.plusHours(durationHours));
        if (hoursRemaining <= 0) {
            // Already expired
            expireBuffs();
            return;
        }

        long ticksRemaining = hoursRemaining * 60L * 60L * 20L;
        durationTaskId = Bukkit.getScheduler().runTaskLater(plugin, this::expireBuffs, ticksRemaining).getTaskId();
        plugin.getLogger().info("Buffs will expire in " + hoursRemaining + " hours.");
    }

    private void expireBuffs() {
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
        currentBuffIds.clear();
        appliedAt = null;
        plugin.getBuffStateStorage().save(currentBuffIds);
        Bukkit.broadcastMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eToday's buffs have expired.");
        plugin.getLogger().info("Buffs expired (duration-hours reached).");
    }

    public void updateDailyBuff() {
        LocalDate now = LocalDate.now();
        DayOfWeek day = now.getDayOfWeek();
        var config = plugin.getConfigManager();
        ConfigManager.DayConfig dayConfig = config.getDayConfig(day);

        List<Integer> newIds = new ArrayList<>();
        switch (dayConfig.mode) {
            case FIXED -> newIds.addAll(dayConfig.fixedBuffs);
            case RANDOM -> {
                List<Integer> pool = dayConfig.randomPool;
                if (pool.isEmpty()) {
                    plugin.getLogger().warning("Day " + day + " is random but pool is empty!");
                } else {
                    long seed = now.toEpochDay();
                    Random rng = new Random(seed);
                    int count = Math.min(config.getRandomCount(), pool.size());
                    List<Integer> shuffled = new ArrayList<>(pool);
                    for (int i = shuffled.size() - 1; i > 0; i--) {
                        int j = rng.nextInt(i + 1);
                        int tmp = shuffled.get(i); shuffled.set(i, shuffled.get(j)); shuffled.set(j, tmp);
                    }
                    newIds.addAll(shuffled.subList(0, count));
                }
            }
            case OFF -> {}
        }

        // Remove old effects, apply new ones
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
        currentBuffIds = newIds;
        appliedAt = LocalDateTime.now();
        Bukkit.getOnlinePlayers().forEach(p -> BuffApplier.apply(p, currentBuffIds, plugin));

        // Persist
        plugin.getBuffStateStorage().save(currentBuffIds);
        plugin.getStatsManager().recordBuffActivation();
        plugin.getLogger().info("Daily buffs updated (" + day + ", mode=" + dayConfig.mode + "): " + currentBuffIds);

        // Schedule duration expiry
        scheduleDurationExpiry();

        // Broadcast Buff of the Day change to ALL players
        if (!currentBuffIds.isEmpty() && config.isBroadcastOnJoin()) {
            StringBuilder msg = new StringBuilder(config.getBroadcastColor());
            for (int id : currentBuffIds) {
                if (msg.length() > 2) msg.append(", ");
                msg.append(getBuffName(id));
            }
            plugin.broadcastAll("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eToday's buff: " + msg);
        }
    }

    /** Override buff via command — broadcasts to ALL players since it's a buff change. */
    public void setCurrentBuffIds(List<Integer> ids) {
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
        this.currentBuffIds = new ArrayList<>(ids);
        this.appliedAt = LocalDateTime.now();
        Bukkit.getOnlinePlayers().forEach(p -> BuffApplier.apply(p, currentBuffIds, plugin));
        plugin.getBuffStateStorage().save(currentBuffIds);
        scheduleDurationExpiry();

        // Broadcast to ALL players
        if (!ids.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (int id : ids) {
                if (!msg.isEmpty()) msg.append(", ");
                msg.append(getBuffName(id));
            }
            plugin.broadcastAll("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eBuff of the Day changed to: " + msg);
        } else {
            plugin.broadcastAll("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eBuff of the Day has been cleared.");
        }
    }

    public void applyBuffs(Player player, List<Integer> ids) { BuffApplier.apply(player, ids, plugin); }
    public void removeAllBuffEffects(Player player) { BuffApplier.removeAll(player); }
    public void applyToPlayer(Player player) { if (!currentBuffIds.isEmpty()) BuffApplier.apply(player, currentBuffIds, plugin); }
    public boolean isBuffActive(int id) { return currentBuffIds.contains(id); }
    public List<Integer> getCurrentBuffIds() { return List.copyOf(currentBuffIds); }
    public String getBuffName(int id) { return (id >= 1 && id < BUFF_NAMES.length) ? BUFF_NAMES[id] : "Unknown"; }
    public String getBuffDescription(int id) { return (id >= 1 && id < BUFF_DESCS.length) ? BUFF_DESCS[id] : ""; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}