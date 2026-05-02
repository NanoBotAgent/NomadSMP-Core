package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class SocialListeners implements Listener {

    private final NomadCore plugin;

    // Commands to block for teleport disable
    private static final List<String> TPA_COMMANDS = List.of("/tpa", "/tpaccept", "/tpdeny");
    private static final List<String> WARP_COMMANDS = List.of("/warp");
    private static final List<String> HOME_COMMANDS = List.of("/home", "/sethome");

    public SocialListeners(NomadCore plugin) {
        this.plugin = plugin;
    }

    // ─── Player Head Drop on PvP Kill ───
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isPlayerHeadDrop()) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return; // Not PvP

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(victim.getPlayerProfile());
            meta.setDisplayName("\u00a7f" + victim.getName() + "'s Head");
            head.setItemMeta(meta);
        }
        event.getDrops().add(head);
    }

    // ─── Block Teleport Commands ───
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase().trim().split(" ")[0];
        var config = plugin.getConfigManager();

        if (config.isDisableTpa() && TPA_COMMANDS.contains(cmd)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eTeleportation is disabled. Walk there.");
        }
        if (config.isDisableWarp() && WARP_COMMANDS.contains(cmd)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eTeleportation is disabled. Walk there.");
        }
        if (config.isDisableHomeCommand() && HOME_COMMANDS.contains(cmd)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eTeleportation is disabled. Walk there.");
        }
    }

    // ─── De-op on login ───
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (plugin.getConfigManager().isNoAdminOp() && plugin.getConfigManager().isBlockGamemodeCommand()) {
            if (event.getPlayer().isOp()) {
                event.getPlayer().setOp(false);
                plugin.getLogger().info("De-opped on login: " + event.getPlayer().getName());
            }
        }
    }
}
