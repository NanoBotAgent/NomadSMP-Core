package com.nomadsmp.core.commands;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.config.ConfigManager;
import com.nomadsmp.core.modules.DailyBuffModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class NomadCommand implements CommandExecutor, TabCompleter {

    private final NomadCore plugin;

    public NomadCommand(NomadCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aConfig reloaded.");
            }
            case "status" -> showStatus(sender);
            case "migratenow" -> {
                if (!(sender instanceof Player) || sender.hasPermission("nomad.admin")) {
                    plugin.getNomadModule().runMigration();
                    sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eMigration triggered!");
                }
            }
            case "setbuff" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage("\u00a7cUsage: /nomad setbuff <id> [id2] [id3]...");
                    return true;
                }
                List<Integer> ids = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    try {
                        int id = Integer.parseInt(args[i]);
                        if (id < 1 || id > 50) {
                            sender.sendMessage("\u00a7cBuff ID must be 1-50. Got: " + id);
                            return true;
                        }
                        ids.add(id);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("\u00a7cInvalid buff ID: " + args[i]);
                        return true;
                    }
                }
                plugin.getDailyBuffModule().setCurrentBuffIds(ids);
                reapplyBuffs();
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aBuff set to: " + buffNames(ids));
            }
            case "setbuffpool" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 4) {
                    sender.sendMessage("\u00a7cUsage: /nomad setbuffpool <day> <mode> <id1,id2,...>");
                    sender.sendMessage("\u00a77Modes: fixed, random, off");
                    sender.sendMessage("\u00a77Days: monday, tuesday, wednesday, thursday, friday, saturday, sunday");
                    return true;
                }
                String dayStr = args[1].toLowerCase();
                String modeStr = args[2].toLowerCase();
                String poolStr = args[3];

                // Validate day
                DayOfWeek day;
                try {
                    day = DayOfWeek.valueOf(dayStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("\u00a7cInvalid day: " + dayStr);
                    return true;
                }

                // Validate mode
                ConfigManager.BuffMode mode = switch (modeStr) {
                    case "fixed" -> ConfigManager.BuffMode.FIXED;
                    case "random" -> ConfigManager.BuffMode.RANDOM;
                    case "off" -> ConfigManager.BuffMode.OFF;
                    default -> {
                        sender.sendMessage("\u00a7cInvalid mode: " + modeStr + ". Use fixed, random, or off.");
                        yield null;
                    }
                };
                if (mode == null) return true;

                // Parse pool/buffs
                List<Integer> ids = new ArrayList<>();
                if (!poolStr.equalsIgnoreCase("none") && !poolStr.equalsIgnoreCase("empty")) {
                    for (String part : poolStr.split(",")) {
                        try {
                            int id = Integer.parseInt(part.trim());
                            if (id < 1 || id > 50) {
                                sender.sendMessage("\u00a7cBuff ID must be 1-50. Got: " + id);
                                return true;
                            }
                            ids.add(id);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("\u00a7cInvalid buff ID: " + part);
                            return true;
                        }
                    }
                }

                // Write to config
                String configPath = "daily-buffs." + dayStr;
                plugin.getConfig().set(configPath + ".mode", modeStr);
                if (mode == ConfigManager.BuffMode.FIXED) {
                    plugin.getConfig().set(configPath + ".buffs", ids);
                    plugin.getConfig().set(configPath + ".pool", List.of());
                } else if (mode == ConfigManager.BuffMode.RANDOM) {
                    plugin.getConfig().set(configPath + ".buffs", List.of());
                    plugin.getConfig().set(configPath + ".pool", ids);
                } else {
                    plugin.getConfig().set(configPath + ".buffs", List.of());
                    plugin.getConfig().set(configPath + ".pool", List.of());
                }
                plugin.saveConfig();
                plugin.reload();

                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7a" + dayStr + " set to " + modeStr
                    + (ids.isEmpty() ? "" : " with [" + ids + "]") + ". Config saved and reloaded.");
            }
            case "setborder" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage("\u00a7cUsage: /nomad setborder <size> [centerX] [centerZ]");
                    return true;
                }
                try {
                    int size = Integer.parseInt(args[1]);
                    int cx = args.length >= 3 ? Integer.parseInt(args[2]) : plugin.getConfigManager().getWorldBorderCenterX();
                    int cz = args.length >= 4 ? Integer.parseInt(args[3]) : plugin.getConfigManager().getWorldBorderCenterZ();

                    // Update config
                    plugin.getConfig().set("social.world-border.size", size);
                    plugin.getConfig().set("social.world-border.center-x", cx);
                    plugin.getConfig().set("social.world-border.center-z", cz);
                    plugin.saveConfig();

                    // Apply immediately
                    World world = Bukkit.getWorlds().getFirst();
                    WorldBorder border = world.getWorldBorder();
                    border.setCenter(cx, cz);
                    border.setSize(size);

                    sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aWorld border set to " + size + "x" + size
                        + " centered at " + cx + ", " + cz);
                } catch (NumberFormatException e) {
                    sender.sendMessage("\u00a7cInvalid number format.");
                }
            }
            case "buffs" -> listBuffs(sender);
            case "sethome" -> {
                if (sender instanceof Player player) {
                    plugin.getHomeStorage().setHome(player.getUniqueId(), player.getLocation());
                    player.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aHome set at your current location.");
                } else {
                    sender.sendMessage("\u00a7cOnly players can set homes.");
                }
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private boolean checkAdmin(CommandSender sender) {
        if (!sender.hasPermission("nomad.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        return true;
    }

    private void reapplyBuffs() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            plugin.getDailyBuffModule().removeAllBuffEffects(p);
            plugin.getDailyBuffModule().applyToPlayer(p);
        }
    }

    private String buffNames(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        DailyBuffModule bm = plugin.getDailyBuffModule();
        for (int id : ids) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("\u00a7a").append(bm.getBuffName(id)).append("\u00a77 (").append(id).append(")");
        }
        return sb.toString();
    }

    private void listBuffs(CommandSender sender) {
        DailyBuffModule bm = plugin.getDailyBuffModule();
        sender.sendMessage("\u00a78\u00a7m                          ");
        sender.sendMessage("\u00a76\u00a7l Buff Reference (1-50)");
        sender.sendMessage("\u00a78\u00a7m                          ");
        for (int i = 1; i < DailyBuffModule.BUFF_NAMES.length; i++) {
            String active = bm.isBuffActive(i) ? "\u00a7a\u2714 " : "\u00a77  ";
            sender.sendMessage(active + "\u00a7e" + i + ". \u00a7f" + bm.getBuffName(i)
                + " \u00a77\u2014 " + bm.getBuffDescription(i));
        }
        sender.sendMessage("\u00a78\u00a7m                          ");
    }

    private void showStatus(CommandSender sender) {
        DailyBuffModule buffModule = plugin.getDailyBuffModule();
        List<Integer> buffIds = buffModule.getCurrentBuffIds();
        var config = plugin.getConfigManager();

        sender.sendMessage("\u00a78\u00a7m                          ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Status");
        sender.sendMessage("\u00a78\u00a7m                          ");

        // Active buffs
        if (buffIds.isEmpty()) {
            sender.sendMessage("\u00a7eActive Buff: \u00a77None");
        } else {
            for (int id : buffIds) {
                sender.sendMessage("\u00a7eActive Buff: \u00a7a" + buffModule.getBuffName(id)
                    + " \u00a77(" + id + ") \u2014 " + buffModule.getBuffDescription(id));
            }
        }

        // Per-day schedule
        sender.sendMessage("\u00a7eBuff Schedule:");
        for (DayOfWeek day : DayOfWeek.values()) {
            ConfigManager.DayConfig dc = config.getDayConfig(day);
            String modeStr = dc.mode.name().toLowerCase();
            String detail = switch (dc.mode) {
                case FIXED -> buffNames(dc.fixedBuffs);
                case RANDOM -> "random from [" + dc.randomPool + "] (pick " + config.getRandomCount() + ")";
                case OFF -> "\u00a77disabled";
            };
            sender.sendMessage("\u00a78  \u00a7b" + day.name().charAt(0) + day.name().substring(1).toLowerCase()
                + ": \u00a7f" + modeStr + " \u00a77\u2014 " + detail);
        }

        // End unlock
        long daysLeft = plugin.getProgressionLockModule().getDaysUntilEndUnlock();
        sender.sendMessage("\u00a7eEnd Unlock: \u00a7a" + daysLeft + " days remaining");

        // Border
        sender.sendMessage("\u00a7eWorld Border: \u00a7a" + config.getWorldBorderSize() + "x" + config.getWorldBorderSize()
            + " \u00a77at " + config.getWorldBorderCenterX() + "," + config.getWorldBorderCenterZ());

        // Migration
        sender.sendMessage("\u00a7eMigration Day: \u00a7a" + config.getMigrateDay() + " at " + config.getMigrateHour() + ":00");

        sender.sendMessage("\u00a78\u00a7m                          ");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("\u00a78\u00a7m                          ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Commands");
        sender.sendMessage("\u00a78\u00a7m                          ");
        sender.sendMessage("\u00a7e/nomad reload \u00a77\u2014 Reload config");
        sender.sendMessage("\u00a7e/nomad status \u00a77\u2014 Show all settings");
        sender.sendMessage("\u00a7e/nomad buffs \u00a77\u2014 List all 50 buffs");
        sender.sendMessage("\u00a7e/nomad setbuff <id...> \u00a77\u2014 Override today's buff");
        sender.sendMessage("\u00a7e/nomad setbuffpool <day> <mode> <ids> \u00a77\u2014 Set day schedule");
        sender.sendMessage("\u00a7e/nomad setborder <size> [cx] [cz] \u00a77\u2014 Set world border");
        sender.sendMessage("\u00a7e/nomad migratenow \u00a77\u2014 Trigger migration now");
        sender.sendMessage("\u00a7e/nomad sethome \u00a77\u2014 Set your home location");
        sender.sendMessage("\u00a78\u00a7m                          ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status", "buffs", "setbuff", "setbuffpool", "setborder", "migratenow", "sethome");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "setbuffpool" -> List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
                case "setborder" -> List.of("500", "1000", "2000", "5000");
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setbuffpool")) {
            return List.of("fixed", "random", "off");
        }
        return List.of();
    }
}
