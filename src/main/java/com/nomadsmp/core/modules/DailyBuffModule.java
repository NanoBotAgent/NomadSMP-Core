package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class DailyBuffModule {
    private final NomadCore plugin;
    private List<Integer> currentBuffIds = new ArrayList<>();
    private int taskId;

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

    public static final String[] BUFF_DESC = {
        "", "Tools take no durability damage", "Haste I", "Speed boost", "Slow Falling",
        "Water Breathing", "Fire Resistance", "Night Vision", "Double mob drops", "Crops grow faster",
        "Instant fishing", "Items fly toward you", "No hunger", "Auto-smelt ores", "Break whole trees",
        "Break connected ores", "Mobs drop their heads", "Swim fast + Dolphin's Grace", "Jump Boost II", "Heal on kill",
        "Double XP", "50% villager discount", "Resistance I", "2x damage dealt & taken", "Free arrows",
        "Enchant for 1 level", "Invisibility", "Wall climb while sneaking", "No pearl damage", "+5 extra hearts",
        "Regen II", "Speed III (fast!)", "Mobs drop gold nuggets", "Grass drops random loot", "Glowing effect",
        "Bone meal in 3x3 area", "Leave snow trail, immune to fire", "Animals follow you", "Bouncy fall damage",
        "5% lightning on hit", "Sneak to teleport 5 blocks", "Slow Falling", "Immune to wither/slowness/blindness",
        "Strength I", "No knockback", "Pull hostile mobs toward you", "3x potion duration", "20% block refund",
        "Double jump in survival", "Infinite air", "No sword damage, Regen IV"
    };

    public DailyBuffModule(NomadCore plugin) { this.plugin = plugin; }

    public void enable() {
        updateDailyBuff();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (LocalTime.now().getHour() == 0 && LocalTime.now().getMinute() == 0) updateDailyBuff();
        }, 1200L, 1200L).getTaskId();
    }

    public void disable() {
        Bukkit.getScheduler().cancelTask(taskId);
        for (Player p : Bukkit.getOnlinePlayers()) removeAllBuffEffects(p);
    }

    public void updateDailyBuff() {
        LocalDate now = LocalDate.now();
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            long seed = now.toEpochDay();
            Random rng = new Random(seed);
            List<Integer> pool = plugin.cfg().getWeekendPool();
            currentBuffIds = List.of(pool.get(rng.nextInt(pool.size())));
        } else {
            currentBuffIds = new ArrayList<>(plugin.cfg().getBuffIdsForDay(day));
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeAllBuffEffects(p);
            applyBuffs(p, currentBuffIds);
        }
    }

    public void applyBuffs(Player p, List<Integer> ids) {
        for (int id : ids) applyBuff(p, id);
    }

    private void applyBuff(Player p, int id) {
        try {
            switch (id) {
                case 2 -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
                case 3 -> p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.12);
                case 4 -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                case 5 -> p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
                case 6 -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 7 -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                case 17 -> { p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.1); p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false)); }
                case 18 -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));
                case 22 -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 26 -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
                case 29 -> p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(30.0);
                case 30 -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                case 31 -> p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
                case 34 -> p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false));
                case 41 -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                case 43 -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false));
                case 50 -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 3, true, false));
            }
        } catch (Exception e) { plugin.getLogger().warning("Failed to apply buff " + id + ": " + e.getMessage()); }
    }

    public void removeAllBuffEffects(Player p) {
        p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
        p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
        p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
    }

    public boolean isBuffActive(int id) { return currentBuffIds.contains(id); }
    public List<Integer> getCurrentBuffIds() { return currentBuffIds; }
    public void setCurrentBuffIds(List<Integer> ids) { this.currentBuffIds = new ArrayList<>(ids); }
    public static String getBuffName(int id) { return (id > 0 && id < BUFF_NAMES.length) ? BUFF_NAMES[id] : "Unknown"; }
    public static String getBuffDesc(int id) { return (id > 0 && id < BUFF_DESC.length) ? BUFF_DESC[id] : ""; }
}
