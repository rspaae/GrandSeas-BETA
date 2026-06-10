package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

/**
 * Prevents water/lava from flowing outside an island's border into the acid ocean.
 * Without this, water sources on islands could "leak" across the entire ocean.
 */
public class FluidFlowListener implements Listener {

    private final GrandSeas plugin;

    public FluidFlowListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        // Only handle events in the island world
        if (!event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;

        // Source block (water/lava origin)
        Island sourceIsland = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());

        // Flow destination block
        Island targetIsland = plugin.getIslandManager().getIslandAt(event.getToBlock().getLocation());

        // If the source is on an island but the destination is outside that island (or on another island) → STOP
        if (sourceIsland != null && !sourceIsland.equals(targetIsland)) {
            event.setCancelled(true);
            return;
        }

        // If the source is not on any island (open ocean) → let ocean water flow normally
        // but block it if the flow direction is into someone else's island → STOP
        if (sourceIsland == null && targetIsland != null) {
            event.setCancelled(true);
        }
    }
}
