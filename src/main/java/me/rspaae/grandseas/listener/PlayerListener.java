package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {
    private final GrandSeas plugin;

    public PlayerListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    // ── #3 FIRST JOIN ──────────────────────────────────────────────
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update border jika sudah di dalam island world
        plugin.getIslandManager().updatePlayerBorder(player, player.getLocation());

        // Jika player pertama kali join dan belum punya island
        if (!player.hasPlayedBefore()) {
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            if (island == null) {
                // Teleport ke spawn dunia utama (lobby) dan buka GUI create
                Location lobby = org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation();
                player.teleport(lobby);
                // Delay sedikit supaya chunk loaded
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
                    player.sendMessage(Component.text("  🏝 Selamat datang di GrandSeas!", NamedTextColor.GOLD));
                    player.sendMessage(Component.text("  Ketik /is untuk membuat pulaumu!", NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
                    player.sendTitle("§6§l🏝 GrandSeas", "§eKetik §b/is §euntuk mulai!", 10, 80, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                }, 20L);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getIslandManager().updatePlayerBorder(event.getPlayer(), event.getTo()));
    }

    // ── #2 RESPAWN DI ISLAND SENDIRI ───────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());

        if (island != null) {
            // Utamakan home yang sudah di-set pemain, lalu center
            Location respawn = island.getHome() != null ? island.getHome() : island.getCenter();
            if (respawn != null) {
                event.setRespawnLocation(respawn);
                return;
            }
        }

        // Jika tidak punya island, respawn di spawn dunia utama
        event.setRespawnLocation(org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    // ── VOID PROTECTION ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;

        boolean isVoid = event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID
                || player.getLocation().getY() <= plugin.getIslandManager().getIslandWorld().getMinHeight() + 5;

        if (isVoid) {
            event.setCancelled(true);
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            Location safe = island != null && island.getHome() != null ? island.getHome()
                    : island != null && island.getCenter() != null ? island.getCenter()
                    : player.getWorld().getSpawnLocation();
            player.teleport(safe);
            player.setFallDistance(0);
            player.sendMessage(Component.text("⚠ Kamu jatuh ke void dan diselamatkan!", NamedTextColor.YELLOW));
            player.playSound(safe, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove from island chat if active
        plugin.getIslandManager().islandChatPlayers.remove(event.getPlayer().getUniqueId());
    }
}
