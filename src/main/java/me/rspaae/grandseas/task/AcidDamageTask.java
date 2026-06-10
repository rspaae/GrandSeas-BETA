package me.rspaae.grandseas.task;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.config.GrandSeasSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies the acid effect to players — inspired by AcidIsland (BentoBox).
 *
 * Instead of a single global timer looping all players, this uses:
 * 1. {@link PlayerMoveEvent} to detect when a player enters acid water or gets exposed to rain.
 * 2. Per-player {@link BukkitRunnable} timers that handle the burn/rain delay and continuous damage.
 * 3. Armor durability damage — acid corrodes armor over time, eventually breaking it.
 * 4. Damage reduction based on the ARMOR attribute (full diamond = ~80% reduction).
 * 5. Immunity via WATER_BREATHING / CONDUIT_POWER potion effects.
 * 6. Rain safety: any helmet, cover above, snow biomes, humidity check.
 * 7. Acid safety: boat/raft, full armor, immune potion effects.
 */
public class AcidDamageTask implements Listener {

    private final GrandSeas plugin;
    private final GrandSeasSettings settings;
    private final String islandWorldName;

    /** Players currently burning in acid water — value is the timestamp when damage starts. */
    private final Map<Player, Long> burningPlayers = new HashMap<>();

    /** Players currently wet from acid rain — value is the timestamp when damage starts. */
    private final Map<Player, Long> wetPlayers = new HashMap<>();

    public AcidDamageTask(GrandSeas plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.islandWorldName = settings.getWorldName();
    }

    // ──────────────────────────────────────────────
    //  Event Handlers
    // ──────────────────────────────────────────────

    /**
     * Clean up maps when a player dies.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        burningPlayers.remove(event.getEntity());
        wetPlayers.remove(event.getEntity());
    }

    /**
     * Bounce players back up if they fall below the world (into the acid void).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSeaBounce(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!player.getWorld().getName().equals(islandWorldName)) {
            return;
        }
        if (player.getLocation().getBlockY() < player.getWorld().getMinHeight()) {
            player.setVelocity(new Vector(player.getVelocity().getX(), 1D, player.getVelocity().getZ()));
        }
    }

    /**
     * Main entry point — detects acid water and rain exposure on movement.
     * Per-player BukkitRunnables handle the actual delay + continuous damage.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isExemptFromAcid(player)) {
            return;
        }

        // --- Rain exposure ---
        handleRainExposure(player);

        // --- Acid water exposure ---
        if (!burningPlayers.containsKey(player) && !isSafeFromAcid(player)) {
            startAcidBurn(player);
        }
    }

    // ──────────────────────────────────────────────
    //  Exemption & Safety Checks
    // ──────────────────────────────────────────────

    /**
     * Check if a player is completely exempt from acid processing.
     */
    private boolean isExemptFromAcid(Player player) {
        return (settings.getAcidRainDamage() == 0 && settings.getAcidPlayerDamage() == 0)
                || player.isDead()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || !player.getWorld().getName().equals(islandWorldName)
                || (player.isOp() && !settings.isAcidDamageOp());
    }

    /**
     * Check if player is safe from acid water.
     */
    boolean isSafeFromAcid(Player player) {
        // Game mode check
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return true;
        }

        // Not in liquid
        Material blockType = player.getLocation().getBlock().getType();
        Material aboveType = player.getLocation().getBlock().getRelative(BlockFace.UP).getType();

        if (blockType != Material.WATER
                && blockType != Material.BUBBLE_COLUMN
                && (blockType != Material.SNOW || !settings.isAcidSnowDamage())
                && aboveType != Material.WATER) {
            return true;
        }

        // Check if player is on a boat or raft
        if (player.getVehicle() != null) {
            String vehicleKey = player.getVehicle().getType().getKey().getKey();
            if (vehicleKey.contains("boat") || vehicleKey.contains("raft")) {
                return true;
            }
        }

        // Check if full armor protects
        if (settings.isFullArmorProtectsFromAcid() && hasFullArmor(player)) {
            return true;
        }

