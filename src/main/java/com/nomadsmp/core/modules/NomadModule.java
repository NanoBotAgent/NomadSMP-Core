package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.utils.HomeStorage;
import com.nomadsmp.core.utils.SafeLocationFinder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NomadModule {

    private final NomadCore plugin;
    private int taskId = -1;

    public NomadModule(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        // Check every 5 minutes if it is migration time
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::checkMigration, 6000L, 6000L).getTaskId();
    }

    public void disable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void checkMigration() {
        var config = plugin.getConfigManager();
        var now = java.time.LocalDateTime.now();
        if (now.getDayOfWeek() == config.getMigrateDay() && now.getHour() == config.getMigrateHour()) {
            // 5-minute warning
            Bukkit.broadcastMessage("\u00a7c\u00a7l\u26a0 THE WORLD SHIFTS IN 5 MINUTES. \u26a0 \u00a7r\u00a7ePrepare your home!");
            // Schedule actual migration 5 minutes later
            Bukkit.getScheduler().runTaskLater(plugin, this::runMigration, 6000L); // 5 min = 6000 ticks
        }
    }

    public void runMigration() {
        var config = plugin.getConfigManager();
        HomeStorage storage = plugin.getHomeStorage();
        Map<UUID, Location> homes = storage.getAllHomes();
        World world = Bukkit.getWorlds().getFirst();
        Random random = new Random();
        int radius = config.getBorderHalfSize();

        Bukkit.broadcastMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eThe world shifts! Homes are migrating...");

        for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
            UUID uuid = entry.getKey();
            Location oldHome = entry.getValue();

            // Find a safe new location
            Location newHome = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                int nx = random.nextInt(radius * 2) - radius;
                int nz = random.nextInt(radius * 2) - radius;
                newHome = SafeLocationFinder.findSafe(world, nx, nz);
                if (newHome != null) break;
            }

            if (newHome == null) {
                plugin.getLogger().warning("Could not find safe location for " + uuid + ", skipping.");
                continue;
            }

            // Copy and paste the house using WorldEdit
            int r = config.getHouseRadius();
            try {
                copyPasteSchematic(oldHome, newHome, r);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to migrate house for " + uuid + ": " + e.getMessage());
                continue;
            }

            // Update home storage
            storage.setHome(uuid, newHome);

            // Teleport online player
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(newHome);
                player.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eYour home has migrated to a new location!");
            }
        }

        Bukkit.broadcastMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aMigration complete! Find your new home.");
    }

    private void copyPasteSchematic(Location from, Location to, int radius) throws WorldEditException {
        var worldEdit = WorldEdit.getInstance();
        var weWorld = BukkitAdapter.adapt(from.getWorld());

        // Define source region
        BlockVector3 min = BlockVector3.at(from.getBlockX() - radius, from.getWorld().getMinHeight(), from.getBlockZ() - radius);
        BlockVector3 max = BlockVector3.at(from.getBlockX() + radius, from.getWorld().getMaxHeight(), from.getBlockZ() + radius);
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        // Copy to clipboard
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BlockVector3.at(from.getBlockX(), from.getBlockY(), from.getBlockZ()));
        var forward = clipboard.getRegion().iterator();
        var source = weWorld.getBlockDistribution();

        // Use WorldEdit operations
        var editSession = worldEdit.newEditSession(weWorld);
        var clipboardHolder = new ClipboardHolder(clipboard);
        Operation operation = clipboardHolder.createPaste(editSession)
                .to(BlockVector3.at(to.getBlockX(), to.getBlockY(), to.getBlockZ()))
                .copyEntities(false)
                .copyBiomes(true)
                .build();
        Operations.complete(operation);
        editSession.close();
    }
}
