package com.nomadsmp.core.utils;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeStorage {
    private final NomadCore plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, Location> homes = new HashMap<>();

    public HomeStorage(NomadCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        homes.clear();
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String worldName = yaml.getString(key + ".world", "world");
                World w = Bukkit.getWorld(worldName);
                if (w == null) w = Bukkit.getWorlds().getFirst();
                double x = yaml.getDouble(key + ".x");
                double y = yaml.getDouble(key + ".y");
                double z = yaml.getDouble(key + ".z");
                homes.put(uuid, new Location(w, x, y, z));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load home for " + key + ": " + e.getMessage());
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
            String key = entry.getKey().toString();
            Location loc = entry.getValue();
            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x", loc.getX());
            yaml.set(key + ".y", loc.getY());
            yaml.set(key + ".z", loc.getZ());
        }
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().severe("Failed to save homes.yml: " + e.getMessage()); }
    }

    public void setHome(UUID uuid, Location loc) { homes.put(uuid, loc); save(); }
    public Location getHome(UUID uuid) { return homes.get(uuid); }
    public boolean hasHome(UUID uuid) { return homes.containsKey(uuid); }
    public void removeHome(UUID uuid) { homes.remove(uuid); yaml.set(uuid.toString(), null); save(); }
    public Map<UUID, Location> getAllHomes() { return Map.copyOf(homes); }
}
