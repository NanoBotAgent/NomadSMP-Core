package com.nomadsmp.core.utils;

import com.nomadsmp.core.NomadCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SafeLocationFinder {

    public static Location findSafe(World world, int x, int z, int minSafeY) {
        for (int y = world.getMaxHeight(); y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid() && !isLiquid(block.getType())) {
                if (y < minSafeY) return null;
                return new Location(world, x, y + 1, z);
            }
        }
        return null;
    }

    private static boolean isLiquid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA;
    }
}
