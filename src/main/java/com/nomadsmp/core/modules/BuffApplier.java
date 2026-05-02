package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class BuffApplier {

    // MC 26.1.2: AttributeModifier uses NamespacedKey instead of UUID
    private static final NamespacedKey SPEED_KEY = new NamespacedKey(NomadCore.getInstance(), "speed-buff");
    private static final NamespacedKey SONIC_KEY = new NamespacedKey(NomadCore.getInstance(), "sonic-buff");
    private static final NamespacedKey HEALTH_KEY = new NamespacedKey(NomadCore.getInstance(), "health-buff");

    // MC 26.1.2: Operation is now ADD_VALUE (not ADD_NUMBER)
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
        SPEED_KEY, 0.12, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier SONIC_MODIFIER = new AttributeModifier(
        SONIC_KEY, 0.4, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
        HEALTH_KEY, 10.0, AttributeModifier.Operation.ADD_VALUE
    );

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        for (int id : ids) {
            applySingle(player, id, plugin);
        }
    }

    private static void applySingle(Player player, int id, NomadCore plugin) {
        try {
            // Infinite ambient potion effect (no particles)
            PotionEffect infinite = (PotionEffect effect) -> effect;
            
            switch (id) {
                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
                case 3 -> player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_MODIFIER);
                case 4 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
                case 6 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 7 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                case 17 -> {
                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_MODIFIER);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
                }
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
                case 29 -> player.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(HEALTH_MODIFIER);
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                case 31 -> player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SONIC_MODIFIER);
                case 34 -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false));
                case 41 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                case 43 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false));
                case 50 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 3, true, false));
                default -> {} // Listener-driven buffs
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply buff " + id + ": " + e.getMessage());
        }
    }

    public static void removeAll(Player player) {
        // Remove all potion effects
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        // Remove our attribute modifiers
        var speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        for (AttributeModifier mod : speedAttr.getModifiers()) {
            if (mod.getKey().getNamespace().equals("nomadsmp")) {
                speedAttr.removeModifier(mod);
            }
        }

        var healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        for (AttributeModifier mod : healthAttr.getModifiers()) {
            if (mod.getKey().getNamespace().equals("nomadsmp")) {
                healthAttr.removeModifier(mod);
            }
        }

        player.setRemainingAir(player.getMaximumAir());
    }
}
