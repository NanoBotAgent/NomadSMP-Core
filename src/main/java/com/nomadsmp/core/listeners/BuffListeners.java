package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.modules.DailyBuffModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class BuffListeners implements Listener {
    private final NomadCore plugin;
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> doubleJumpUsed = new HashMap<>();

    public BuffListeners(NomadCore plugin) { this.plugin = plugin; }

    // Buff 1: Titanium
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (plugin.buffs().isBuffActive(1)) e.setCancelled(true);
    }

    // Buff 8: Looter + Buff 19: Vampire + Buff 32: Rich + Buff 39: Thor + Buff 16: Trophy Hunter
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (plugin.buffs().isBuffActive(8)) {
            List<ItemStack> extra = new ArrayList<>();
            for (ItemStack drop : e.getDrops()) extra.add(drop.clone());
            e.getDrops().addAll(extra);
        }
        if (plugin.buffs().isBuffActive(19)) {
            double max = killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            killer.setHealth(Math.min(killer.getHealth() + 1, max));
        }
        if (plugin.buffs().isBuffActive(32) && Math.random() < 0.1) e.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
        if (plugin.buffs().isBuffActive(39) && Math.random() < 0.05) e.getEntity().getWorld().strikeLightning(e.getEntity().getLocation());
        if (plugin.buffs().isBuffActive(16)) {
            Material headType = switch (e.getEntity().getType()) {
                case ZOMBIE -> Material.ZOMBIE_HEAD;
                case SKELETON -> Material.SKELETON_SKULL;
                case CREEPER -> Material.CREEPER_HEAD;
                default -> null;
            };
            if (headType != null) e.getDrops().add(new ItemStack(headType));
        }
    }

    // Buff 12: Chef
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && plugin.buffs().isBuffActive(12)) {
            if (e.getFoodLevel() < p.getFoodLevel()) e.setCancelled(true);
        }
    }

    // Buff 20: XP Junkie
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent e) {
        if (plugin.buffs().isBuffActive(20)) e.setAmount(e.getAmount() * 2);
    }

    // Buff 23: Glass Cannon (offensive)
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && plugin.buffs().isBuffActive(23)) e.setDamage(e.getDamage() * 2);
    }

    // Buff 23/38/42/28: defensive
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (plugin.buffs().isBuffActive(23)) e.setDamage(e.getDamage() * 2);
        if (plugin.buffs().isBuffActive(38) && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            p.setVelocity(new Vector(0, p.getFallDistance() * 0.05, 0));
        }
        if (plugin.buffs().isBuffActive(42) && e.getCause() == EntityDamageEvent.DamageCause.WITHER) e.setCancelled(true);
        if (plugin.buffs().isBuffActive(28) && e.getCause() == EntityDamageEvent.DamageCause.FALL && p.getFallDistance() < 5) e.setCancelled(true);
    }

    // Buff 40: Teleporter
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!plugin.buffs().isBuffActive(40) || !e.isSneaking()) return;
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();
        if (teleportCooldowns.containsKey(uuid) && now - teleportCooldowns.get(uuid) < 10000) return;
        teleportCooldowns.put(uuid, now);
        Location target = p.getLocation().add(p.getLocation().getDirection().multiply(5));
        if (target.getBlock().getType().isSolid()) return;
        p.teleport(target);
    }

    // Buff 47: Builder
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (plugin.buffs().isBuffActive(47) && Math.random() < 0.2) e.getItemInHand().setAmount(e.getItemInHand().getAmount() + 1);
    }

    // Buff 48: Double Jump
    @EventHandler
    public void onFlightToggle(PlayerToggleFlightEvent e) {
        if (!plugin.buffs().isBuffActive(48)) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        e.setCancelled(true);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setVelocity(p.getVelocity().setY(0.8));
        doubleJumpUsed.put(p.getUniqueId(), true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (plugin.buffs().isBuffActive(48) && p.isOnGround()) {
            doubleJumpUsed.remove(p.getUniqueId());
            p.setAllowFlight(true);
        }
    }

    // Buff 14: Timber + Buff 15: Vein Miner + Buff 33: Scavenger
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (plugin.buffs().isBuffActive(14) && isLog(b.getType())) breakConnected(b, b.getType(), 50);
        if (plugin.buffs().isBuffActive(15) && isOre(b.getType())) breakConnected(b, b.getType(), 32);
        if (plugin.buffs().isBuffActive(33) && (b.getType() == Material.SHORT_GRASS)) {
            if (Math.random() < 0.05) {
                Material[] loot = {Material.STRING, Material.WHEAT_SEEDS, Material.DANDELION, Material.POPPY};
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(loot[(int)(Math.random() * loot.length)]));
            }
        }
    }

    private void breakConnected(Block start, Material type, int max) {
        Set<Location> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start.getLocation());
        int count = 0;
        while (!queue.isEmpty() && count < max) {
            Block current = queue.poll();
            if (current.getType() != type) continue;
            current.breakNaturally();
            count++;
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (!visited.contains(neighbor.getLocation()) && neighbor.getType() == type) {
                            visited.add(neighbor.getLocation());
                            queue.add(neighbor);
                        }
                    }
        }
    }

    private boolean isLog(Material m) { return m.name().contains("_LOG") || m.name().contains("_STEM") || m.name().contains("_WOOD") || m.name().contains("_HYPHAE"); }
    private boolean isOre(Material m) { return m.name().contains("_ORE") || m.name().equals("ANCIENT_DEBRIS"); }

    // Buff 49: Whale
    @EventHandler
    public void onAirChange(EntityAirChangeEvent e) {
        if (e.getEntity() instanceof Player && plugin.buffs().isBuffActive(49)) e.setAmount(Integer.MAX_VALUE);
    }

    // Buff 50: Pacifist
    @EventHandler
    public void onPacifistAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !plugin.buffs().isBuffActive(50)) return;
        if (p.getInventory().getItemInMainHand().getType().name().contains("SWORD")) e.setDamage(0);
    }

    // Join/Quit
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.buffs().applyBuffs(p, plugin.buffs().getCurrentBuffIds());
        if (plugin.cfg().isBroadcastOnJoin() && !plugin.buffs().getCurrentBuffIds().isEmpty()) {
            int id = plugin.buffs().getCurrentBuffIds().getFirst();
            p.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eToday's buff: \u00a7a" + DailyBuffModule.getBuffName(id) + " \u00a77\u2014 " + DailyBuffModule.getBuffDesc(id));
        }
        if (plugin.buffs().isBuffActive(48)) p.setAllowFlight(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        teleportCooldowns.remove(e.getPlayer().getUniqueId());
        doubleJumpUsed.remove(e.getPlayer().getUniqueId());
    }
}
