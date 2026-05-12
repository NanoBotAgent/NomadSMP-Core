package com.nomadsmp.core.commands;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.config.ConfigManager;
import com.nomadsmp.core.modules.DailyBuffModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class NomadCommand implements CommandExecutor, TabCompleter {

    private final NomadCore plugin;

    /** Maximum buff ID — update when new buffs are added. */
    private static final int MAX_BUFF_ID = DailyBuffModule.BUFF_NAMES.length - 1;

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
                String msg = "\u00a78[\u00a76NomadSMP\u00a78] \u00a7aConfig reloaded.";
                sender.sendMessage(msg);
                if (sender instanceof Player p) {
                    plugin.notifyOps(msg);
                }
            }
            case "status" -> showStatus(sender);
            case "stats" -> showStats(sender);
            case "migratenow" -> {
                if (!(sender instanceof Player) || sender.hasPermission("nomad.admin")) {
                    plugin.getNomadModule().runMigration();
                    plugin.getStatsManager().recordMigration();
                    String msg = "\u00a78[\u00a76NomadSMP\u00a78] \u00a7eMigration triggered!";
                    sender.sendMessage(msg);
                    plugin.notifyOps(msg);
                }
            }
            case "setbuff" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    // No args = clear all buffs
                    plugin.getDailyBuffModule().setCurrentBuffIds(List.of());
                    if (sender instanceof Player p) {
                        plugin.notifySelf(p, "\u00a78[\u00a76NomadSMP\u00a78] \u00a77You cleared all buffs. All players have been notified.");
                    }
                    return true;
                }
                List<Integer> ids = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    try {
                        int id = Integer.parseInt(args[i]);
                        if (id < 1 || id > MAX_BUFF_ID) {
                            sender.sendMessage("\u00a7cBuff ID must be 1-" + MAX_BUFF_ID + ". Got: " + id);
                            return true;
                        }
                        ids.add(id);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("\u00a7cInvalid buff ID: " + args[i]);
                        return true;
                    }
                }
                plugin.getDailyBuffModule().setCurrentBuffIds(ids);
                if (sender instanceof Player p) {
                    plugin.notifySelf(p, "\u00a78[\u00a76NomadSMP\u00a78] \u00a77You set the buff. All players have been notified.");
                }
            }
            case "setbuffpool" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 4) {
                    sender.sendMessage("\u00a7cUsage: /nomad setbuffpool <day> <mode> <id1,id2,...>");
                    sender.sendMessage("\u00a77Modes: fixed, random, off");
                    sender.sendMessage("\u00a77Days: monday-sunday");
                    return true;
                }
                String dayStr = args[1].toLowerCase();
                String modeStr = args[2].toLowerCase();
                String poolStr = args[3];

                DayOfWeek day;
                try { day = DayOfWeek.valueOf(dayStr.toUpperCase()); }
                catch (IllegalArgumentException e) { sender.sendMessage("\u00a7cInvalid day: " + dayStr); return true; }

                ConfigManager.BuffMode mode = switch (modeStr) {
                    case "fixed" -> ConfigManager.BuffMode.FIXED;
                    case "random" -> ConfigManager.BuffMode.RANDOM;
                    case "off" -> ConfigManager.BuffMode.OFF;
                    default -> { sender.sendMessage("\u00a7cInvalid mode. Use fixed, random, or off."); yield null; }
                };
                if (mode == null) return true;

                List<Integer> ids = new ArrayList<>();
                if (!poolStr.equalsIgnoreCase("none") && !poolStr.equalsIgnoreCase("empty")) {
                    for (String part : poolStr.split(",")) {
                        try {
                            int id = Integer.parseInt(part.trim());
                            if (id < 1 || id > MAX_BUFF_ID) { sender.sendMessage("\u00a7cBuff ID must be 1-" + MAX_BUFF_ID + ". Got: " + id); return true; }
                            ids.add(id);
                        } catch (NumberFormatException e) { sender.sendMessage("\u00a7cInvalid buff ID: " + part); return true; }
                    }
                }

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

                String msg = "\u00a78[\u00a76NomadSMP\u00a78] \u00a7a" + dayStr + " set to " + modeStr
                    + (ids.isEmpty() ? "" : " with [" + ids + "]") + ". Config saved.";
                plugin.notifyOps(msg);
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

                    plugin.getConfig().set("social.world-border.size", size);
                    plugin.getConfig().set("social.world-border.center-x", cx);
                    plugin.getConfig().set("social.world-border.center-z", cz);
                    plugin.saveConfig();

                    World world = Bukkit.getWorlds().getFirst();
                    WorldBorder border = world.getWorldBorder();
                    border.setCenter(cx, cz);
                    border.setSize(size);

                    String msg = "\u00a78[\u00a76NomadSMP\u00a78] \u00a7aWorld border set to " + size + "x" + size
                        + " centered at " + cx + ", " + cz;
                    if (sender instanceof Player p) plugin.notifySelf(p, msg);
                    else sender.sendMessage(msg);
                    plugin.notifyOps(msg);
                } catch (NumberFormatException e) {
                    sender.sendMessage("\u00a7cInvalid number format.");
                }
            }
            case "buffs" -> {
                if (sender instanceof Player player) {
                    openBuffsGUI(player);
                } else {
                    listBuffsChat(sender);
                }
            }
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

    // ─── GUI Menu for /nomad buffs ───

    private void openBuffsGUI(Player player) {
        DailyBuffModule bm = plugin.getDailyBuffModule();
        int totalBuffs = MAX_BUFF_ID;
        int guiSize = ((totalBuffs + 9) / 9) * 9 + 9; // Round up to next 9, plus 1 row for info
        guiSize = Math.min(guiSize, 54); // Max 54 slots
        Inventory gui = Bukkit.createInventory(null, guiSize, "\u00a76\u00a7lNomadSMP Buffs");

        for (int i = 1; i <= Math.min(totalBuffs, guiSize - 9); i++) {
            boolean isActive = bm.isBuffActive(i);
            boolean isEnabled = plugin.getConfigManager().isBuffEnabled(i);

            Material mat = isActive ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            if (!isEnabled) mat = Material.RED_STAINED_GLASS_PANE;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = isActive ? "\u00a7a\u2714 " : (!isEnabled ? "\u00a7c\u2718 " : "\u00a77 ");
                meta.setDisplayName(prefix + "\u00a7f" + bm.getBuffName(i));
                List<String> lore = new ArrayList<>();
                lore.add("\u00a77ID: " + i);
                lore.add("\u00a77" + bm.getBuffDescription(i));
                if (isActive) lore.add("\u00a7a\u25cf Currently Active");
                else if (!isEnabled) lore.add("\u00a7cDisabled in config");
                else lore.add("\u00a77Inactive today");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i - 1, item);
        }

        // Info item in last row
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("\u00a76\u00a7lBuff Info");
            List<String> lore = new ArrayList<>();
            var appliedAt = bm.getAppliedAt();
            if (appliedAt != null) {
                lore.add("\u00a77Applied at: " + appliedAt.toLocalTime().toString().substring(0, 5));
            }
            int dur = plugin.getConfigManager().getDurationHours();
            lore.add("\u00a77Duration: " + (dur == 0 ? "All day" : dur + "h"));
            lore.add("\u00a77Stacking: " + plugin.getConfigManager().getStacking().name().toLowerCase());
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(guiSize - 3, info);

        player.openInventory(gui);
    }

    // ─── Chat-based buff list (for console) ───

    private void listBuffsChat(CommandSender sender) {
        DailyBuffModule bm = plugin.getDailyBuffModule();
        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a76\u00a7l Buff Reference (1-" + MAX_BUFF_ID + ")");
        sender.sendMessage("\u00a78\u00a7m ");
        for (int i = 1; i < DailyBuffModule.BUFF_NAMES.length; i++) {
            String enabled = plugin.getConfigManager().isBuffEnabled(i) ? "\u00a7a\u2714 " : "\u00a7c\u2718 ";
            String active = bm.isBuffActive(i) ? "\u00a7a\u25cf " : "\u00a77 ";
            sender.sendMessage(active + enabled + "\u00a7e" + i + ". \u00a7f" + bm.getBuffName(i)
                + " \u00a77\u2014 " + bm.getBuffDescription(i));
        }
        sender.sendMessage("\u00a78\u00a7m ");
    }

    // ─── Stats ───

    private void showStats(CommandSender sender) {
        var stats = plugin.getStatsManager();
        long uptimeSec = stats.getUptimeSeconds();
        long hours = uptimeSec / 3600;
        long mins = (uptimeSec % 3600) / 60;
        long secs = uptimeSec % 60;

        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Stats");
        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a7eUptime: \u00a7a" + hours + "h " + mins + "m " + secs + "s");
        sender.sendMessage("\u00a7eTotal Player Joins: \u00a7a" + stats.getTotalJoins());
        sender.sendMessage("\u00a7eBuff Activations: \u00a7a" + stats.getBuffActivations());
        sender.sendMessage("\u00a7eMigrations Run: \u00a7a" + stats.getMigrationCount());
        sender.sendMessage("\u00a7eTimber Uses: \u00a7a" + stats.getTimberUses());
        sender.sendMessage("\u00a7eVein Miner Uses: \u00a7a" + stats.getVeinMinerUses());
        sender.sendMessage("\u00a78\u00a7m ");
    }

    // ─── Status ───

    private void showStatus(CommandSender sender) {
        DailyBuffModule bm = plugin.getDailyBuffModule();
        List<Integer> buffIds = bm.getCurrentBuffIds();
        var config = plugin.getConfigManager();

        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Status");
        sender.sendMessage("\u00a78\u00a7m ");

        sender.sendMessage("\u00a7eModules:");
        sender.sendMessage(" " + onOff(config.isNomadSystemEnabled()) + " \u00a7bNomad System");
        sender.sendMessage(" " + onOff(config.isDailyBuffsEnabled()) + " \u00a7bDaily Buffs"
            + " \u00a77(" + config.getStacking().name().toLowerCase() + ", " + (config.getDurationHours() == 0 ? "all day" : config.getDurationHours() + "h") + ")");
        sender.sendMessage(" " + onOff(config.isProgressionLockEnabled()) + " \u00a7bProgression Lock");
        sender.sendMessage(" " + onOff(config.isAntiCheatEnabled()) + " \u00a7bAnti-Cheat");
        sender.sendMessage(" " + onOff(config.isSocialEnabled()) + " \u00a7bSocial");

        if (buffIds.isEmpty()) {
            sender.sendMessage("\u00a7eActive Buff: \u00a77None");
        } else {
            for (int id : buffIds) {
                String enabled = config.isBuffEnabled(id) ? "\u00a7a" : "\u00a7c(disabled) ";
                sender.sendMessage("\u00a7eActive Buff: " + enabled + bm.getBuffName(id)
                    + " \u00a77(" + id + ") \u2014 " + bm.getBuffDescription(id));
            }
        }

        sender.sendMessage("\u00a7eBuff Schedule:");
        for (DayOfWeek day : DayOfWeek.values()) {
            ConfigManager.DayConfig dc = config.getDayConfig(day);
            String modeStr = dc.mode.name().toLowerCase();
            String detail = switch (dc.mode) {
                case FIXED -> buffNames(dc.fixedBuffs);
                case RANDOM -> "random from [" + dc.randomPool + "] (pick " + config.getRandomCount() + ")";
                case OFF -> "\u00a77disabled";
            };
            sender.sendMessage("\u00a78 \u00a7b" + day.name().charAt(0) + day.name().substring(1).toLowerCase()
                + ": \u00a7f" + modeStr + " \u00a77\u2014 " + detail);
        }

        sender.sendMessage("\u00a7eProgression Lock Features:");
        sender.sendMessage(" " + onOff(config.isEndLockEnabled()) + " \u00a7bEnd Lock"
            + " \u00a77(" + plugin.getProgressionLockModule().getDaysUntilEndUnlock() + " days left)");
        sender.sendMessage(" " + onOff(config.isNetheriteCraftingBanEnabled()) + " \u00a7bNetherite Crafting Ban");
        sender.sendMessage(" " + onOff(config.isNetheriteEquipPunishEnabled()) + " \u00a7bNetherite Equip Punish");
        sender.sendMessage(" " + onOff(config.isBlockGamemodeCommandEnabled()) + " \u00a7bBlock Gamemode Command");
        sender.sendMessage(" " + onOff(config.isBlockGiveCommandEnabled()) + " \u00a7bBlock Give Command");

        sender.sendMessage("\u00a7eAnti-Cheat Features:");
        sender.sendMessage(" " + onOff(config.isBlockSeedCommandEnabled()) + " \u00a7bBlock Seed Command");
        sender.sendMessage(" " + onOff(config.isScrambleStructureSeedsEnabled()) + " \u00a7bScramble Structure Seeds");
        sender.sendMessage(" " + onOff(config.isOreObfuscationEnabled()) + " \u00a7bOre Obfuscation");

        sender.sendMessage("\u00a7eSocial Features:");
        sender.sendMessage(" " + onOff(config.isWorldBorderEnabled()) + " \u00a7bWorld Border"
            + " \u00a77(" + config.getWorldBorderSize() + "x" + config.getWorldBorderSize()
            + " at " + config.getWorldBorderCenterX() + "," + config.getWorldBorderCenterZ() + ")");
        sender.sendMessage(" " + onOff(config.isDisableTpaEnabled()) + " \u00a7bDisable TPA");
        sender.sendMessage(" " + onOff(config.isDisableWarpEnabled()) + " \u00a7bDisable Warp");
        sender.sendMessage(" " + onOff(config.isDisableHomeCommandEnabled()) + " \u00a7bDisable Home");
        sender.sendMessage(" " + onOff(config.isPlayerHeadDropEnabled()) + " \u00a7bPlayer Head Drop");
        sender.sendMessage(" " + onOff(config.isNoAdminOpEnabled()) + " \u00a7bNo Admin OP");

        sender.sendMessage("\u00a7eMigration Day: \u00a7a" + config.getMigrateDay() + " at " + config.getMigrateHour() + ":00");

        sender.sendMessage("\u00a78\u00a7m ");
    }

    private boolean checkAdmin(CommandSender sender) {
        if (!sender.hasPermission("nomad.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        return true;
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

    private String onOff(boolean value) {
        return value ? "\u00a7a\u2714" : "\u00a7c\u2718";
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Commands");
        sender.sendMessage("\u00a78\u00a7m ");
        sender.sendMessage("\u00a7e/nomad reload \u00a77\u2014 Reload config");
        sender.sendMessage("\u00a7e/nomad status \u00a77\u2014 Show all settings");
        sender.sendMessage("\u00a7e/nomad stats \u00a77\u2014 Show server stats");
        sender.sendMessage("\u00a7e/nomad buffs \u00a77\u2014 Open buff menu (GUI for players)");
        sender.sendMessage("\u00a7e/nomad setbuff [id...] \u00a77\u2014 Override today's buff (no args = clear)");
        sender.sendMessage("\u00a7e/nomad setbuffpool <day> <mode> <ids> \u00a77\u2014 Set day schedule");
        sender.sendMessage("\u00a7e/nomad setborder <size> [cx] [cz] \u00a77\u2014 Set world border");
        sender.sendMessage("\u00a7e/nomad migratenow \u00a77\u2014 Trigger migration now");
        sender.sendMessage("\u00a7e/nomad sethome \u00a77\u2014 Set your home location");
        sender.sendMessage("\u00a78\u00a7m ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status", "stats", "buffs", "setbuff", "setbuffpool", "setborder", "migratenow", "sethome");
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
