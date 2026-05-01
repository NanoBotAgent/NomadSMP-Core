package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;

public class AntiCheatModule {
    private final NomadCore plugin;
    public AntiCheatModule(NomadCore plugin) { this.plugin = plugin; }
    public void enable() {
        plugin.getLogger().info("Anti-cheat module enabled. Configure paper-world.yml for ore obfuscation & seed scrambling.");
    }
}
