package com.nomadsmp.core.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SafeLocationFinder {

    /**
     * Walk down from max height at (x, z) to find first solid, non-liquid block.
     * If Y < 60 (ocean floor risk), returns null to trigger a retry.
     */
    public static Location findSafe(World world, int x, int z) {
        for (int y = world.getMaxHeight(); y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid() && !isLiquid(block.getType())) {
                if (y < 60) return null; // Ocean floor risk
                return new Location(world, x, y + 1, z);
            }
        }
        return null; // No safe spot found
    }

    private static boolean isLiquid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA;
    }
}
