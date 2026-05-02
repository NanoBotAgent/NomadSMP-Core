package com.nomadsmp.core.listeners;

import com.nomadsmp.core.NomadCore;
import com.nomadsmp.core.modules.DailyBuffModule;
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
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BuffListeners implements Listener {

    private final NomadCore plugin;

    // Per-player cooldowns and state
    private final Map<UUID, Long> teleporterCooldowns = new HashMap<>();
    private final Set<UUID> hasDoubleJumped = new HashMap<>();
    private final Map<UUID, Long> pearlTimeMap = new HashMap<>();
    private int magnetTaskId = -1;
    private int gravityWellTaskId = -1;

    public BuffListeners(NomadCore plugin) {
        this.plugin = plugin;

        // Magnet (buff 11) — repeating task every 4 ticks
        magnetTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::magnetTick, 4L, 4L).getTaskId();

        // Gravity Well (buff 45) — repeating task every 10 ticks
        gravityWellTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::gravityWellTick, 10L, 10L).getTaskId();
    }

    private DailyBuffModule buffs() { return plugin.getDailyBuffModule(); }
    private boolean isActive(int id) { return buffs().isBuffActive(id); }

    // ─── Buff 1: Titanium — no durability loss ───
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isActive(1)) event.setCancelled(true);
    }

    // ─── Buff 8: Looter — double mob drops ───
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (isActive(8)) { // Looter — double drops
            List<ItemStack> extra = new ArrayList<>();
            for (ItemStack drop : event.getDrops()) {
                extra.add(drop.clone());
            }
            event.getDrops().addAll(extra);
        }

        if (isActive(16)) { // Trophy Hunter — mob head drop
            var equipment = event.getEntity().getEquipment();
            if (equipment != null) {
                ItemStack helmet = equipment.getHelmet();
                if (helmet != null && helmet.getType() == Material.PLAYER_HEAD) {
                    event.getDrops().add(helmet.clone());
                }
            }
        }

        if (isActive(19)) { // Vampire — heal 1 HP on kill
            double maxHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            killer.setHealth(Math.min(killer.getHealth() + 1.0, maxHealth));
        }

        if (isActive(32)) { // Rich — 10% gold nugget drop
            if (Math.random() < 0.1) {
                event.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
            }
        }

        if (isActive(39)) { // Thor — 5% lightning on hit (handled in damage event)
            // Handled in onEntityDamageByEntity
        }
    }

    // ─── Buff 9: Bountiful Harvest — double crop growth ───
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (isActive(9)) {
            Block block = event.getBlock();
            Material type = block.getType();
            if (Tag.CROPS.isTagged(type) || type == Material.PUMPKIN_STEM || type == Material.MELON_STEM) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        var state = block.getState();
                        if (state.getData() instanceof org.bukkit.material.Crops crops) {
                            crops.setState(org.bukkit.CropState.RIPE);
                            state.update();
                        }
                    } catch (Exception ignored) {}
                }, 1L);
            }
        }
    }

    // ─── Buff 12: Chef — cancel hunger drain ───
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (isActive(12) && event.getFoodLevel() < event.getEntity().getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // ─── Buff 13: Blacksmith — auto-smelt ore drops ───
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!isActive(13)) return;
        Block block = event.getBlock();
        Material blockType = block.getType();

        // Map raw ores to smelted results
        Map<Material, Material> smeltingMap = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.IRON_ORE, Material.IRON_INGOT,
            Material.GOLD_ORE, Material.GOLD_INGOT,
            Material.COPPER_ORE, Material.COPPER_INGOT
        );

        Material smelted = smeltingMap.get(blockType);
        if (smelted != null) {
            for (Item item : event.getItems()) {
                if (smeltingMap.containsKey(item.getItemStack().getType())) {
                    item.getItemStack().setType(smelted);
                }
            }
        }
    }

    // ─── Buff 14: Timber — tree feller ───
    @EventHandler(priority = EventPriority.HIGH)
    public void onTimber(BlockBreakEvent event) {
        if (!isActive(14)) return;
        Block block = event.getBlock();
        if (!Tag.LOGS.isTagged(block.getType())) return;

        Set<Block> treeBlocks = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(block);
        treeBlocks.add(block);

        while (!queue.isEmpty() && treeBlocks.size() < 50) {
            Block current = queue.poll();
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN}) {
                Block neighbor = current.getRelative(face);
                if (!treeBlocks.contains(neighbor) && neighbor.getType() == block.getType()) {
                    treeBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        for (Block b : treeBlocks) {
            if (b.equals(block)) continue;
            b.breakNaturally(event.getPlayer().getInventory().getItemInMainHand());
        }
    }

    // ─── Buff 15: Vein Miner ───
    @EventHandler(priority = EventPriority.HIGH)
    public void onVeinMiner(BlockBreakEvent event) {
        if (!isActive(15)) return;
        Block block = event.getBlock();
        Material oreType = block.getType();
        if (!isOre(oreType)) return;

        Set<Block> veinBlocks = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(block);
        veinBlocks.add(block);

        while (!queue.isEmpty() && veinBlocks.size() < 32) {
            Block current = queue.poll();
            for (BlockFace face : BlockFace.values()) {
                Block neighbor = current.getRelative(face);
                if (!veinBlocks.contains(neighbor) && neighbor.getType() == oreType) {
                    veinBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        for (Block b : veinBlocks) {
            if (b.equals(block)) continue;
            b.breakNaturally(event.getPlayer().getInventory().getItemInMainHand());
        }
    }

    private boolean isOre(Material mat) {
        return mat.name().endsWith("_ORE") || mat.name().endsWith("_DEEPSLATE_ORE");
    }

    // ─── Buff 20: XP Junkie — double XP ───
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (isActive(20)) {
            event.setAmount(event.getAmount() * 2);
        }
    }

    // ─── Buff 23: Glass Cannon — 2x damage dealt & taken ───
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isActive(23) && event.getDamager() instanceof Player player && buffs().isBuffActive(23)) {
            event.setDamage(event.getDamage() * 2);
        }

        // Buff 39: Thor — 5% lightning on hit
        if (isActive(39) && event.getDamager() instanceof Player && Math.random() < 0.05) {
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
        }

        // Buff 50: Pacifist — swords deal 0 damage
        if (isActive(50) && event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon.getType().name().endsWith("_SWORD")) {
                event.setDamage(0);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Buff 23: Glass Cannon — 2x damage taken
        if (isActive(23)) {
            event.setDamage(event.getDamage() * 2);
        }

        // Buff 28: Ender — no pearl fall damage
        if (isActive(28) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long pearlTime = pearlTimeMap.get(player.getUniqueId());
            if (pearlTime != null && System.currentTimeMillis() - pearlTime < 1000) {
                event.setCancelled(true);
                pearlTimeMap.remove(player.getUniqueId());
            }
        }

        // Buff 36: Snowman — fire/lava immune
        if (isActive(36) && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        }

        // Buff 38: Slimy — cancel fall damage, bounce
        if (isActive(38) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            double fallDistance = player.getFallDistance();
            player.setVelocity(player.getVelocity().setY(fallDistance * 0.05));
        }

        // Buff 42: Unstoppable — immune to wither
        if (isActive(42) && event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }

        // Buff 44: Inertia — cancel knockback
        if (isActive(44) && event instanceof EntityKnockbackEvent) {
            event.setCancelled(true);
        }
    }

    // ─── Buff 24: Archer — free arrows ───
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isActive(24)) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        // Mark arrow as no-consume (handled via custom metadata)
        arrow.setCustomName("nomad-arrow");
    }

    // ─── Buff 25: Librarian — 1-level enchanting ───
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (isActive(25)) {
            event.setExpLevelCost(1);
        }
    }

    // ─── Buff 27: Spider — wall climbing when sneaking ───
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isActive(27) && player.isSneaking()) {
            Block beside = player.getLocation().getBlock().getRelative(player.getFacing());
            if (beside.getType().isSolid()) {
                player.setVelocity(player.getVelocity().setY(0.3));
            }
        }

        // Buff 36: Snowman — leave snow trail
        if (isActive(36)) {
            Block prev = event.getFrom().getBlock();
            if (prev.getType() == Material.AIR && prev.getRelative(BlockFace.DOWN).getType().isSolid()) {
                prev.setType(Material.SNOW);
            }
        }

        // Buff 48: Double Jump — reset on ground
        if (isActive(48) && player.isOnGround()) {
            hasDoubleJumped.remove(player.getUniqueId());
        }
    }

    // ─── Buff 33: Scavenger — bonus grass loot ───
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

    // ─── Buff 35: Gardener — 3x3 bone meal ───
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isActive(35)) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;
        Block target = event.getClickedBlock();
        if (target == null) return;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block relative = target.getRelative(dx, 0, dz);
                relative.applyBoneMeal();
            }
        }
    }

    // ─── Buff 40: Teleporter — 5-block sneak teleport ───
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!isActive(40) || !event.isSneaking()) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Long lastUse = teleporterCooldowns.get(uuid);
        if (lastUse != null && System.currentTimeMillis() - lastUse < 10000) return;

        org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize().multiply(5);
        org.bukkit.Location target = player.getLocation().add(direction);
        if (target.getBlock().getType().isSolid() && target.getBlock().getRelative(BlockFace.UP).getType().isSolid()) return;

        player.teleport(target);
        teleporterCooldowns.put(uuid, System.currentTimeMillis());
        player.sendMessage("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eTeleported!");
    }

    // ─── Buff 46: Alchemist — 3x potion duration ───
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!isActive(46)) return;
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) return;
        // Re-apply effects with 3x duration after 1 tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
                event.getPlayer().addPotionEffect(new PotionEffect(
                    effect.getType(), effect.getDuration() * 3, effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles()
                ));
            }
        }, 1L);
    }

    // ─── Buff 47: Builder — 20% block refund ───
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (!isActive(47)) return;
        if (Math.random() < 0.2) {
            ItemStack hand = event.getItemInHand();
            hand.setAmount(hand.getAmount() + 1);
        }
    }

    // ─── Buff 48: Double Jump ───
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!isActive(48)) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (hasDoubleJumped.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setVelocity(player.getVelocity().setY(0.8));
        hasDoubleJumped.put(player.getUniqueId(), Boolean.TRUE);
    }

    // ─── Buff 49: Whale — infinite oxygen ───
    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (!isActive(49)) return;
        if (event.getEntity() instanceof Player) {
            event.setAmount(Integer.MAX_VALUE);
        }
    }

    // ─── Ender pearl tracking for buff 28 ───
    @EventHandler
    public void onPearlThrow(PlayerTeleportEvent event) {
        if (isActive(28) && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            pearlTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    // ─── Player join — apply buffs ───
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDailyBuffModule().applyToPlayer(player);

        // Broadcast buff
        if (plugin.getConfigManager().isBroadcastOnJoin()) {
            var buffIds = plugin.getDailyBuffModule().getCurrentBuffIds();
            if (!buffIds.isEmpty()) {
                StringBuilder msg = new StringBuilder("\u00a78[\u00a76NomadSMP\u00a78] \u00a7eToday's buff: ");
                for (int id : buffIds) {
                    msg.append("\u00a7a").append(plugin.getDailyBuffModule().getBuffName(id));
                    msg.append(" \u00a77\u2014 ").append(plugin.getDailyBuffModule().getBuffDescription(id));
                }
                player.sendMessage(msg.toString());
            }
        }
    }

    // ─── Player quit — cleanup ───
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        teleporterCooldowns.remove(uuid);
        hasDoubleJumped.remove(uuid);
        pearlTimeMap.remove(uuid);
    }

    // ─── Magnet tick (buff 11) ───
    private void magnetTick() {
        if (!isActive(11)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Item item) {
                    org.bukkit.util.Vector direction = player.getLocation().toVector()
                        .subtract(item.getLocation().toVector())
                        .normalize().multiply(0.5);
                    item.setVelocity(direction);
                }
            }
        }
    }

    // ─── Gravity Well tick (buff 45) ───
    private void gravityWellTick() {
        if (!isActive(45)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Monster monster && monster.getTarget() == player) {
                    org.bukkit.util.Vector direction = player.getLocation().toVector()
                        .subtract(monster.getLocation().toVector())
                        .normalize().multiply(0.3);
                    monster.setVelocity(direction);
                }
            }
        }
    }
}
