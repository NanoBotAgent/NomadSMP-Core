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
 * attribute modifiers to avoid the FOV zoom that Minecraft applies client-side
 * when MOVEMENT_SPEED attributes change. walkSpeed is server-authoritative and
 * does NOT trigger FOV changes on the client.
 *
 * The base walk speed is 0.2 (vanilla default). We track any buff-applied
 * speed delta so we can cleanly add/remove without stacking errors.
 */
public class BuffApplier {

    private static final NamespacedKey HEALTH_KEY = NamespacedKey.minecraft("nomad-health");

    /** Vanilla default walk speed */
    private static final float BASE_WALK_SPEED = 0.2f;

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        ConfigManager cfg = plugin.getConfigManager();
        // Calculate total speed bonus from active speed buffs
        float speedBonus = 0f;
        for (int id : ids) {
            if (!cfg.isBuffEnabled(id)) continue;
            speedBonus += getSpeedBonus(id, cfg);
            applySingle(player, id, cfg);
        }
        // Apply combined walk speed (no FOV change)
        if (speedBonus > 0f) {
            player.setWalkSpeed(Math.min(BASE_WALK_SPEED + speedBonus, 1.0f));
        }
    }

    /** Returns the walkSpeed bonus for speed-related buffs, 0 for non-speed buffs. */
    private static float getSpeedBonus(int id, ConfigManager cfg) {
        return switch (id) {
            case 3  -> (float) cfg.getRoadrunnerSpeed();  // 0.12
            case 17 -> (float) cfg.getDolphinSpeed();     // 0.12
            case 31 -> (float) cfg.getSonicSpeed();       // 0.4
            default -> 0f;
        };
    }

    private static void applySingle(Player player, int id, ConfigManager cfg) {
        try {
            switch (id) {
                // 2: Power Miner — Haste (ambient, no particles)
                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, cfg.getPowerMinerAmplifier(), true, false));

                // 3: Roadrunner — speed handled via walkSpeed above (no FOV shift)

                // 4: Featherweight — Slow Falling
                case 4 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, cfg.buffInt(4, "amplifier", 0), true, false));

                // 5: Iron Lung — Water Breathing
                case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, cfg.buffInt(5, "amplifier", 0), true, false));

                // 6: Pyro — Fire Resistance
                case 6 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(6, "amplifier", 0), true, false));

                // 7: Night Owl — Night Vision
                case 7 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, cfg.buffInt(7, "amplifier", 0), true, false));

                // 17: Dolphin — speed handled via walkSpeed above; add Dolphin's Grace effect
                case 17 -> player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));

                // 18: Gravity — Jump Boost
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, cfg.buffInt(18, "jump-amplifier", 1), true, false));

                // 22: Tank — Resistance
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(22, "resistance-amplifier", 0), true, false));

                // 26: Ninja — Invisibility
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, cfg.buffInt(26, "amplifier", 0), true, false));

                // 29: Healthy — Extra hearts (attribute modifier, no FOV impact)
                case 29 -> player.getAttribute(Attribute.MAX_HEALTH).addModifier(
                    new AttributeModifier(HEALTH_KEY, cfg.getHealthyHearts(), AttributeModifier.Operation.ADD_NUMBER));

                // 30: Medic — Regen II
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, cfg.buffInt(30, "regen-amplifier", 1), true, false));

                // 31: Sonic — speed handled via walkSpeed above (no FOV shift)

                // 34: Glowstick — Glowing
                case 34 -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, cfg.buffInt(34, "amplifier", 0), true, false));

                // 41: Parachute — Slow Falling
                case 41 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, cfg.buffInt(41, "amplifier", 0), true, false));

                // 43: Warrior — Strength
                case 43 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, cfg.buffInt(43, "strength-amplifier", 0), true, false));

                // 50: Pacifist — Regen IV
                case 50 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, cfg.buffInt(50, "regen-amplifier", 3), true, false));

                default -> {}
            }
        } catch (Exception e) {
            // Avoid spam if attribute is missing
        }
    }

    public static void removeAll(Player player) {
        // Remove all potion effects
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        // Remove max health modifier
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            for (AttributeModifier mod : List.copyOf(healthAttr.getModifiers())) {
                if (mod.getKey().getKey().startsWith("nomad-")) healthAttr.removeModifier(mod);
            }
        }

        // Reset walk speed to vanilla default (removes any speed buff FOV-free speed)
        player.setWalkSpeed(BASE_WALK_SPEED);

        // Reset air (Whale buff)
        player.setRemainingAir(player.getMaximumAir());
    }
}
