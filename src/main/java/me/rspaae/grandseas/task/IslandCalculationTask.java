package me.rspaae.grandseas.task;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the island level/points asynchronously using ChunkSnapshots.
 */
public class IslandCalculationTask {

    private final GrandSeas plugin;
    private final Island island;
    private final Player requestor;
    
    private final Map<Material, Integer> blockValues;
    private final int defaultBlockValue;

    public IslandCalculationTask(GrandSeas plugin, Island island, Player requestor) {
        this.plugin = plugin;
        this.island = island;
        this.requestor = requestor;
        
        this.blockValues = new HashMap<>();
        var settings = plugin.getConfigManager().getSettings();
        this.defaultBlockValue = settings.getDefaultBlockValue();

        ConfigurationSection blocks = settings.getPointsBlocksSection();
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    blockValues.put(mat, blocks.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid material in points config: " + key);
                }
            }
        }
    }

    public void start() {
        if (island.getCenter() == null) {
            if (requestor != null) {
                requestor.sendMessage(Component.text("Cannot calculate island points: Center not found.", NamedTextColor.RED));
            }
            return;
        }

        if (!island.getCalculatingFlag().compareAndSet(false, true)) {
            if (requestor != null) {
                requestor.sendMessage(Component.text("⏳ Calculation is already in progress...", NamedTextColor.YELLOW));
            }
            return;
        }

        if (requestor != null) {
            requestor.sendActionBar(Component.text("🔍 Menghitung blok pulau...", NamedTextColor.YELLOW));
            requestor.playSound(requestor.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        }

        // 1. Gather all chunks within the island border synchronously
        int radius = island.getBorderRadius();
        int cx = island.getCenter().getBlockX();
        int cz = island.getCenter().getBlockZ();
        
        int minX = cx - radius;
        int maxX = cx + radius;
        int minZ = cz - radius;
        int maxZ = cz + radius;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        List<ChunkSnapshot> snapshots = new ArrayList<>();
        
        // This must be done on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    Chunk chunk = island.getCenter().getWorld().getChunkAt(x, z);
                    snapshots.add(chunk.getChunkSnapshot());
                }
            }

            // 2. Process the snapshots asynchronously
            int minHeight = island.getCenter().getWorld().getMinHeight();
            int maxHeight = island.getCenter().getWorld().getMaxHeight();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    long totalPoints = 0;

                    for (ChunkSnapshot snapshot : snapshots) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                // Calculate absolute world coordinates to ensure we only count blocks strictly within the border
                                int worldX = (snapshot.getX() << 4) + x;
                                int worldZ = (snapshot.getZ() << 4) + z;
                                
                                // Check if this specific column is within the exact circular/square border
                                if (worldX < minX || worldX > maxX || worldZ < minZ || worldZ > maxZ) {
                                    continue;
                                }

                                for (int y = minHeight; y < maxHeight; y++) {
                                    Material type = snapshot.getBlockType(x, y, z);
                                    if (type.isAir()) continue;
                                    
                                    // Ignore water and lava in calculation
                                    if (type == Material.WATER || type == Material.LAVA) continue;

                                    totalPoints += blockValues.getOrDefault(type, defaultBlockValue);
                                }
                            }
                        }
                    }

                    final long finalPoints = totalPoints;

                    // 3. Update the island synchronously
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        long totalStoragePoints = 0;
                        for (me.rspaae.grandseas.model.PointBlock pb : plugin.getPointBlockManager().getAll()) {
                            if (island.isWithinBorder(pb.getLocation())) {
                                int val = blockValues.getOrDefault(pb.getMaterial(), defaultBlockValue);
                                if (pb.getAmount() > 1) {
                                    totalStoragePoints += (pb.getAmount() - 1) * val;
                                }
                            }
                        }
                        long actualFinalPoints = finalPoints + totalStoragePoints;

                        island.setPoints(actualFinalPoints);
                        plugin.getIslandManager().saveIslands();
                    
                        if (requestor != null && requestor.isOnline()) {
                            // Hilangkan chat message agar tidak tertimbun, ganti dengan Title
                            requestor.sendTitle(
                                    "§b§lLEVEL PULAU: " + island.getIslandLevel(),
                                    "§eTotal Poin: §f" + String.format("%,d", actualFinalPoints),
                                    10, 70, 20
                            );
                            requestor.playSound(requestor.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                        }
                    });
                } finally {
                    island.getCalculatingFlag().set(false);
                }
            });
        });
    }
}
