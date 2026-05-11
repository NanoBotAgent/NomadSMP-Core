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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class BuffListeners implements Listener {

    private final NomadCore plugin;
    private final Map<UUID, Long> teleporterCooldowns = new HashMap<>();
    private final Set<UUID> hasDoubleJumped = new HashSet<>();
    private final Map<UUID, Long> pearlTimeMap = new HashMap<>();
    private final Set<UUID> inertiaPlayers = new HashSet<>();
    // Tracks blocks placed by players to exclude from Timber/LeafCut
    private final Set<Location> playerPlacedBlocks = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
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

    // Track player-placed blocks
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        playerPlacedBlocks.add(event.getBlock().getLocation());
    }

    /** Check if a block was placed by a player (not naturally generated). */
    private boolean isPlayerPlaced(Block block) {
        return playerPlacedBlocks.contains(block.getLocation());
    }

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

    // 14: Timber (rewritten), 15: Vein Miner, 51: Leaf Cut
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 14: Timber — only breaks naturally-generated logs in the same tree
        if (active(14) && Tag.LOGS.isTagged(block.getType())) {
            if (!isPlayerPlaced(block)) {
                timberBreak(block, cfg().getTimberMax(), player, tool);
                plugin.getStatsManager().recordTimberUse();
            }
        }

        // 15: Vein Miner
        if (active(15) && block.getType().name().endsWith("_ORE")) {
            bfsBreak(block, block.getType(), cfg().getVeinMinerMax(), player);
            plugin.getStatsManager().recordVeinMinerUse();
        }

        // 51: Leaf Cut — when breaking a log, check if the tree above is fully cut
        // (no more logs remaining above this block) and remove natural leaves
        if (active(51) && Tag.LOGS.isTagged(block.getType()) && !isPlayerPlaced(block)) {
            // Delay 1 tick so the broken log is gone when we scan
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeOrphanedLeaves(block);
            }, 1L);
        }
    }

    /**
     * Timber: BFS that prioritizes the tree being cut.
     * - Starts from the broken block, searches UP first (tree trunk), then sideways
     * - Skips player-placed logs
     * - Applies durability damage to the tool per log broken
     */
    private void timberBreak(Block start, int maxBlocks, Player player, ItemStack tool) {
        Set<Block> visited = new HashSet<>();
        // Priority: UP first (trunk), then sideways, then down
        BlockFace[] priorityFaces = {
            BlockFace.UP, BlockFace.UP_2,
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
            BlockFace.DOWN
        };

        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            Block current = queue.poll();
            for (BlockFace face : priorityFaces) {
                Block neighbor = current.getRelative(face);
                if (!visited.contains(neighbor)
                        && Tag.LOGS.isTagged(neighbor.getType())
                        && !isPlayerPlaced(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // Break all connected logs (skip the start block — already broken by the event)
        for (Block b : visited) {
            if (!b.equals(start)) {
                b.breakNaturally(tool);
                applyDurabilityDamage(tool, 1);
            }
        }

        // After timber, check for leaf cut opportunity (if both buffs are active)
        if (active(51)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeOrphanedLeaves(start);
            }, 2L);
        }
    }

    /**
     * Apply durability damage to a tool. Respects Unbreaking enchantment.
     * Does nothing if the tool is not damageable (e.g. hand, non-tool item).
     */
    private void applyDurabilityDamage(ItemStack tool, int damage) {
        if (tool == null || tool.getType() == Material.AIR) return;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        // Unbreaking enchantment: chance to cancel damage
        int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        for (int i = 0; i < damage; i++) {
            if (unbreaking > 0 && Math.random() < (1.0 / (unbreaking + 1.0))) {
                continue; // Unbreaking cancelled this damage tick
            }
            damageable.setDamage(damageable.getDamage() + 1);
        }
        tool.setItemMeta(meta);

        // Break the tool if max damage reached
        if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
            tool.setAmount(0);
            playerBreakEffect(tool);
        }
    }

    /** Play break sound/particles when a tool breaks from durability. */
    private void playerBreakEffect(ItemStack tool) {
        // Bukkit handles the break animation client-side when amount reaches 0
    }

    /**
     * Leaf Cut (51): Remove natural leaves from trees that have been fully cut.
     * Scans upward from the origin block. If no connected logs remain in the
     * tree column, removes all natural (non-player-placed) leaves above.
     */
    private void removeOrphanedLeaves(Block origin) {
        World world = origin.getWorld();
        int baseX = origin.getX();
        int baseZ = origin.getZ();
        int startY = origin.getY();

        // Scan the column and nearby columns for any remaining logs
        // A tree trunk is typically 1 block wide (oak/birch) or 2x2 (large oak/spruce)
        boolean hasLogsRemaining = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = startY; y < world.getMaxHeight(); y++) {
                    Block check = world.getBlockAt(baseX + dx, y, baseZ + dz);
                    if (Tag.LOGS.isTagged(check.getType()) && !isPlayerPlaced(check)) {
                        hasLogsRemaining = true;
                        break;
                    }
                }
                if (hasLogsRemaining) break;
            }
            if (hasLogsRemaining) break;
        }

        // Also check slightly wider for large tree canopies
        if (!hasLogsRemaining) {
            // Wider scan — 5x5 area, check a few blocks above origin
            for (int dx = -2; dx <= 2 && !hasLogsRemaining; dx++) {
                for (int dz = -2; dz <= 2 && !hasLogsRemaining; dz++) {
                    for (int y = startY; y < Math.min(startY + 30, world.getMaxHeight()); y++) {
                        Block check = world.getBlockAt(baseX + dx, y, baseZ + dz);
                        if (Tag.LOGS.isTagged(check.getType()) && !isPlayerPlaced(check)) {
                            hasLogsRemaining = true;
                            break;
                        }
                    }
                }
            }
        }

        if (hasLogsRemaining) return; // Tree not fully cut — leave the leaves alone

        // No logs remaining — remove all natural leaves in the canopy area
        int maxLeafRadius = cfg().buffInt(51, "leaf-radius", 4);
        int maxLeafHeight = cfg().buffInt(51, "leaf-height", 30);
        int removed = 0;
        int maxRemove = cfg().buffInt(51, "max-leaves", 100);

        for (int dx = -maxLeafRadius; dx <= maxLeafRadius && removed < maxRemove; dx++) {
            for (int dz = -maxLeafRadius; dz <= maxLeafRadius && removed < maxRemove; dz++) {
                for (int dy = 0; dy < maxLeafHeight && removed < maxRemove; dy++) {
                    Block leaf = world.getBlockAt(baseX + dx, startY + dy, baseZ + dz);
                    if (Tag.LEAVES.isTagged(leaf.getType()) && !isPlayerPlaced(leaf)) {
                        // Double-check: this leaf is not connected to any remaining log elsewhere
                        leaf.breakNaturally();
                        removed++;
                    }
                }
            }
        }
    }

    // Vein Miner uses the original BFS (ores are always natural)
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

    // 40: Teleporter — real teleportation via raytrace (safe landing)
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

        int distance = cfg().getTeleporterDistance();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        // Raytrace to find a safe teleport destination
        Location destination = null;
        Location safeLanding = null;

        // Step along the ray in 1-block increments
        for (int step = 1; step <= distance; step++) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(step));

            // Check if this block is passable (air/water/etc.)
            Block feetBlock = checkLoc.getBlock();
            Block headBlock = checkLoc.clone().add(0, 1, 0).getBlock();

            boolean feetPassable = !feetBlock.getType().isSolid();
            boolean headPassable = !headBlock.getType().isSolid();

            if (!feetPassable || !headPassable) {
                // Hit a wall — stop at the last safe position
                break;
            }

            // Check if there's solid ground below the feet position
            Block groundBlock = checkLoc.clone().add(0, -1, 0).getBlock();
            if (groundBlock.getType().isSolid()) {
                safeLanding = checkLoc.clone();
            }

            destination = checkLoc.clone();
        }

        // Prefer safe landing (standing on ground), otherwise last passable position
        Location teleportTarget = safeLanding != null ? safeLanding : destination;

        if (teleportTarget == null) {
            // No valid teleport destination found (staring at a wall at point-blank)
            player.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7cNo valid teleport destination.");
            return;
        }

        // Set proper rotation and center the player in the block
        teleportTarget.setX(teleportTarget.getBlockX() + 0.5);
        teleportTarget.setZ(teleportTarget.getBlockZ() + 0.5);
        teleportTarget.setYaw(player.getLocation().getYaw());
        teleportTarget.setPitch(player.getLocation().getPitch());

        player.teleport(teleportTarget);
        teleporterCooldowns.put(player.getUniqueId(), now);

        // Visual feedback
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
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
