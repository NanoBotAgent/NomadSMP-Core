package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Applies buff effects to players.
 *
 * Speed-related buffs (Roadrunner, Dolphin, Sonic) use walkSpeed instead of
 * attribute modifiers to avoid the FOV zoom that Minecraft applies client-side.
 * Stacking behavior is controlled by config: highest | additive | replace.
 */
public class BuffApplier {

    private static final NamespacedKey HEALTH_KEY = NamespacedKey.minecraft("nomad-health");
    private static final float BASE_WALK_SPEED = 0.2f;

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        ConfigManager cfg = plugin.getConfigManager();
        float speedBonus = calculateSpeedBonus(ids, cfg);

        if (speedBonus > 0f) {
            player.setWalkSpeed(Math.min(BASE_WALK_SPEED + speedBonus, 1.0f));
        }

        for (int id : ids) {
            if (!cfg.isBuffEnabled(id)) continue;
            applySingle(player, id, cfg);
        }
    }

    /** Calculate combined speed bonus based on stacking mode. */
    private static float calculateSpeedBonus(List<Integer> ids, ConfigManager cfg) {
        ConfigManager.BuffStacking mode = cfg.getStacking();

        float roadrunner = 0f, dolphin = 0f, sonic = 0f;
        for (int id : ids) {
            if (!cfg.isBuffEnabled(id)) continue;
            switch (id) {
                case 3  -> roadrunner = (float) cfg.getRoadrunnerSpeed();
                case 17 -> dolphin = (float) cfg.getDolphinSpeed();
                case 31 -> sonic = (float) cfg.getSonicSpeed();
            }
        }

        return switch (mode) {
            case HIGHEST  -> Math.max(roadrunner, Math.max(dolphin, sonic));
            case REPLACE  -> sonic > 0 ? sonic : (dolphin > 0 ? dolphin : roadrunner); // highest priority: sonic > dolphin > roadrunner
            case ADDITIVE -> roadrunner + dolphin + sonic;
        };
    }

    private static void applySingle(Player player, int id, ConfigManager cfg) {
        try {
            switch (id) {
                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, cfg.getPowerMinerAmplifier(), true, false));
                // 3: Roadrunner — speed via walkSpeed, no potion effect
                case 4 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, cfg.buffInt(4, "amplifier", 0), true, false));
                case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, cfg.buffInt(5, "amplifier", 0), true, false));
                case 6 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(6, "amplifier", 0), true, false));
                case 7 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, cfg.buffInt(7, "amplifier", 0), true, false));
                case 17 -> player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, cfg.buffInt(18, "jump-amplifier", 1), true, false));
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(22, "resistance-amplifier", 0), true, false));
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, cfg.buffInt(26, "amplifier", 0), true, false));
                case 29 -> player.getAttribute(Attribute.MAX_HEALTH).addModifier(
                    new AttributeModifier(HEALTH_KEY, cfg.getHealthyHearts(), AttributeModifier.Operation.ADD_NUMBER));
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, cfg.buffInt(30, "regen-amplifier", 1), true, false));
                // 31: Sonic — speed via walkSpeed, no potion effect
                case 34 -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, cfg.buffInt(34, "amplifier", 0), true, false));
                case 41 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, cfg.buffInt(41, "amplifier", 0), true, false));
                case 43 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, cfg.buffInt(43, "strength-amplifier", 0), true, false));
                case 50 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, cfg.buffInt(50, "regen-amplifier", 3), true, false));
                default -> {}
            }
        } catch (Exception e) {
            // Avoid spam if attribute is missing
        }
    }

    public static void removeAll(Player player) {
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            for (AttributeModifier mod : List.copyOf(healthAttr.getModifiers())) {
                if (mod.getKey().getKey().startsWith("nomad-")) healthAttr.removeModifier(mod);
            }
        }
        player.setWalkSpeed(BASE_WALK_SPEED);
        player.setRemainingAir(player.getMaximumAir());
    }
}