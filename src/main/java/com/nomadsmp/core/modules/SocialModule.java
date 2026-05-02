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

        // Set world border from config
        if (config.isWorldBorderEnabled()) {
            World world = Bukkit.getWorlds().getFirst();
            WorldBorder border = world.getWorldBorder();
            border.setCenter(config.getWorldBorderCenterX(), config.getWorldBorderCenterZ());
            border.setSize(config.getWorldBorderSize());
            border.setDamageAmount(config.getWorldBorderDamageAmount());
            border.setWarningDistance(config.getWorldBorderWarningDistance());
            plugin.getLogger().info("World border set to " + config.getWorldBorderSize()
                + "x" + config.getWorldBorderSize()
                + " centered at " + config.getWorldBorderCenterX() + "," + config.getWorldBorderCenterZ());
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
