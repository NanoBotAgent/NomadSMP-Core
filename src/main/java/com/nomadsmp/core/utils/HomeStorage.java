package com.nomadsmp.core.utils;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
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
        loadHomes();
    }

    private void loadHomes() {
        homes.clear();
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection sec = yaml.getConfigurationSection(key);
                if (sec == null) continue;
                String worldName = sec.getString("world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) world = Bukkit.getWorlds().getFirst();
                double x = sec.getDouble("x");
                double y = sec.getDouble("y");
                double z = sec.getDouble("z");
                homes.put(uuid, new Location(world, x, y, z));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid home entry: " + key);
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
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save homes.yml: " + e.getMessage());
        }
    }

    public void setHome(UUID uuid, Location location) {
        homes.put(uuid, location);
        save();
    }

    public Location getHome(UUID uuid) { return homes.get(uuid); }
    public boolean hasHome(UUID uuid) { return homes.containsKey(uuid); }
    public void removeHome(UUID uuid) {
        homes.remove(uuid);
        yaml.set(uuid.toString(), null);
        save();
    }
    public Map<UUID, Location> getAllHomes() { return Map.copyOf(homes); }
}
