package me.rspaae.grandseas.listener;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.gui.PointStorageGUI;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.model.PointBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PointBlockListener implements Listener {

    private final GrandSeas plugin;

    public PointBlockListener(GrandSeas plugin) {
        this.plugin = plugin;
    }

    private boolean isPointBlockType(Material material) {
        ConfigurationSection sec = plugin.getConfigManager().getSettings().getPointsBlocksSection();
        if (sec == null) return false;
        return sec.contains(material.name()) && sec.getInt(material.name()) > 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;
        
        Island island = plugin.getIslandManager().getIslandAt(block.getLocation());
        if (island == null) return;

        if (isPointBlockType(block.getType())) {
            plugin.getPointBlockManager().addPointBlock(block.getLocation(), block.getType());
            Player player = event.getPlayer();
            player.sendMessage(Component.text("✨ ", NamedTextColor.YELLOW)
                    .append(Component.text("Point Storage created! Right-click it to start depositing blocks.", NamedTextColor.GREEN)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;

        PointBlock pb = plugin.getPointBlockManager().getPointBlock(block.getLocation());
        if (pb != null) {
            if (pb.getAmount() > 1) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("⚠ ", NamedTextColor.RED)
                        .append(Component.text("Jangan dihancurkan! Ambil dulu semua isinya lewat menu (Klik Kanan).", NamedTextColor.YELLOW)));
            } else {
                plugin.getPointBlockManager().removePointBlock(block.getLocation());
                event.getPlayer().sendMessage(Component.text("🗑 Timbunan dihancurkan.", NamedTextColor.GRAY));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!block.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;

        PointBlock pb = plugin.getPointBlockManager().getPointBlock(block.getLocation());
        if (pb != null) {
            event.setCancelled(true); // Prevent normal block interaction if any
            Player player = event.getPlayer();
            Island island = plugin.getIslandManager().getIslandAt(block.getLocation());
            if (island == null) return;

            if (!island.isMemberOrOwner(player.getUniqueId()) && !player.hasPermission("grandseas.admin")) {
                player.sendMessage(Component.text("⚠ Hei! Kamu tidak punya izin untuk membuka ini!", NamedTextColor.RED));
                return;
            }

            PointStorageGUI gui = new PointStorageGUI(plugin, pb, island);
            player.openInventory(gui.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getLocation().getWorld() == null) return;
        if (!event.getLocation().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;
        event.blockList().removeIf(block -> plugin.getPointBlockManager().getPointBlock(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;
        event.blockList().removeIf(block -> plugin.getPointBlockManager().getPointBlock(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;
        for (Block block : event.getBlocks()) {
            if (plugin.getPointBlockManager().getPointBlock(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.getBlock().getWorld().getName().equals(plugin.getIslandManager().getWorldName())) return;
        for (Block block : event.getBlocks()) {
            if (plugin.getPointBlockManager().getPointBlock(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
