package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class BuffListeners implements Listener {

    private final NomadCore plugin;
    private final Map<UUID, Long> teleporterCooldowns = new HashMap<>();
    private final Set<UUID> hasDoubleJumped = new HashSet<>();
    private final Map<UUID, Long> pearlTimeMap = new HashMap<>();
    private final Set<UUID> inertiaPlayers = new HashSet<>();
    private int magnetTaskId = -1;
    private int gravityWellTaskId = -1;
    private int inertiaTaskId = -1;

    public BuffListeners(NomadCore plugin) {
        this.plugin = plugin;
        magnetTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::magnetTick, 4L, 4L).getTaskId();
        gravityWellTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::gravityWellTick, 10L, 10L).getTaskId();
        // Inertia (buff 44): continuously zero knockback velocity for recently-hit players
        inertiaTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::inertiaTick, 1L, 1L).getTaskId();
    }

    private boolean isActive(int id) { return plugin.getDailyBuffModule().isBuffActive(id); }

    // Buff 1: Titanium — no durability loss
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isActive(1)) event.setCancelled(true);
    }

    // Buff 8: Looter, 19: Vampire, 32: Rich
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (isActive(8)) {
            List<ItemStack> extra = new ArrayList<>();
            for (ItemStack drop : event.getDrops()) extra.add(drop.clone());
            event.getDrops().addAll(extra);
        }

        if (isActive(19)) {
            double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
            killer.setHealth(Math.min(killer.getHealth() + 1.0, maxHealth));
        }

        if (isActive(32) && Math.random() < 0.1) {
            event.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
        }
    }

    // Buff 9: Bountiful Harvest
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!isActive(9)) return;
        Block block = event.getBlock();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { block.applyBoneMeal(BlockFace.UP); } catch (Exception ignored) {}
        }, 1L);
    }

    // Buff 12: Chef
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (isActive(12) && event.getFoodLevel() < event.getEntity().getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // Buff 13: Blacksmith
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!isActive(13)) return;
        Map<Material, Material> smelting = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT
        );
        for (Item item : event.getItems()) {
            Material smelted = smelting.get(item.getItemStack().getType());
            if (smelted != null) item.getItemStack().setType(smelted);
        }
    }

    // Buff 14: Timber, Buff 15: Vein Miner
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isActive(14) && Tag.LOGS.isTagged(block.getType())) {
            bfsBreak(block, block.getType(), 50, event.getPlayer());
        }
        if (isActive(15) && block.getType().name().endsWith("_ORE")) {
            bfsBreak(block, block.getType(), 32, event.getPlayer());
        }
    }

    private void bfsBreak(Block start, Material target, int maxBlocks, Player player) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                Block neighbor = current.getRelative(face);
                if (!visited.contains(neighbor) && neighbor.getType() == target) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        for (Block b : visited) {
            if (!b.equals(start)) b.breakNaturally(player.getInventory().getItemInMainHand());
        }
    }

    // Buff 20: XP Junkie
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (isActive(20)) event.setAmount(event.getAmount() * 2);
    }

    // Buff 23: Glass Cannon (damage dealt), 39: Thor, 50: Pacifist
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (isActive(23) && event.getDamager() instanceof Player) {
            event.setDamage(event.getDamage() * 2);
        }
        if (isActive(39) && event.getDamager() instanceof Player && Math.random() < 0.05) {
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
        }
        if (isActive(50) && event.getDamager() instanceof Player player) {
            if (player.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD")) {
                event.setDamage(0);
            }
        }
    }

    // Buff 23 (damage taken), 28 (ender pearl fall), 36 (fire immune), 38 (slimy bounce), 42 (wither immune), 44 (inertia)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isActive(23)) event.setDamage(event.getDamage() * 2);

        if (isActive(28) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long t = pearlTimeMap.get(player.getUniqueId());
            if (t != null && System.currentTimeMillis() - t < 1000) {
                event.setCancelled(true);
                pearlTimeMap.remove(player.getUniqueId());
            }
        }

        if (isActive(36) && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }

        if (isActive(38) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            player.setVelocity(player.getVelocity().setY(player.getFallDistance() * 0.05));
        }

        if (isActive(42) && event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }

        // Buff 44: Inertia — mark player so inertiaTick cancels their knockback velocity
        if (isActive(44)) {
            inertiaPlayers.add(player.getUniqueId());
        }
    }

    // Buff 25: Librarian
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (isActive(25)) event.setExpLevelCost(1);
    }

    // Buff 27: Spider (wall climb), 36: Snowman trail, 48: Double Jump reset
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isActive(27) && player.isSneaking()) {
            Block beside = player.getLocation().getBlock().getRelative(player.getFacing());
            if (beside.getType().isSolid()) {
                player.setVelocity(player.getVelocity().setY(0.3));
            }
        }
        if (isActive(36)) {
            Block prev = event.getFrom().getBlock();
            if (prev.getType() == Material.AIR && prev.getRelative(BlockFace.DOWN).getType().isSolid()) {
                prev.setType(Material.SNOW);
            }
        }
        if (isActive(48) && player.isOnGround()) {
            hasDoubleJumped.remove(player.getUniqueId());
        }
    }

    // Buff 33: Scavenger
    @EventHandler
    public void onScavengerBreak(BlockBreakEvent event) {
        if (!isActive(33)) return;
        Material type = event.getBlock().getType();
        if (type == Material.GRASS_BLOCK || type == Material.SHORT_GRASS) {
            if (Math.random() < 0.05) {
                Material[] loot = {Material.STRING, Material.WHEAT_SEEDS, Material.POPPY, Material.DANDELION, Material.FEATHER};
                event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation(),
                    new ItemStack(loot[(int)(Math.random() * loot.length)])
                );
            }
        }
    }

    // Buff 35: Gardener — 3x3 bone meal
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isActive(35) || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;
        Block target = event.getClickedBlock();
        if (target == null) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block relative = target.getRelative(dx, 0, dz);
                try { relative.applyBoneMeal(BlockFace.UP); } catch (Exception ignored) {}
            }
        }
    }

    // Buff 40: Teleporter
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!isActive(40) || !event.isSneaking()) return;
        Player player = event.getPlayer();
        Long lastUse = teleporterCooldowns.get(player.getUniqueId());
        if (lastUse != null && System.currentTimeMillis() - lastUse < 10000) return;
        Location target = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));
        player.teleport(target);
        teleporterCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // Buff 46: Alchemist
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!isActive(46) || event.getItem().getType() != Material.POTION) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
                event.getPlayer().addPotionEffect(new PotionEffect(
                    effect.getType(), effect.getDuration() * 3, effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles()
                ));
            }
        }, 1L);
    }

    // Buff 47: Builder
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (isActive(47) && Math.random() < 0.2) {
            ItemStack hand = event.getItemInHand();
            hand.setAmount(hand.getAmount() + 1);
        }
    }

    // Buff 48: Double Jump
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!isActive(48) || event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (hasDoubleJumped.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        event.getPlayer().setAllowFlight(false);
        event.getPlayer().setVelocity(event.getPlayer().getVelocity().setY(0.8));
        hasDoubleJumped.add(event.getPlayer().getUniqueId());
    }

    // Buff 49: Whale
    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (isActive(49) && event.getEntity() instanceof Player) {
            event.setAmount(Integer.MAX_VALUE);
        }
    }

    // Ender pearl tracking (buff 28)
    @EventHandler
    public void onPearlThrow(PlayerTeleportEvent event) {
        if (isActive(28) && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            pearlTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    // Player join — apply buffs
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDailyBuffModule().applyToPlayer(event.getPlayer());
        if (plugin.getConfigManager().isBroadcastOnJoin()) {
            var buffIds = plugin.getDailyBuffModule().getCurrentBuffIds();
            if (!buffIds.isEmpty()) {
                StringBuilder msg = new StringBuilder("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eToday's buff: ");
                for (int id : buffIds) {
                    msg.append("\u00a7a").append(plugin.getDailyBuffModule().getBuffName(id));
                    msg.append(" \u00a77\u2014 ").append(plugin.getDailyBuffModule().getBuffDescription(id));
                }
                event.getPlayer().sendMessage(msg.toString());
            }
        }
    }

    // Player quit — cleanup
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        teleporterCooldowns.remove(uuid);
        hasDoubleJumped.remove(uuid);
        pearlTimeMap.remove(uuid);
        inertiaPlayers.remove(uuid);
    }

    /**
     * Inertia tick (buff 44): Cancel knockback velocity for recently-hit players.
     * Since EntityKnockbackEvent doesn't exist in Paper 26.1.2, we detect
     * high horizontal velocity after damage and zero it out.
     */
    private void inertiaTick() {
        if (!isActive(44)) {
            inertiaPlayers.clear();
            return;
        }
        for (UUID uuid : Set.copyOf(inertiaPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Vector vel = player.getVelocity();
                // Knockback typically gives horizontal velocity > 0.3
                if (Math.abs(vel.getX()) > 0.3 || Math.abs(vel.getZ()) > 0.3) {
                    // Zero horizontal knockback, keep vertical (gravity/jumping)
                    player.setVelocity(new Vector(0, vel.getY(), 0));
                    inertiaPlayers.remove(uuid);
                }
            } else {
                inertiaPlayers.remove(uuid);
            }
        }
    }

    // Magnet tick (buff 11)
    private void magnetTick() {
        if (!isActive(11)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Item item) {
                    item.setVelocity(player.getLocation().toVector()
                        .subtract(item.getLocation().toVector())
                        .normalize().multiply(0.5));
                }
            }
        }
    }

    // Gravity Well tick (buff 45)
    private void gravityWellTick() {
        if (!isActive(45)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Monster monster && monster.getTarget() == player) {
                    monster.setVelocity(player.getLocation().toVector()
                        .subtract(monster.getLocation().toVector())
                        .normalize().multiply(0.3));
                }
            }
        }
    }
}
