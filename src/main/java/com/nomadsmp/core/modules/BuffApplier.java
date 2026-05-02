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

    private static final NamespacedKey SPEED_KEY = NamespacedKey.minecraft("nomad-speed");
    private static final NamespacedKey SONIC_KEY = NamespacedKey.minecraft("nomad-sonic");
    private static final NamespacedKey HEALTH_KEY = NamespacedKey.minecraft("nomad-health");

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        for (int id : ids) {
            applySingle(player, id, plugin);
        }
    }

    private static void applySingle(Player player, int id, NomadCore plugin) {
        try {
            switch (id) {
                // MC 26.1.2 PotionEffectType names (verified from javadoc)
                case 2  -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
                case 4  -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                case 5  -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
                case 6  -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 7  -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                case 34 -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false));
                case 43 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false));
                case 50 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 3, true, false));

                // Attribute modifiers — MC 26.1.2: no GENERIC_ prefix, NamespacedKey constructor
                case 3  -> player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                              new AttributeModifier(SPEED_KEY, 0.12, AttributeModifier.Operation.ADD_VALUE));
                case 17 -> {
                              player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                                  new AttributeModifier(SPEED_KEY, 0.12, AttributeModifier.Operation.ADD_VALUE));
                              player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
                          }
                case 29 -> player.getAttribute(Attribute.MAX_HEALTH).addModifier(
                              new AttributeModifier(HEALTH_KEY, 10.0, AttributeModifier.Operation.ADD_VALUE));
                case 31 -> player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                              new AttributeModifier(SONIC_KEY, 0.4, AttributeModifier.Operation.ADD_VALUE));
                case 41 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false));
                default -> {} // Listener-driven buffs
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply buff " + id + ": " + e.getMessage());
        }
    }

    public static void removeAll(Player player) {
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        // Remove our modifiers by key namespace
        for (Attribute attr : new Attribute[]{Attribute.MOVEMENT_SPEED, Attribute.MAX_HEALTH}) {
            var attrInstance = player.getAttribute(attr);
            if (attrInstance != null) {
                for (AttributeModifier mod : attrInstance.getModifiers()) {
                    if (mod.getKey().getNamespace().equals("minecraft")
                            && mod.getKey().getKey().startsWith("nomad-")) {
                        attrInstance.removeModifier(mod);
                    }
                }
            }
        }

        player.setRemainingAir(player.getMaximumAir());
    }
}
