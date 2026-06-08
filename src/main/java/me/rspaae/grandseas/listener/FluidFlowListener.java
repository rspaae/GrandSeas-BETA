package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

/**
 * Mencegah air/lava mengalir keluar dari border island ke laut acid.
 * Tanpa ini, water source di island bisa "bocor" ke seluruh lautan.
 */
public class FluidFlowListener implements Listener {

    private final GrandSeas plugin;

    public FluidFlowListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        // Hanya tangani di island world
        if (!event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;

        // Blok asal (sumber air/lava)
        Island sourceIsland = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());

        // Blok tujuan aliran
        Island targetIsland = plugin.getIslandManager().getIslandAt(event.getToBlock().getLocation());

        // Kalau sumber ada di island tapi tujuan sudah di luar island (atau island lain) → STOP
        if (sourceIsland != null && !sourceIsland.equals(targetIsland)) {
            event.setCancelled(true);
            return;
        }

        // Kalau sumber bukan di island manapun (di laut bebas) → biarkan air laut mengalir normal
        // tapi kalau arahnya masuk ke island orang lain → STOP
        if (sourceIsland == null && targetIsland != null) {
            event.setCancelled(true);
        }
    }
}
