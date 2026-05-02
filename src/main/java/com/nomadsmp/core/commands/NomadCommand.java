package com.nomadsmp.core.commands;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.modules.DailyBuffModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NomadCommand implements CommandExecutor {

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
                if (args.length < 2) {
                    sender.sendMessage("\u00a7cUsage: /nomad setbuff <id>");
                    return true;
                }
                try {
                    int buffId = Integer.parseInt(args[1]);
                    if (buffId < 1 || buffId > 50) {
                        sender.sendMessage("\u00a7cBuff ID must be 1-50.");
                        return true;
                    }
                    plugin.getDailyBuffModule().setCurrentBuffIds(List.of(buffId));
                    // Re-apply to all online players
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        plugin.getDailyBuffModule().removeAllBuffEffects(p);
                        plugin.getDailyBuffModule().applyToPlayer(p);
                    }
                    sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aBuff set to " + buffId + " (" + plugin.getDailyBuffModule().getBuffName(buffId) + ") for today.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("\u00a7cInvalid buff ID.");
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

    private void showStatus(CommandSender sender) {
        DailyBuffModule buffModule = plugin.getDailyBuffModule();
        List<Integer> buffIds = buffModule.getCurrentBuffIds();

        sender.sendMessage("\u00a78\u00a7m                          ");
        sender.sendMessage("\u00a76\u00a7l NomadSMP-Core Status");
        sender.sendMessage("\u00a78\u00a7m                          ");

        // Active buffs
        if (buffIds.isEmpty()) {
            sender.sendMessage("\u00a7eActive Buff: \u00a77None");
        } else {
            for (int id : buffIds) {
                sender.sendMessage("\u00a7eActive Buff: \u00a7a" + buffModule.getBuffName(id) + " \u00a77(" + id + ") \u2014 " + buffModule.getBuffDescription(id));
            }
        }

        // End unlock
        long daysLeft = plugin.getProgressionLockModule().getDaysUntilEndUnlock();
        sender.sendMessage("\u00a7eEnd Unlock: \u00a7a" + daysLeft + " days remaining");

        // Border size
        int size = plugin.getConfigManager().getBorderHalfSize() * 2;
        sender.sendMessage("\u00a7eWorld Border: \u00a7a" + size + "x" + size);

        // Next migration
        sender.sendMessage("\u00a7eMigration Day: \u00a7a" + plugin.getConfigManager().getMigrateDay() + " at " + plugin.getConfigManager().getMigrateHour() + ":00");

        sender.sendMessage("\u00a78\u00a7m                          ");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("\u00a7cUsage: /nomad <reload|status|migratenow|setbuff <id>|sethome>");
    }
}
