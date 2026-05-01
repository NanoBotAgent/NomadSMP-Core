package com.nomadsmp.core.commands;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.modules.DailyBuffModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class NomadCommand implements CommandExecutor {
    private final NomadCore plugin;
    public NomadCommand(NomadCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("nomad.admin")) { sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cNo permission."); return true; }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> { plugin.cfg().reload(); sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aConfig reloaded."); }
            case "status" -> {
                List<Integer> buffs = plugin.buffs().getCurrentBuffIds();
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eActive buffs: \u00a7a" + String.join(", ", buffs.stream().map(DailyBuffModule::getBuffName).toList()));
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eEnd unlock in: \u00a7a" + plugin.prog().daysUntilEndUnlock() + " days");
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eBorder: \u00a7a" + (plugin.cfg().getBorderHalfSize() * 2) + "x" + (plugin.cfg().getBorderHalfSize() * 2));
                sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eNext migration: \u00a7a" + plugin.cfg().getMigrateDay() + " at " + plugin.cfg().getMigrateHour() + ":00");
            }
            case "migratenow" -> { plugin.nomad().runMigration(); sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aMigration triggered!"); }
            case "setbuff" -> {
                if (args.length < 2) { sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cUsage: /nomad setbuff <id>"); return true; }
                try {
                    int id = Integer.parseInt(args[1]);
                    plugin.buffs().setCurrentBuffIds(List.of(id));
                    for (Player p : plugin.getServer().getOnlinePlayers()) { plugin.buffs().removeAllBuffEffects(p); plugin.buffs().applyBuffs(p, List.of(id)); }
                    sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aBuff set to " + DailyBuffModule.getBuffName(id));
                } catch (NumberFormatException ex) { sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cInvalid buff ID."); }
            }
            case "sethome" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cOnly players can set homes."); return true; }
                plugin.homes().setHome(p.getUniqueId(), p.getLocation());
                p.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7aHome set! It will migrate with you each week.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eCommands:");
        s.sendMessage("\u00a77  /nomad reload\u00a7- Reload config");
        s.sendMessage("\u00a77  /nomad status\u00a7- View plugin status");
        s.sendMessage("\u00a77  /nomad migratenow\u00a7- Force migration");
        s.sendMessage("\u00a77  /nomad setbuff <id>\u00a7- Override today's buff");
        s.sendMessage("\u00a77  /nomad sethome\u00a7- Set your nomad home");
    }
}
