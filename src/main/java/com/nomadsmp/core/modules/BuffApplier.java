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

public class BuffApplier {

    private static final NamespacedKey SPEED_KEY = NamespacedKey.minecraft("nomad-speed");
    private static final NamespacedKey SONIC_KEY = NamespacedKey.minecraft("nomad-sonic");
    private static final NamespacedKey HEALTH_KEY = NamespacedKey.minecraft("nomad-health");

    public static void apply(Player player, List<Integer> ids, NomadCore plugin) {
        ConfigManager cfg = plugin.getConfigManager();
        for (int id : ids) {
            if (!cfg.isBuffEnabled(id)) continue;
            applySingle(player, id, cfg);
        }
    }

    private static void applySingle(Player player, int id, ConfigManager cfg) {
        try {
            switch (id) {
                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, cfg.getPowerMinerAmplifier(), true, false));
                case 3 -> player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                    new AttributeModifier(SPEED_KEY, cfg.getRoadrunnerSpeed(), AttributeModifier.Operation.ADD_NUMBER));
                case 4 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, cfg.buffInt(4, "amplifier", 0), true, false));
                case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, cfg.buffInt(5, "amplifier", 0), true, false));
                case 6 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(6, "amplifier", 0), true, false));
                case 7 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, cfg.buffInt(7, "amplifier", 0), true, false));
                case 17 -> {
                    player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                        new AttributeModifier(SPEED_KEY, cfg.getDolphinSpeed(), AttributeModifier.Operation.ADD_NUMBER));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
                }
                case 18 -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, cfg.buffInt(18, "jump-amplifier", 1), true, false));
                case 22 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, cfg.buffInt(22, "resistance-amplifier", 0), true, false));
                case 26 -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, cfg.buffInt(26, "amplifier", 0), true, false));
                case 29 -> player.getAttribute(Attribute.MAX_HEALTH).addModifier(
                    new AttributeModifier(HEALTH_KEY, cfg.getHealthyHearts(), AttributeModifier.Operation.ADD_NUMBER));
                case 30 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, cfg.buffInt(30, "regen-amplifier", 1), true, false));
                case 31 -> player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(
                    new AttributeModifier(SONIC_KEY, cfg.getSonicSpeed(), AttributeModifier.Operation.ADD_NUMBER));
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
        for (Attribute attr : new Attribute[]{Attribute.MOVEMENT_SPEED, Attribute.MAX_HEALTH}) {
            var attrInstance = player.getAttribute(attr);
            if (attrInstance != null) {
                for (AttributeModifier mod : List.copyOf(attrInstance.getModifiers())) {
                    if (mod.getKey().getKey().startsWith("nomad-")) attrInstance.removeModifier(mod);
                }
            }
        }
        player.setRemainingAir(player.getMaximumAir());
    }
}
