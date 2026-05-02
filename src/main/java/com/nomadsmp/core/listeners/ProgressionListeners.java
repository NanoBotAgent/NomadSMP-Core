package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProgressionListeners implements Listener {

    private final NomadCore plugin;

    public ProgressionListeners(NomadCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isProgressionLockEnabled()) return;
        if (!cfg.isEndLockEnabled()) return;
        if (plugin.getProgressionLockModule().isEndLocked()
            && event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
            && event.getTo() != null && event.getTo().getWorld() != null
            && event.getTo().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eThe End is sealed. Survive longer first.");
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isProgressionLockEnabled()) return;
        if (!cfg.isNetheriteCraftingBanEnabled()) return;
        ItemStack result = event.getInventory().getResult();
        if (result != null && result.getType().name().contains("NETHERITE")) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onSmithingClick(InventoryClickEvent event) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isProgressionLockEnabled()) return;
        if (!cfg.isNetheriteCraftingBanEnabled()) return;
        if (event.getInventory().getType() != InventoryType.SMITHING) return;
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType().name().contains("NETHERITE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        checkNetheritePunish(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkNetheritePunish(player), 1L);
        }
    }

    private void checkNetheritePunish(Player player) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isProgressionLockEnabled()) return;
        if (!cfg.isNetheriteEquipPunishEnabled()) return;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().contains("NETHERITE")) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    cfg.getNetheritePunishDurationTicks(),
                    cfg.getNetheritePunishAmplifier()
                ));
                player.sendMessage("\u00a7c[NomadSMP] \u00a7eNetherite armor is too heavy! Slowness applied.");
                return;
            }
        }
    }
}
