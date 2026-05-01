package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.utils.HomeStorage;
import com.nomadsmp.core.utils.SafeLocationFinder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NomadModule {
    private final NomadCore plugin;
    private int taskId;
    private boolean warned = false;

    public NomadModule(NomadCore plugin) { this.plugin = plugin; }

    public void enable() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 0L, 6000L).getTaskId();
    }

    public void disable() { Bukkit.getScheduler().cancelTask(taskId); }

    private void check() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        int hour = LocalTime.now().getHour();
        DayOfWeek target = plugin.cfg().getMigrateDay();
        int targetHour = plugin.cfg().getMigrateHour();

        if (today == target && hour == targetHour) {
            if (!warned) {
                warned = true;
                Bukkit.broadcastMessage("\u00a7c\u00a7l\u26a0 THE WORLD SHIFTS IN 5 MINUTES. \u26a0 \u00a7r\u00a7ePrepare your home!");
                Bukkit.getScheduler().runTaskLater(plugin, this::runMigration, 6000L);
            }
        } else {
            warned = false;
        }
    }

    public void runMigration() {
        int borderSize = plugin.cfg().getBorderHalfSize();
        Random rng = new Random();
        HomeStorage storage = plugin.homes();
        World world = Bukkit.getWorlds().getFirst();

        Bukkit.broadcastMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eThe world shifts! Finding new homes...");

        for (Map.Entry<UUID, Location> entry : storage.getAllHomes().entrySet()) {
            UUID uuid = entry.getKey();
            Location oldHome = entry.getValue();

            int nx = rng.nextInt(borderSize * 2) - borderSize;
            int nz = rng.nextInt(borderSize * 2) - borderSize;
            Location newLoc = SafeLocationFinder.findSafe(world, nx, nz);

            if (newLoc == null) {
                plugin.getLogger().warning("Could not find safe location for " + uuid);
                continue;
            }

            int radius = plugin.cfg().getHouseRadius();
            try {
                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
                try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
                    BlockVector3 min = BlockVector3.at(oldHome.getBlockX() - radius, oldHome.getBlockY() - radius, oldHome.getBlockZ() - radius);
                    BlockVector3 max = BlockVector3.at(oldHome.getBlockX() + radius, oldHome.getBlockY() + radius, oldHome.getBlockZ() + radius);
                    CuboidRegion region = new CuboidRegion(weWorld, min, max);
                    com.sk89q.worldedit.function.operation.Operation op = session.copy(region, BlockVector3.at(newLoc.getBlockX() - radius, newLoc.getBlockY() - radius, newLoc.getBlockZ() - radius));
                    com.sk89q.worldedit.function.operation.Operations.complete(op);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("FAWE migration failed for " + uuid + ": " + e.getMessage());
                continue;
            }

            storage.setHome(uuid, newLoc);

            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    online.teleport(newLoc);
                    online.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eYour home has migrated to a new location!");
                });
            }
        }

        Bukkit.broadcastMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aMigration complete! New homes assigned.");
    }
}
