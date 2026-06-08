package me.rspaae.grandseas.manager;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.PointBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PointBlockManager {

    private final GrandSeas plugin;
    private final Map<Location, PointBlock> pointBlocks = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public PointBlockManager(GrandSeas plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        dataFile = new File(dataDir, "storage_blocks.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create storage_blocks.yml", e);
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection blocksSec = dataConfig.getConfigurationSection("blocks");
        if (blocksSec == null) return;

        for (String key : blocksSec.getKeys(false)) {
            ConfigurationSection sec = blocksSec.getConfigurationSection(key);
            if (sec == null) continue;

            String worldName = sec.getString("world");
            if (worldName == null || Bukkit.getWorld(worldName) == null) continue;

            Location loc = new Location(
                    Bukkit.getWorld(worldName),
                    sec.getInt("x"),
                    sec.getInt("y"),
                    sec.getInt("z")
            );

            Material mat;
            try {
                mat = Material.valueOf(sec.getString("material"));
            } catch (Exception e) {
                continue;
            }

            long amount = sec.getLong("amount", 1);
            PointBlock pb = new PointBlock(loc, mat, amount);

            if (sec.contains("text_display")) {
                try {
                    pb.setTextDisplayId(UUID.fromString(sec.getString("text_display")));
                } catch (Exception ignored) {}
            }

            pointBlocks.put(loc, pb);
            
            // Re-spawn or update hologram
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateHologram(pb), 20L);
        }
    }

    public void save() {
        if (dataFile == null) return;
        dataConfig = new YamlConfiguration();
        
        ConfigurationSection blocksSec = dataConfig.createSection("blocks");
        int i = 0;
        for (PointBlock pb : pointBlocks.values()) {
            ConfigurationSection sec = blocksSec.createSection(String.valueOf(i++));
            sec.set("world", pb.getLocation().getWorld().getName());
            sec.set("x", pb.getLocation().getBlockX());
            sec.set("y", pb.getLocation().getBlockY());
            sec.set("z", pb.getLocation().getBlockZ());
            sec.set("material", pb.getMaterial().name());
            sec.set("amount", pb.getAmount());
            if (pb.getTextDisplayId() != null) {
                sec.set("text_display", pb.getTextDisplayId().toString());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save storage_blocks.yml", e);
        }
    }

    public PointBlock getPointBlock(Location loc) {
        return pointBlocks.get(loc);
    }

    public void addPointBlock(Location loc, Material material) {
        PointBlock pb = new PointBlock(loc, material, 1);
        pointBlocks.put(loc, pb);
        updateHologram(pb);
        save();
    }

    public void removePointBlock(Location loc) {
        PointBlock pb = pointBlocks.remove(loc);
        if (pb != null) {
            removeHologram(pb);
            save();
        }
    }

    public void updateHologram(PointBlock pb) {
        TextDisplay display = getDisplayEntity(pb);
        
        if (display == null) {
            Location spawnLoc = pb.getLocation().clone().add(0.5, 1.2, 0.5);
            display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, e -> {
                e.setBillboard(Display.Billboard.CENTER);
                e.setDefaultBackground(false);
                e.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // Transparan 100%
                e.setShadowed(true); // Biar tulisan tetap jelas walau tanpa background
                
                // Set scale smaller to look nice above a block
                Transformation t = e.getTransformation();
                t.getScale().set(0.8f, 0.8f, 0.8f);
                e.setTransformation(t);
            });
            pb.setTextDisplayId(display.getUniqueId());
        }

        String matName = pb.getMaterial().name().replace("_", " ");
        Component text = Component.text(matName, NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("x" + pb.getAmount(), NamedTextColor.YELLOW, TextDecoration.BOLD));
                
        display.text(text);
    }

    private void removeHologram(PointBlock pb) {
        TextDisplay display = getDisplayEntity(pb);
        if (display != null) {
            display.remove();
        }
        pb.setTextDisplayId(null);
    }

    private TextDisplay getDisplayEntity(PointBlock pb) {
        if (pb.getTextDisplayId() == null) return null;
        org.bukkit.entity.Entity entity = Bukkit.getEntity(pb.getTextDisplayId());
        if (entity instanceof TextDisplay) {
            return (TextDisplay) entity;
        }
        return null;
    }
    
    public Collection<PointBlock> getAll() {
        return pointBlocks.values();
    }
}