        // Check if player has an immune potion effect
        return hasImmuneEffect(player);
    }

    /**
     * Check if player is safe from acid rain.
     */
    private boolean isSafeFromRain(Player player) {
        // Game mode / environment
        if (player.getGameMode() != GameMode.SURVIVAL
                || player.getWorld().getEnvironment() == Environment.NETHER
                || player.getWorld().getEnvironment() == Environment.THE_END) {
            return true;
        }

        // Helmet protection (any helmet, not just turtle)
        if (settings.isHelmetProtectsFromRain()) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null && helmet.getType().name().contains("HELMET")) {
                return true;
            }
        }

        // Snow biome (snow falls instead of rain) and snow damage is disabled
        if (!settings.isAcidSnowDamage() && player.getLocation().getBlock().getTemperature() < 0.1) {
            return true;
        }

        // Dry biome (humidity == 0)
        if (player.getLocation().getBlock().getHumidity() == 0) {
            return true;
        }

        // Immune potion effects
        if (hasImmuneEffect(player)) {
            return true;
        }

        // Check if all blocks above player are air (has cover)
        int playerY = player.getLocation().getBlockY() + 2;
        int maxY = player.getLocation().getWorld().getMaxHeight();
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        for (int y = playerY; y < maxY; y++) {
            if (!player.getWorld().getBlockAt(x, y, z).getType().equals(Material.AIR)) {
                return true; // Found a non-air block above = has cover
            }
        }

        return false;
    }

    // ──────────────────────────────────────────────
    //  Rain Handling
    // ──────────────────────────────────────────────

    /**
     * Handle rain exposure — starts a per-player rain timer if not already tracked.
     */
    private void handleRainExposure(Player player) {
        if (settings.getAcidRainDamage() <= 0D || !isRaining(player)) {
            return;
        }

        if (isSafeFromRain(player)) {
            wetPlayers.remove(player);
            return;
        }

        // Start rain timer if not already tracked
        if (wetPlayers.putIfAbsent(player,
                System.currentTimeMillis() + settings.getAcidBurnDelaySeconds() * 1000L) == null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (checkAndApplyRain(player)) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    /**
     * Check if it's raining in the player's world.
     */
    private boolean isRaining(Player player) {
        World world = player.getWorld();
        return world.hasStorm() || world.isThundering();
    }

    /**
     * Periodic rain check — applies damage or cancels if player is safe.
     *
     * @return true if the rain timer should be cancelled.
     */
    private boolean checkAndApplyRain(Player player) {
        // Stop conditions
        if (!isRaining(player) || player.isDead() || isSafeFromRain(player)
                || settings.getAcidRainDamage() <= 0D) {
            wetPlayers.remove(player);
            return true;
        }

        // Check if delay has passed
        Long startTime = wetPlayers.get(player);
        if (startTime == null) {
            return true;
        }
        if (startTime > System.currentTimeMillis()) {
            return false; // Still in delay period
        }

        // Calculate damage with armor reduction
        double protection = settings.getAcidRainDamage() * getDamageReduced(player);
        double totalDamage = Math.max(0, settings.getAcidRainDamage() - protection);

        if (totalDamage > 0D) {
            // Apply potion effects (filtered to valid effects only)
            int effectTicks = settings.getRainEffectDurationSeconds() * 20;
            for (PotionEffectType type : settings.getRainEffects()) {
                if (GrandSeasSettings.VALID_ACID_EFFECTS.contains(type)) {
                    player.addPotionEffect(new PotionEffect(type, effectTicks, 1, false, false, false));
                }
            }

            // Apply damage
            player.damage(totalDamage);

            // Action bar warning
            player.sendActionBar(Component.text("☂ ACID RAIN IS BURNING YOU! ☂", NamedTextColor.DARK_RED));

            // Sound
            Location loc = player.getLocation();
            player.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 3F, 3F);
        }

        return false;
    }

    // ──────────────────────────────────────────────
    //  Acid Water Handling
    // ──────────────────────────────────────────────

    /**
     * Start the acid burn process for a player — creates a per-player repeating timer.
     */
    private void startAcidBurn(Player player) {
        burningPlayers.put(player, System.currentTimeMillis() + settings.getAcidBurnDelaySeconds() * 1000L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (continuouslyHurtPlayer(player)) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Periodic acid check — applies damage or cancels if player is safe.
     *
     * @return true if the acid timer should be cancelled.
     */
    private boolean continuouslyHurtPlayer(Player player) {
        // Stop conditions
        if (player.isDead() || isSafeFromAcid(player)) {
            burningPlayers.remove(player);
            return true;
        }

        // Check if delay has passed
        Long startTime = burningPlayers.get(player);
        if (startTime == null) {
            return true;
        }
        if (startTime > System.currentTimeMillis()) {
            return false; // Still in delay period
        }

        // Calculate damage with armor reduction
        double protection = settings.getAcidPlayerDamage() * getDamageReduced(player);
        double totalDamage = Math.max(0, settings.getAcidPlayerDamage() - protection);

        if (totalDamage > 0D) {
            // Apply potion effects (filtered to valid effects only)
            int effectTicks = settings.getAcidEffectDurationSeconds() * 20;
            for (PotionEffectType type : settings.getAcidEffects()) {
                if (GrandSeasSettings.VALID_ACID_EFFECTS.contains(type)) {
                    player.addPotionEffect(new PotionEffect(type, effectTicks, 1, false, false, false));
                }
            }

            // Apply damage
            player.damage(totalDamage);

            // Action bar warning
            player.sendActionBar(Component.text("☠ THE ACID IS BURNING YOU! ☠", NamedTextColor.RED));

            // Sound
            Location loc = player.getLocation();
            player.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 3F, 3F);
        }

        return false;
    }

    // ──────────────────────────────────────────────
    //  Armor System
    // ──────────────────────────────────────────────

    /**
     * Calculates damage reduction based on armor attribute and damages armor durability.
     * Full diamond armor (armor value = 20) gives ~80% reduction. Enchantments can go higher.
     *
     * @param entity the living entity
     * @return reduction factor (0.0 = no armor, 0.8 = full diamond)
     */
    public static double getDamageReduced(LivingEntity entity) {
        // Armor attribute value * 0.04 gives reduction (max 0.8 for full diamond = 20 * 0.04)
        double reduction = 0;
        if (entity.getAttribute(Attribute.ARMOR) != null) {
            reduction = entity.getAttribute(Attribute.ARMOR).getValue() * 0.04;
        }

        // Damage each armor piece (acid corrodes armor)
        EntityEquipment inv = entity.getEquipment();
        if (inv == null) return reduction;

        ItemStack helmet = inv.getHelmet();
        ItemStack chestplate = inv.getChestplate();
        ItemStack leggings = inv.getLeggings();
        ItemStack boots = inv.getBoots();

        if (helmet != null && helmet.getType().name().contains("HELMET") && damageArmor(helmet)) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
            inv.setHelmet(null);
        }
        if (boots != null && damageArmor(boots)) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
            inv.setBoots(null);
        }
        if (leggings != null && damageArmor(leggings)) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
            inv.setLeggings(null);
        }
        if (chestplate != null && damageArmor(chestplate)) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
            inv.setChestplate(null);
        }

        return reduction;
    }

    /**
     * Damages an armor piece by 1 durability point.
     *
     * @param item the armor item to damage
     * @return true if the item is now broken (durability exhausted)
     */
    private static boolean damageArmor(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable && !damageable.isUnbreakable()) {
            damageable.setDamage(damageable.getDamage() + 1);
            item.setItemMeta(damageable);
            return damageable.getDamage() >= item.getType().getMaxDurability();
        }
        return false;
    }

    // ──────────────────────────────────────────────
    //  Utility Methods
    // ──────────────────────────────────────────────

    /**
     * Check if a player has any immune potion effect active.
     */
    private boolean hasImmuneEffect(Player player) {
        return player.getActivePotionEffects().stream()
                .map(PotionEffect::getType)
                .anyMatch(GrandSeasSettings.IMMUNE_EFFECTS::contains);
    }

    /**
     * Check if a player has full armor set.
     */
    private boolean hasFullArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor.length < 4) {
            return false;
        }
        return Arrays.stream(armor).allMatch(i -> i != null && i.getType() != Material.AIR);
    }

    /**
     * Cleanup method — call on plugin disable to cancel all running tasks.
     */
    public void cleanup() {
        burningPlayers.clear();
        wetPlayers.clear();
    }
}
