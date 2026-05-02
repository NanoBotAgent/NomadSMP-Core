package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DailyBuffModule {

    private final NomadCore plugin;
    private List<Integer> currentBuffIds = new ArrayList<>();
    private int taskId = -1;

    // Buff names for display
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

    // Buff one-line descriptions
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

    public DailyBuffModule(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        updateDailyBuff();
        // Check every 60 seconds for midnight roll-over
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var now = java.time.LocalTime.now();
            if (now.getHour() == 0 && now.getMinute() == 0) {
                updateDailyBuff();
            }
        }, 1200L, 1200L).getTaskId();
    }

    public void disable() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        // Remove all buff effects
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
    }

    public void updateDailyBuff() {
        LocalDate now = LocalDate.now();
        DayOfWeek day = now.getDayOfWeek();
        var config = plugin.getConfigManager();

        List<Integer> newIds;
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            long seed = now.toEpochDay();
            Random rng = new Random(seed);
            List<Integer> pool = config.getWeekendPool();
            int chosen = pool.get(rng.nextInt(pool.size()));
            newIds = List.of(chosen);
        } else {
            newIds = config.getBuffIdsForDay(day);
        }

        // Remove old buffs, apply new ones
        Bukkit.getOnlinePlayers().forEach(this::removeAllBuffEffects);
        currentBuffIds = new ArrayList<>(newIds);
        Bukkit.getOnlinePlayers().forEach(p -> applyBuffs(p, currentBuffIds));

        plugin.getLogger().info("Daily buffs updated: " + currentBuffIds);
    }

    public void applyBuffs(Player player, List<Integer> ids) {
        BuffApplier.apply(player, ids, plugin);
    }

    public void removeAllBuffEffects(Player player) {
        BuffApplier.removeAll(player);
    }

    public void applyToPlayer(Player player) {
        if (!currentBuffIds.isEmpty()) {
            applyBuffs(player, currentBuffIds);
        }
    }

    public boolean isBuffActive(int id) { return currentBuffIds.contains(id); }
    public List<Integer> getCurrentBuffIds() { return List.copyOf(currentBuffIds); }
    public void setCurrentBuffIds(List<Integer> ids) { this.currentBuffIds = new ArrayList<>(ids); }

    public String getBuffName(int id) {
        return (id >= 1 && id < BUFF_NAMES.length) ? BUFF_NAMES[id] : "Unknown";
    }

    public String getBuffDescription(int id) {
        return (id >= 1 && id < BUFF_DESCS.length) ? BUFF_DESCS[id] : "";
    }
}
