package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import me.rspaae.grandseas.model.Island;

/**
 * Protects islands by preventing unauthorized block/entity interactions.
 * Only the island owner and members can interact within an island's border.
 * Nobody can interact in the open ocean (outside any island border) in the island world.
 */
public class IslandProtectionListener implements Listener {
    private final GrandSeas plugin;

    private static final Component DENY_MESSAGE = Component.text("⛔ You can't do that here!", NamedTextColor.RED);
    private static final Component BAN_MESSAGE = Component.text("⛔ You are banned from this island!", NamedTextColor.RED);

    public IslandProtectionListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getIslandManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getIslandManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (plugin.getIslandManager().getIslandWorld() == null) return;
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();

        // Check if player is banned from this island
        if (player.getWorld().getName().equals(plugin.getIslandManager().getIslandWorld().getName())) {
            Island at = plugin.getIslandManager().getIslandAt(loc);
            if (at != null && at.isBanned(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(BAN_MESSAGE);
                // Teleport them out
                player.teleport(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
                return;
            }
        }

        if (!plugin.getIslandManager().canInteract(player, loc)) {
            event.setCancelled(true);
            player.sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.getIslandManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!plugin.getIslandManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        
        // Handle PvP
        if (event.getEntity() instanceof Player) {
            Island island = plugin.getIslandManager().getIslandAt(event.getEntity().getLocation());
            if (island != null) {
                if (!island.isPvpEnabled()) {
                    event.setCancelled(true);
                    attacker.sendMessage(Component.text("⚔ PvP is disabled on this island!", NamedTextColor.RED));
                    return;
                }
            } else {
                // Open ocean PvP is disabled by default to prevent spawn killing
                event.setCancelled(true);
                attacker.sendMessage(Component.text("⚔ PvP is disabled in the open ocean!", NamedTextColor.RED));
                return;
            }
        }
        
        // Handle generic protection (breaking frames, killing animals, etc)
        if (!plugin.getIslandManager().canBuild(attacker, event.getEntity().getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage(DENY_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Hanya izinkan mob spawn dari SPAWNER, kecuali setting pulau mengizinkan
        if (event.getLocation().getWorld() != null
                && event.getLocation().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) {
            
            Island island = plugin.getIslandManager().getIslandAt(event.getLocation());
            if (island != null && island.isMobSpawning()) {
                return; // Allow if setting is on
            }
            
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // Hapus semua drop item dari entity yang mati di island world
        if (event.getEntity().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
