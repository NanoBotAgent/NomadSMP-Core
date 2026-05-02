package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ProgressionLockModule {

    private final NomadCore plugin;

    public ProgressionLockModule(NomadCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getLogger().info("Progression lock module enabled.");
        checkEndUnlock();
    }

    public boolean isEndLocked() {
        var cfg = plugin.getConfigManager();
        if (!cfg.isEndLockEnabled()) return false;
        return getDaysSinceCreation() < cfg.getEndUnlockDays();
    }

    public long getDaysSinceCreation() {
        String startDate = plugin.getConfigManager().getServerStartDate();
        try {
            LocalDate start = LocalDate.parse(startDate);
            return ChronoUnit.DAYS.between(start, LocalDate.now());
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid server-start-date: " + startDate + ", using 0");
            return 0;
        }
    }

    public long getDaysUntilEndUnlock() {
        var cfg = plugin.getConfigManager();
        if (!cfg.isEndLockEnabled()) return 0;
        long daysSince = getDaysSinceCreation();
        long required = cfg.getEndUnlockDays();
        return Math.max(0, required - daysSince);
    }

    private void checkEndUnlock() {
        if (!isEndLocked() && plugin.getConfigManager().isEndLockEnabled()) {
            org.bukkit.Bukkit.broadcastMessage("\u00a7a[NomadSMP] \u00a7eThe End has been unsealed. Good luck.");
        }
    }
}
