package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.config.ConfigManager;
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
        inertiaTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::inertiaTick, 1L, 1L).getTaskId();
    }

    private boolean active(int id) {
        return plugin.getDailyBuffModule().isBuffActive(id) && plugin.getConfigManager().isBuffEnabled(id);
    }

    private ConfigManager cfg() { return plugin.getConfigManager(); }

    // 1: Titanium
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (active(1)) event.setCancelled(true);
    }

    // 8: Looter, 19: Vampire, 32: Rich
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (active(8)) {
            int mult = cfg().getLooterMultiplier();
            List<ItemStack> original = new ArrayList<>(event.getDrops());
            for (int i = 1; i < mult; i++) {
                for (ItemStack drop : original) event.getDrops().add(drop.clone());
            }
        }

        if (active(19)) {
            double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
            killer.setHealth(Math.min(killer.getHealth() + cfg().getVampireHeal(), maxHealth));
        }

        if (active(32) && Math.random() < cfg().getRichNuggetChance()) {
            event.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
        }
    }

    // 9: Bountiful Harvest
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (active(9)) {
            Block block = event.getBlock();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { block.applyBoneMeal(BlockFace.UP); } catch (Exception ignored) {}
            }, 1L);
        }
    }

    // 12: Chef
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (active(12) && event.getFoodLevel() < event.getEntity().getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // 13: Blacksmith
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!active(13)) return;
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

    // 14: Timber, 15: Vein Miner
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (active(14) && Tag.LOGS.isTagged(block.getType())) {
            bfsBreak(block, block.getType(), cfg().getTimberMax(), event.getPlayer());
            plugin.getStatsManager().recordTimberUse();
        }
        if (active(15) && block.getType().name().endsWith("_ORE")) {
            bfsBreak(block, block.getType(), cfg().getVeinMinerMax(), event.getPlayer());
            plugin.getStatsManager().recordVeinMinerUse();
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

    // 33: Scavenger
    @EventHandler
    public void onScavengerBreak(BlockBreakEvent event) {
        if (!active(33)) return;
        Material type = event.getBlock().getType();
        if (type == Material.GRASS_BLOCK || type == Material.SHORT_GRASS) {
            if (Math.random() < cfg().getScavengerChance()) {
                List<Material> loot = cfg().getScavengerLoot();
                if (loot.isEmpty()) loot = List.of(Material.STRING, Material.WHEAT_SEEDS, Material.POPPY);
                event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation(),
                    new ItemStack(loot.get((int)(Math.random() * loot.size())))
                );
            }
        }
    }

    // 20: XP Junkie
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (active(20)) event.setAmount(event.getAmount() * cfg().getXpMultiplier());
    }

    // 23: Glass Cannon, 39: Thor, 50: Pacifist
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (active(23) && event.getDamager() instanceof Player) {
            event.setDamage(event.getDamage() * cfg().getGlassCannonDealt());
        }
        if (active(39) && event.getDamager() instanceof Player && Math.random() < cfg().getThorChance()) {
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
        }
        if (active(50) && event.getDamager() instanceof Player player) {
            if (player.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD")) {
                event.setDamage(0);
            }
        }
    }

    // 23 (taken), 28, 36, 38, 42, 44
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (active(23)) event.setDamage(event.getDamage() * cfg().getGlassCannonTaken());

        if (active(28) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long t = pearlTimeMap.get(player.getUniqueId());
            if (t != null && System.currentTimeMillis() - t < cfg().getEnderPearlWindowMs()) {
                event.setCancelled(true);
                pearlTimeMap.remove(player.getUniqueId());
            }
        }

        if (active(36) && cfg().buffBool(36, "fire-immune", true)
            && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
            || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
            || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }

        if (active(38) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            player.setVelocity(player.getVelocity().setY(player.getFallDistance() * cfg().getSlimyBounceMultiplier()));
        }

        if (active(42) && event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }

        if (active(44)) inertiaPlayers.add(player.getUniqueId());
    }

    // 25: Librarian
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (active(25)) event.setExpLevelCost(cfg().getLibrarianCost());
    }

    // 27: Spider, 36: Snowman trail, 48: Double Jump reset
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (active(27) && player.isSneaking()) {
            Block beside = player.getLocation().getBlock().getRelative(player.getFacing());
            if (beside.getType().isSolid()) {
                player.setVelocity(player.getVelocity().setY(cfg().getSpiderClimbVelocity()));
            }
        }
        if (active(36) && cfg().buffBool(36, "snow-trail", true)) {
            Block prev = event.getFrom().getBlock();
            if (prev.getType() == Material.AIR && prev.getRelative(BlockFace.DOWN).getType().isSolid()) {
                prev.setType(Material.SNOW);
            }
        }
        if (active(48) && player.isOnGround()) {
            hasDoubleJumped.remove(player.getUniqueId());
        }
    }

    // 35: Gardener
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!active(35) || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;
        Block target = event.getClickedBlock();
        if (target == null) return;
        int r = cfg().getGardenerRadius();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                try { target.getRelative(dx, 0, dz).applyBoneMeal(BlockFace.UP); } catch (Exception ignored) {}
            }
        }
    }

    // 40: Teleporter — with cooldown message
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!active(40) || !event.isSneaking()) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long lastUse = teleporterCooldowns.get(player.getUniqueId());
        long cooldownMs = cfg().getTeleporterCooldownMs();

        if (lastUse != null && now - lastUse < cooldownMs) {
            long remainingMs = cooldownMs - (now - lastUse);
            double remainingSec = remainingMs / 1000.0;
            player.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eTeleporter on cooldown: "
                + String.format("%.1f", remainingSec) + "s remaining.");
            return;
        }

        Location target = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(cfg().getTeleporterDistance()));
        player.teleport(target);
        teleporterCooldowns.put(player.getUniqueId(), now);
    }

    // 46: Alchemist
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!active(46) || event.getItem().getType() != Material.POTION) return;
        int mult = cfg().getAlchemistMultiplier();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
                event.getPlayer().addPotionEffect(new PotionEffect(
                    effect.getType(), effect.getDuration() * mult, effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles()
                ));
            }
        }, 1L);
    }

    // 47: Builder
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (active(47) && Math.random() < cfg().getBuilderRefundChance()) {
            ItemStack hand = event.getItemInHand();
            hand.setAmount(hand.getAmount() + 1);
        }
    }

    // 48: Double Jump
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!active(48) || event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (hasDoubleJumped.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        event.getPlayer().setAllowFlight(false);
        event.getPlayer().setVelocity(event.getPlayer().getVelocity().setY(cfg().getDoubleJumpVelocity()));
        hasDoubleJumped.add(event.getPlayer().getUniqueId());
    }

    // 49: Whale
    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (active(49) && cfg().buffBool(49, "infinite-oxygen", true) && event.getEntity() instanceof Player) {
            event.setAmount(Integer.MAX_VALUE);
        }
    }

    // 28: Ender pearl tracking
    @EventHandler
    public void onPearlThrow(PlayerTeleportEvent event) {
        if (active(28) && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            pearlTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getStatsManager().recordJoin();
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        teleporterCooldowns.remove(uuid);
        hasDoubleJumped.remove(uuid);
        pearlTimeMap.remove(uuid);
        inertiaPlayers.remove(uuid);
    }

    private void inertiaTick() {
        if (!active(44)) { inertiaPlayers.clear(); return; }
        double threshold = cfg().getInertiaThreshold();
        for (UUID uuid : Set.copyOf(inertiaPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Vector vel = player.getVelocity();
                if (Math.abs(vel.getX()) > threshold || Math.abs(vel.getZ()) > threshold) {
                    player.setVelocity(new Vector(0, vel.getY(), 0));
                    inertiaPlayers.remove(uuid);
                }
            } else {
                inertiaPlayers.remove(uuid);
            }
        }
    }

    private void magnetTick() {
        if (!active(11)) return;
        int range = cfg().getMagnetRange();
        double strength = cfg().getMagnetStrength();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof Item item) {
                    item.setVelocity(player.getLocation().toVector()
                        .subtract(item.getLocation().toVector())
                        .normalize().multiply(strength));
                }
            }
        }
    }

    private void gravityWellTick() {
        if (!active(45)) return;
        int range = cfg().getGravityWellRange();
        double strength = cfg().getGravityWellStrength();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof Monster monster && monster.getTarget() == player) {
                    monster.setVelocity(player.getLocation().toVector()
                        .subtract(monster.getLocation().toVector())
                        .normalize().multiply(strength));
                }
            }
        }
    }
}