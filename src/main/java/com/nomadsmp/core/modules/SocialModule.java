package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class SocialModule {

    private final NomadCore plugin;

    public SocialModule(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        var config = plugin.getConfigManager();

        // Set world border
        if (config.isWorldBorder()) {
            World world = Bukkit.getWorlds().getFirst();
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0, 0);
            int size = config.getBorderHalfSize() * 2;
            border.setSize(size);
            border.setDamageAmount(0.5);
            border.setWarningDistance(10);
            plugin.getLogger().info("World border set to " + size + "x" + size);
        }

        // Strip OP on startup
        if (config.isNoAdminOp() && config.isBlockGamemodeCommand()) {
            stripAllOp();
        }

        plugin.getLogger().info("Social module enabled.");
    }

    private void stripAllOp() {
        for (var opPlayer : Bukkit.getOperators()) {
            opPlayer.setOp(false);
            plugin.getLogger().info("De-opped: " + opPlayer.getName());
        }
    }
}
