package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiCheatListeners implements Listener {
    private final NomadCore plugin;
    public AntiCheatListeners(NomadCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onSeed(PlayerCommandPreprocessEvent e) {
        if (plugin.cfg().isBlockSeedCommand() && e.getMessage().toLowerCase().startsWith("/seed")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cThe seed is classified.");
        }
    }
}
