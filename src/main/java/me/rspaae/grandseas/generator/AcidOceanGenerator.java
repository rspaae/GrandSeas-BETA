package me.rspaae.grandseas.generator;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

import org.bukkit.util.noise.SimplexOctaveGenerator;

/**
 * Natural ocean generator.
 * Generates an uneven seabed and fills water up to SEA_LEVEL.
 */
public class AcidOceanGenerator extends ChunkGenerator {

    /** Y-level permukaan air (island akan diletakkan di atas ini). */
    public static final int SEA_LEVEL = 63;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        
        SimplexOctaveGenerator generator = new SimplexOctaveGenerator(new Random(worldInfo.getSeed()), 8);
        generator.setScale(0.015D); // Semakin kecil semakin landai gelombangnya

        int minHeight = worldInfo.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int realX = chunkX * 16 + x;
                int realZ = chunkZ * 16 + z;

                // Hitung ketinggian dasar laut menggunakan noise
                // Hasil noise: -1 sampai 1.
                double noise = generator.noise(realX, realZ, 0.5D, 0.5D);
                // Kita angkat sedikit agar jarak pulau (y=63) ke dasar tidak terlalu jauh
                int baseHeight = 45 + (int) (noise * 10); 

                // Batas bawah pasti bedrock
                chunkData.setBlock(x, minHeight, z, Material.BEDROCK);

                // Isi dari atas bedrock sampai baseHeight dengan batu & dirt/sand
                for (int y = minHeight + 1; y <= baseHeight; y++) {
                    if (y >= baseHeight - 2) {
                        // 3 blok teratas dasar laut berupa pasir atau dirt
                        chunkData.setBlock(x, y, z, random.nextBoolean() ? Material.SAND : Material.DIRT);
                    } else {
                        // Bawahnya batu
                        chunkData.setBlock(x, y, z, Material.STONE);
                    }
                }

                // Isi air dari atas dasar laut sampai SEA_LEVEL
                for (int y = baseHeight + 1; y <= SEA_LEVEL; y++) {
                    chunkData.setBlock(x, y, z, Material.WATER);
                }
            }
        }
    }

    // Matikan semua vanilla generation agar dunia tetap bersih
    // Dihapus @Override agar cross-version compatible (tidak error di Paper 1.26+ jika method ini dihapus dari API)
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateNoise()        { return false; }
    
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateSurface()      { return false; }
    
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateCaves()        { return false; }
    
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateDecorations()  { return false; }
    
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateMobs()         { return false; }
    
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateStructures()   { return false; }

    @SuppressWarnings("deprecation")
    public boolean canSpawn(@NotNull World world, int x, int z) {
        return true;
    }
}
