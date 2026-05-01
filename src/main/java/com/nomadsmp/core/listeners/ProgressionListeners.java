package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProgressionListeners implements Listener {
    private final NomadCore plugin;
    public ProgressionListeners(NomadCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if (e.getTo() != null && e.getTo().getWorld() != null
                && e.getTo().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END
                && plugin.prog().isEndLocked()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cThe End is sealed. Survive longer first.");
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        if (!plugin.cfg().isNetheriteCraftingBanned()) return;
        if (e.getResult() != null && e.getResult().getType().name().contains("NETHERITE"))
            e.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler
    public void onSmithing(InventoryClickEvent e) {
        if (!plugin.cfg().isNetheriteCraftingBanned()) return;
        if (e.getInventory() instanceof SmithingInventory) {
            ItemStack cursor = e.getCurrentItem();
            if (cursor != null && cursor.getType().name().contains("NETHERITE")) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onNetheriteEquip(InventoryClickEvent e) {
        if (!plugin.cfg().isNetheriteEquipPunish() || !(e.getWhoClicked() instanceof Player p)) return;
        ItemStack item = e.getCurrentItem();
        if (item != null && item.getType().name().contains("NETHERITE") && item.getType().name().contains("ARMOR")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
            p.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cNetherite armor is forbidden!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage().toLowerCase().split(" ")[0];
        if (plugin.cfg().isBlockGamemodeCommand() && (cmd.equals("/gamemode") || cmd.equals("/gm"))) {
            e.setCancelled(true); e.getPlayer().sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cAdmin commands are disabled.");
        }
        if (plugin.cfg().isBlockGiveCommand() && cmd.equals("/give")) {
            e.setCancelled(true); e.getPlayer().sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cAdmin commands are disabled.");
        }
        if (cmd.equals("/op") || cmd.equals("/deop")) {
            e.setCancelled(true); e.getPlayer().sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cAdmin commands are disabled.");
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        String cmd = e.getCommand().toLowerCase().split(" ")[0];
        if (plugin.cfg().isBlockGamemodeCommand() && (cmd.equals("gamemode") || cmd.equals("gm"))) e.setCancelled(true);
        if (plugin.cfg().isBlockGiveCommand() && cmd.equals("give")) e.setCancelled(true);
        if (cmd.equals("op") || cmd.equals("deop")) e.setCancelled(true);
    }
}
