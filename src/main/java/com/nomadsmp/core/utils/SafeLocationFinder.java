package com.nomadsmp.core.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SafeLocationFinder {
    public static Location findSafe(World world, int x, int z) {
        return findSafe(world, x, z, 10);
    }

    public static Location findSafe(World world, int x, int z, int retries) {
        for (int attempt = 0; attempt <= retries; attempt++) {
            int cx = (attempt == 0) ? x : x + (int) (Math.random() * 40 - 20);
            int cz = (attempt == 0) ? z : z + (int) (Math.random() * 40 - 20);
            for (int y = world.getMaxHeight(); y >= world.getMinHeight(); y--) {
                Block block = world.getBlockAt(cx, y, cz);
                if (!block.getType().isAir() && !block.isLiquid() && block.getType().isSolid()) {
                    if (y >= 60) return new Location(world, cx, y + 1, cz);
                    break;
                }
            }
        }
        return null;
    }
}
