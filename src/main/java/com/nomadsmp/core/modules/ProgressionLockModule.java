package com.nomadsmp.core.modules;

import com.nomadsmp.core.NomadCore;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ProgressionLockModule {
    private final NomadCore plugin;
    public ProgressionLockModule(NomadCore plugin) { this.plugin = plugin; }
    public void enable() { plugin.getLogger().info("Progression lock module enabled."); }

    public boolean isEndLocked() {
        if (!plugin.cfg().isEndLocked()) return false;
        try {
            LocalDate start = LocalDate.parse(plugin.cfg().getServerStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            long days = ChronoUnit.DAYS.between(start, LocalDate.now());
            return days < plugin.cfg().getEndUnlockDays();
        } catch (Exception e) { return plugin.cfg().isEndLocked(); }
    }

    public long daysUntilEndUnlock() {
        try {
            LocalDate start = LocalDate.parse(plugin.cfg().getServerStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            long days = ChronoUnit.DAYS.between(start, LocalDate.now());
            return Math.max(0, plugin.cfg().getEndUnlockDays() - days);
        } catch (Exception e) { return plugin.cfg().getEndUnlockDays(); }
    }
}
