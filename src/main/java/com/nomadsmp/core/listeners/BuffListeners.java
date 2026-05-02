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

    private boolean isActive(int id) { return plugin.getDailyBuffModule().isBuffActive(id); }
    private ConfigManager cfg() { return plugin.getConfigManager(); }

    // Buff 1: Titanium
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isActive(1) && cfg().getBuffBool("titanium-durability-cancel", true)) event.setCancelled(true);
    }

    // Buff 8: Looter, 19: Vampire, 32: Rich
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (isActive(8)) {
            int mult = cfg().getLooterMultiplier();
            List<ItemStack> original = new ArrayList<>(event.getDrops());
            for (int i = 1; i < mult; i++) {
                for (ItemStack drop : original) event.getDrops().add(drop.clone());
            }
        }

        if (isActive(19)) {
            double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
            killer.setHealth(Math.min(killer.getHealth() + cfg().getVampireHeal(), maxHealth));
        }

        if (isActive(32) && Math.random() < cfg().getRichNuggetChance()) {
            event.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
        }
    }

    // Buff 9: Bountiful Harvest
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (isActive(9) && cfg().getBuffBool("bountiful-growth-boost", true)) {
            Block block = event.getBlock();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { block.applyBoneMeal(BlockFace.UP); } catch (Exception ignored) {}
            }, 1L);
        }
    }

    // Buff 12: Chef
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (isActive(12) && cfg().getBuffBool("chef-cancel-hunger", true)
                && event.getFoodLevel() < event.getEntity().getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // Buff 13: Blacksmith
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!isActive(13) || !cfg().getBuffBool("blacksmith-auto-smelt", true)) return;
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
            bfsBreak(block, block.getType(), cfg().getTimberMax(), event.getPlayer());
        }
        if (isActive(15) && block.getType().name().endsWith("_ORE")) {
            bfsBreak(block, block.getType(), cfg().getVeinMinerMax(), event.getPlayer());
        }
    }

    private void bfsBreak(Block start, Material target, int maxBlocks, Player player) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(start); visited.add(start);
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                Block neighbor = current.getRelative(face);
                if (!visited.contains(neighbor) && neighbor.getType() == target) {
                    visited.add(neighbor); queue.add(neighbor);
                }
            }
        }
        for (Block b : visited) {
            if (!b.equals(start)) b.breakNaturally(player.getInventory().getItemInMainHand());
        }
    }

    // Buff 33: Scavenger
    @EventHandler(priority = EventPriority.NORMAL)
    public void onScavengerBreak(BlockBreakEvent event) {
        if (!isActive(33)) return;
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

    // Buff 20: XP Junkie
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (isActive(20)) event.setAmount(event.getAmount() * cfg().getXpMultiplier());
    }

    // Buff 23: Glass Cannon, 39: Thor, 50: Pacifist
    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (isActive(23) && event.getDamager() instanceof Player) {
            event.setDamage(event.getDamage() * cfg().getGlassCannonDealt());
        }
        if (isActive(39) && event.getDamager() instanceof Player && Math.random() < cfg().getThorChance()) {
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
        }
        if (isActive(50) && event.getDamager() instanceof Player player) {
            if (player.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD")) {
                event.setDamage(0);
            }
        }
    }

    // Buff 23 (taken), 28, 36, 38, 42, 44
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isActive(23)) event.setDamage(event.getDamage() * cfg().getGlassCannonTaken());

        if (isActive(28) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long t = pearlTimeMap.get(player.getUniqueId());
            if (t != null && System.currentTimeMillis() - t < cfg().getEnderPearlWindowMs()) {
                event.setCancelled(true);
                pearlTimeMap.remove(player.getUniqueId());
            }
        }

        if (isActive(36) && cfg().getBuffBool("snowman-snow-trail", true)
                && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }

        if (isActive(38) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            player.setVelocity(player.getVelocity().setY(player.getFallDistance() * cfg().getSlimyBounceMultiplier()));
        }

        if (isActive(42) && event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }

        if (isActive(44)) inertiaPlayers.add(player.getUniqueId());
    }

    // Buff 25: Librarian
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (isActive(25)) event.setExpLevelCost(cfg().getLibrarianCost());
    }

    // Buff 27: Spider, 36: Snowman trail, 48: Double Jump reset
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isActive(27) && player.isSneaking()) {
            Block beside = player.getLocation().getBlock().getRelative(player.getFacing());
            if (beside.getType().isSolid()) {
                player.setVelocity(player.getVelocity().setY(cfg().getSpiderClimbVelocity()));
            }
        }
        if (isActive(36) && cfg().getBuffBool("snowman-snow-trail", true)) {
            Block prev = event.getFrom().getBlock();
            if (prev.getType() == Material.AIR && prev.getRelative(BlockFace.DOWN).getType().isSolid()) {
                prev.setType(Material.SNOW);
            }
        }
        if (isActive(48) && player.isOnGround()) {
            hasDoubleJumped.remove(player.getUniqueId());
        }
    }

    // Buff 35: Gardener
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isActive(35) || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;
        Block target = event.getClickedBlock();
        if (target == null) return;
        int r = cfg().getGardenerRadius();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
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
        if (lastUse != null && System.currentTimeMillis() - lastUse < cfg().getTeleporterCooldownMs()) return;
        Location target = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(cfg().getTeleporterDistance()));
        player.teleport(target);
        teleporterCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // Buff 46: Alchemist
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!isActive(46) || event.getItem().getType() != Material.POTION) return;
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

    // Buff 47: Builder
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (isActive(47) && Math.random() < cfg().getBuilderRefundChance()) {
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
        event.getPlayer().setVelocity(event.getPlayer().getVelocity().setY(cfg().getDoubleJumpVelocity()));
        hasDoubleJumped.add(event.getPlayer().getUniqueId());
    }

    // Buff 49: Whale
    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (isActive(49) && cfg().getBuffBool("whale-infinite-oxygen", true) && event.getEntity() instanceof Player) {
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

    // Player join
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

    // Player quit
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        teleporterCooldowns.remove(uuid); hasDoubleJumped.remove(uuid);
        pearlTimeMap.remove(uuid); inertiaPlayers.remove(uuid);
    }

    private void inertiaTick() {
        if (!isActive(44)) { inertiaPlayers.clear(); return; }
        double threshold = cfg().getInertiaThreshold();
        for (UUID uuid : Set.copyOf(inertiaPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Vector vel = player.getVelocity();
                if (Math.abs(vel.getX()) > threshold || Math.abs(vel.getZ()) > threshold) {
                    player.setVelocity(new Vector(0, vel.getY(), 0));
                    inertiaPlayers.remove(uuid);
                }
            } else { inertiaPlayers.remove(uuid); }
        }
    }

    private void magnetTick() {
        if (!isActive(11)) return;
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
        if (!isActive(45)) return;
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
