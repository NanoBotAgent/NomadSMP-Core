package com.nomadsmp.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Automatically downloads required dependencies (FAWE / WorldEdit) on first startup
 * if they are not already present in the server's plugins folder.
 *
 * Download sources (configurable in config.yml):
 * - FAWE: https://github.com/roggy666/FastAsyncWorldEdit-26.1/releases/download/26.1/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT.jar
 * - WorldEdit: https://cdn.modrinth.com/data/1u6JkXh5/versions/yDUBafTJ/worldedit-bukkit-7.4.3.jar
 */
public class DependencyDownloader {

    private final Plugin plugin;

    // Default download URLs
    private static final String DEFAULT_FAWE_URL =
            "https://github.com/roggy666/FastAsyncWorldEdit-26.1/releases/download/26.1/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT.jar";
    private static final String DEFAULT_WE_URL =
            "https://cdn.modrinth.com/data/1u6JkXh5/versions/yDUBafTJ/worldedit-bukkit-7.4.3.jar";

    // File names for the downloaded jars
    private static final String FAWE_FILE = "FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT.jar";
    private static final String WE_FILE = "worldedit-bukkit-7.4.3.jar";

    // Marker file to skip downloads on subsequent starts
    private static final String MARKER_FILE = ".nomadsmp-deps-installed";

    public DependencyDownloader(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check and download missing dependencies. Runs synchronously during onEnable.
     * Prefers FAWE if neither is installed; falls back to WorldEdit if FAWE download fails.
     */
    public void ensureDependencies() {
        File pluginsDir = plugin.getDataFolder().getParentFile(); // /plugins/
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            plugin.getLogger().warning("Could not locate plugins directory for dependency download.");
            return;
        }

        // Check if WorldEdit or FAWE is already installed as a plugin
        boolean hasFawe = isPluginInstalled("FastAsyncWorldEdit", pluginsDir);
        boolean hasWe = isPluginInstalled("WorldEdit", pluginsDir);

        if (hasFawe) {
            plugin.getLogger().info("FastAsyncWorldEdit detected — skipping dependency download.");
            return;
        }
        if (hasWe) {
            plugin.getLogger().info("WorldEdit detected — skipping dependency download.");
            return;
        }

        // Check marker file (skip if we already downloaded in a previous start)
        File marker = new File(pluginsDir, MARKER_FILE);
        if (marker.exists()) {
            plugin.getLogger().info("Dependencies were previously downloaded. If they're missing, delete "
                    + MARKER_FILE + " in the plugins folder to re-trigger download.");
            return;
        }

        // Read URLs from config (allows overrides)
        String faweUrl = plugin.getConfig().getString("dependency-downloads.fawe-url", DEFAULT_FAWE_URL);
        String weUrl = plugin.getConfig().getString("dependency-downloads.worldedit-url", DEFAULT_WE_URL);
        boolean preferFawe = plugin.getConfig().getBoolean("dependency-downloads.prefer-fawe", true);

        plugin.getLogger().info("No WorldEdit/FAWE installation detected!");
        plugin.getLogger().info("Automatically downloading " + (preferFawe ? "FAWE" : "WorldEdit") + "...");

        boolean success = false;

        if (preferFawe) {
            // Try FAWE first, fall back to WorldEdit
            success = downloadJar(faweUrl, new File(pluginsDir, FAWE_FILE), "FAWE");
            if (!success) {
                plugin.getLogger().warning("FAWE download failed — falling back to WorldEdit...");
                success = downloadJar(weUrl, new File(pluginsDir, WE_FILE), "WorldEdit");
            }
        } else {
            // WorldEdit only
            success = downloadJar(weUrl, new File(pluginsDir, WE_FILE), "WorldEdit");
        }

        if (success) {
            // Create marker file
            try {
                marker.createNewFile();
                try (var writer = new FileWriter(marker)) {
                    writer.write("NomadSMP-Core auto-installed dependencies on " + java.time.Instant.now() + "\n");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create dependency marker file: " + e.getMessage());
            }

            plugin.getLogger().info("╔══════════════════════════════════════════════════════════╗");
            plugin.getLogger().info("║  Dependency downloaded successfully!                    ║");
            plugin.getLogger().info("║  Please RESTART the server for it to take effect.       ║");
            plugin.getLogger().info("╚══════════════════════════════════════════════════════════╝");
        } else {
            plugin.getLogger().severe("╔══════════════════════════════════════════════════════════╗");
            plugin.getLogger().severe("║  Automatic dependency download FAILED!                  ║");
            plugin.getLogger().severe("║  Please manually install one of:                        ║");
            plugin.getLogger().severe("║  - FAWE: " + DEFAULT_FAWE_URL);
            plugin.getLogger().severe("║  - WorldEdit: " + DEFAULT_WE_URL);
            plugin.getLogger().severe("║  Place the .jar in your /plugins/ folder and restart.   ║");
            plugin.getLogger().severe("╚══════════════════════════════════════════════════════════╝");
        }
    }

    /**
     * Download a JAR file from a URL. Follows redirects (up to 5).
     */
    private boolean downloadJar(String urlString, File targetFile, String label) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = followRedirects(url, 5);

            if (conn == null) {
                plugin.getLogger().severe("[" + label + "] Too many redirects — download aborted.");
                return false;
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().severe("[" + label + "] HTTP " + responseCode + " — download failed.");
                conn.disconnect();
                return false;
            }

            long contentLength = conn.getContentLengthLong();
            String sizeStr = contentLength > 0 ? String.format("%.1f MB", contentLength / (1024.0 * 1024.0)) : "unknown size";

            plugin.getLogger().info("[" + label + "] Downloading " + sizeStr + " from " + urlString);

            // Download to temp file first, then move (avoids partial files on crash)
            File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
            try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                }
                out.flush();
                plugin.getLogger().info("[" + label + "] Downloaded " + String.format("%.1f MB", totalRead / (1024.0 * 1024.0)));
            }

            // Move temp file to final location
            if (targetFile.exists()) {
                targetFile.delete();
            }
            if (!tempFile.renameTo(targetFile)) {
                // Fallback: copy then delete
                Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tempFile.delete();
            }

            conn.disconnect();
            plugin.getLogger().info("[" + label + "] Saved to " + targetFile.getName());
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("[" + label + "] Download error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Follow HTTP redirects up to maxRedirects times.
     */
    private HttpURLConnection followRedirects(URL url, int maxRedirects) throws Exception {
        int redirects = 0;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        while (redirects < maxRedirects) {
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) return null;
                url = new URL(url, location);
                redirects++;
                conn = (HttpURLConnection) url.openConnection();
            } else {
                return conn;
            }
        }
        conn.disconnect();
        return null;
    }

    /**
     * Check if a plugin JAR exists in the plugins directory or is already loaded.
     */
    private boolean isPluginInstalled(String pluginName, File pluginsDir) {
        // Check if Bukkit has already loaded it
        if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
            return true;
        }

        // Check for JAR files containing the plugin name (case-insensitive)
        File[] jars = pluginsDir.listFiles((dir, name) ->
                name.toLowerCase().contains(pluginName.toLowerCase()) && name.endsWith(".jar"));

        return jars != null && jars.length > 0;
    }
}
