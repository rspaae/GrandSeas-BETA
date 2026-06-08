package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.block.BlockState;

import java.util.Random;

public class OreGeneratorListener implements Listener {
    private final GrandSeas plugin;
    private final Random random;

    public OreGeneratorListener(GrandSeas plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        BlockState newState = event.getNewState();
        if (newState.getType() == Material.COBBLESTONE || newState.getType() == Material.STONE) {
            
            // Periksa apakah block tersebut berada di dalam pulau (radius)
            if (event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getIslandWorld().getName())) {
                Island island = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());
                
                if (island != null) {
                    Material generatedOre = getRandomOre(island.getGeneratorLevel());
                    newState.setType(generatedOre);
                    // The event will naturally apply the newState
                }
            }
        }
    }

    private Material getRandomOre(int level) {
        int chance = random.nextInt(100); // 0 to 99

        switch (level) {
            case 1:
                // Level 1: Gold 5%, Redstone 5%, Lapis 5%, Iron 20%, Coal 25%, Cobble 40%
                if (chance < 5) return Material.GOLD_ORE;
                if (chance < 10) return Material.REDSTONE_ORE;
                if (chance < 15) return Material.LAPIS_ORE;
                if (chance < 35) return Material.IRON_ORE;
                if (chance < 60) return Material.COAL_ORE;
                return Material.COBBLESTONE;
            case 2:
                // Level 2: Diamond 2%, Gold 8%, Redstone 10%, Lapis 10%, Iron 20%, Coal 20%, Cobble 30%
                if (chance < 2) return Material.DIAMOND_ORE;
                if (chance < 10) return Material.GOLD_ORE;
                if (chance < 20) return Material.REDSTONE_ORE;
                if (chance < 30) return Material.LAPIS_ORE;
                if (chance < 50) return Material.IRON_ORE;
                if (chance < 70) return Material.COAL_ORE;
                return Material.COBBLESTONE;
            case 3:
                // Level 3: Emerald 2%, Diamond 8%, Gold 15%, Redstone 10%, Lapis 10%, Iron 20%, Coal 15%, Cobble 20%
                if (chance < 2) return Material.EMERALD_ORE;
                if (chance < 10) return Material.DIAMOND_ORE;
                if (chance < 25) return Material.GOLD_ORE;
                if (chance < 35) return Material.REDSTONE_ORE;
                if (chance < 45) return Material.LAPIS_ORE;
                if (chance < 65) return Material.IRON_ORE;
                if (chance < 80) return Material.COAL_ORE;
                return Material.COBBLESTONE;
            case 4:
                // Level 4: Netherite 1%, Emerald 5%, Diamond 10%, Gold 15%, Redstone 15%, Lapis 15%, Iron 15%, Coal 14%, Cobble 10%
                if (chance < 1) return Material.ANCIENT_DEBRIS;
                if (chance < 6) return Material.EMERALD_ORE;
                if (chance < 16) return Material.DIAMOND_ORE;
                if (chance < 31) return Material.GOLD_ORE;
                if (chance < 46) return Material.REDSTONE_ORE;
                if (chance < 61) return Material.LAPIS_ORE;
                if (chance < 76) return Material.IRON_ORE;
                if (chance < 90) return Material.COAL_ORE;
                return Material.COBBLESTONE;
            case 5:
                // Level 5: Netherite 2%, Emerald 15%, Diamond 15%, Gold 15%, Redstone 15%, Lapis 15%, Iron 8%, Coal 10%, Cobble 5%
                if (chance < 2) return Material.ANCIENT_DEBRIS;
                if (chance < 17) return Material.EMERALD_ORE;
                if (chance < 32) return Material.DIAMOND_ORE;
                if (chance < 47) return Material.GOLD_ORE;
                if (chance < 62) return Material.REDSTONE_ORE;
                if (chance < 77) return Material.LAPIS_ORE;
                if (chance < 85) return Material.IRON_ORE;
                if (chance < 95) return Material.COAL_ORE;
                return Material.COBBLESTONE;
            default:
                return Material.COBBLESTONE;
        }
    }
}
