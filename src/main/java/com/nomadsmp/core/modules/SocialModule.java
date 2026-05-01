package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class SocialModule {
    private final NomadCore plugin;
    public SocialModule(NomadCore plugin) { this.plugin = plugin; }

    public void enable() {
        if (plugin.cfg().isWorldBorder()) {
            int half = plugin.cfg().getBorderHalfSize();
            World world = Bukkit.getWorlds().getFirst();
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(half * 2);
            border.setDamageAmount(0.5);
            border.setWarningDistance(10);
        }
        if (plugin.cfg().isNoAdminOp() && plugin.cfg().isBlockGamemodeCommand()) {
            Bukkit.getOperators().forEach(op -> { op.setOp(false); plugin.getLogger().info("De-opped: " + op.getName()); });
        }
        plugin.getLogger().info("Social module enabled.");
    }
}
