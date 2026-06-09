package me.rspaae.grandseas.task;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.AuditLog;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs every 60 seconds to purge expired co-op sessions from all islands.
 * If a co-op player is still on the island when their session expires,
 * they are teleported out and notified.
 */
public class CoopExpiryTask extends BukkitRunnable {

    private final GrandSeas plugin;

    public CoopExpiryTask(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, Island> entry : plugin.getIslandManager().getAllIslands().entrySet()) {
            Island island = entry.getValue();
            Set<UUID> expired = island.purgeExpiredCoop();

            if (expired.isEmpty()) continue;

            // Log and notify for each expired co-op player
            for (UUID expiredUuid : expired) {
                // Add audit log entry
                String expiredName = Bukkit.getOfflinePlayer(expiredUuid).getName();
                if (expiredName == null) expiredName = expiredUuid.toString().substring(0, 8);

                plugin.getIslandManager().addAuditLog(island.getOwner(),
                        new AuditLog(expiredUuid, expiredName, AuditLog.Action.COOP_EXPIRE,
                                "Co-op session expired"));

                // Notify the island owner
                Player owner = Bukkit.getPlayer(island.getOwner());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(Component.text(
                            "ℹ " + expiredName + "'s co-op session on your island has expired.",
                            NamedTextColor.GRAY));
                }

                // Kick the co-op player if they are still on the island
                Player coopPlayer = Bukkit.getPlayer(expiredUuid);
                if (coopPlayer != null && coopPlayer.isOnline()) {
                    String worldName = plugin.getIslandManager().getWorldName();
                    if (coopPlayer.getWorld().getName().equals(worldName)) {
                        Location coopLoc = coopPlayer.getLocation();
                        Island currentIsland = plugin.getIslandManager().getIslandAt(coopLoc);
                        if (currentIsland != null && currentIsland.getOwner().equals(island.getOwner())) {
                            // Teleport back to main world spawn
                            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                            coopPlayer.teleport(spawn);
                            coopPlayer.setWorldBorder(null);
                            coopPlayer.sendMessage(Component.text(
                                    "⏰ Your co-op session on this island has expired. You have been removed.",
                                    NamedTextColor.YELLOW));
                        }
                    }
                }
            }
        }
    }
}
