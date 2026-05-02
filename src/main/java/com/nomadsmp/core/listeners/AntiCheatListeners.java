package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class AntiCheatListeners implements Listener {

    private final NomadCore plugin;

    public AntiCheatListeners(NomadCore plugin) {
        this.plugin = plugin;
    }

    // ─── Block /seed command ───
    @EventHandler
    public void onSeedCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isBlockSeedCommand()) return;
        String cmd = event.getMessage().toLowerCase().trim();
        if (cmd.equals("/seed") || cmd.startsWith("/seed ")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eThe seed is classified.");
        }
    }

    // ─── Block admin commands (gamemode, give, op) ───
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        var config = plugin.getConfigManager();
        String cmd = event.getMessage().toLowerCase().trim().split(" ")[0];

        if (config.isBlockGamemodeCommand() && isBlockedCommand(cmd)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c[NomadSMP] \u00a7eAdmin commands are disabled on this server.");
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        var config = plugin.getConfigManager();
        String cmd = event.getCommand().toLowerCase().trim().split(" ")[0];

        if (config.isBlockGamemodeCommand() && isBlockedCommand(cmd)) {
            event.setCancelled(true);
        }
    }

    private boolean isBlockedCommand(String cmd) {
        return cmd.equals("/gamemode") || cmd.equals("/gm")
            || cmd.equals("/give") || cmd.equals("/op") || cmd.equals("/deop");
    }
}
