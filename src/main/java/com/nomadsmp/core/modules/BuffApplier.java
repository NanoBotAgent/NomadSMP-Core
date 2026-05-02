package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

public class BuffApplier {

    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
        UUID.nameUUIDFromBytes("nomad-speed".getBytes()),
        "nomad-speed", 0.12, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier SONIC_MODIFIER = new AttributeModifier(
        UUID.nameUUIDFromBytes("nomad-sonic".getBytes()),
        "nomad-sonic", 0.4, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
        UUID.nameUUIDFromBytes("nomad-healthy".getBytes()),
        "nomad-healthy", 10.0, AttributeModifier.Operation.ADD_VALUE
    );

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        for (int id : ids) {
            applySingle(player, id, plugin);
        }
    }

    private static void applySingle(Player player, int id, NomadCore plugin) {
        try {
            switch (id) {
                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false)); // Power Miner
                case 3 -> player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_MODIFIER); // Roadrunner
                case 4 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false)); // Featherweight
                case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false)); // Iron Lung
                case 6 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false)); // Pyro
                case 7 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false)); // Night Owl
                case 12 -> {} // Chef — handled by listener
                case 17 -> { // Dolphin
                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_MODIFIER);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
                }
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, true, false)); // Gravity
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false)); // Tank
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false)); // Ninja
                case 29 -> player.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(HEALTH_MODIFIER); // Healthy
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false)); // Medic
                case 31 -> player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SONIC_MODIFIER); // Sonic
                case 34 -> player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false)); // Glowstick
                case 41 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false)); // Parachute
                case 43 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false)); // Warrior
                case 50 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 3, true, false)); // Pacifist
                // All other buffs (1, 8-11, 13-16, 19-21, 23-25, 27-28, 32-40, 42, 44-49) are handled via listeners
                default -> {} // Listener-driven buffs
            }
        } catch (Exception e) {
            NomadCore.getInstance().getLogger().warning("Failed to apply buff " + id + " to " + player.getName() + ": " + e.getMessage());
        }
    }

    public static void removeAll(Player player) {
        // Remove all potion effects
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        // Reset movement speed
        var speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        for (AttributeModifier mod : speedAttr.getModifiers()) {
            if (mod.getKey().getNamespace().equals("nomad-speed") || mod.getKey().getNamespace().equals("nomad-sonic")) {
                speedAttr.removeModifier(mod);
            }
        }

        // Reset max health
        var healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        for (AttributeModifier mod : healthAttr.getModifiers()) {
            if (mod.getKey().getNamespace().equals("nomad-healthy")) {
                healthAttr.removeModifier(mod);
            }
        }

        // Reset air
        player.setRemainingAir(player.getMaximumAir());
    }
}
