package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class SocialListeners implements Listener {
    private final NomadCore plugin;
    public SocialListeners(NomadCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.cfg().isPlayerHeadDrop()) return;
        if (e.getEntity().getKiller() instanceof Player killer) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) { meta.setPlayerProfile(killer.getPlayerProfile()); head.setItemMeta(meta); }
            e.getDrops().add(head);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage().toLowerCase().split(" ")[0];
        String msg = "\u00a78[\u00a76NomadSMP\u00a78] \u00a7cTeleportation is disabled. Walk there.";
        if (plugin.cfg().isDisableTpa() && (cmd.equals("/tpa") || cmd.equals("/tpaccept") || cmd.equals("/tpdeny"))) { e.setCancelled(true); e.getPlayer().sendMessage(msg); }
        if (plugin.cfg().isDisableWarp() && cmd.equals("/warp")) { e.setCancelled(true); e.getPlayer().sendMessage(msg); }
        if (plugin.cfg().isDisableHomeCommand() && (cmd.equals("/home") || cmd.equals("/sethome"))) { e.setCancelled(true); e.getPlayer().sendMessage(msg); }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (plugin.cfg().isNoAdminOp() && plugin.cfg().isBlockGamemodeCommand() && e.getPlayer().isOp()) {
            e.getPlayer().setOp(false);
            plugin.getLogger().info("De-opped on login: " + e.getPlayer().getName());
        }
    }
}
