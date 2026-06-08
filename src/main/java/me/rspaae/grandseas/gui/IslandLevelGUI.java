package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class IslandLevelGUI extends AbstractGUI {

    private final GrandSeas plugin;
    private final Player player;
    private final Island island;

    public IslandLevelGUI(GrandSeas plugin, Player player, Island island) {
        super(54, "✦ Island Progress & Stats");
        this.plugin = plugin;
        this.player = player;
        this.island = island;
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillBorders(Material.BLACK_STAINED_GLASS_PANE);
        fillEmpty(Material.GRAY_STAINED_GLASS_PANE);

        long points = island.getPoints();
        int islandLevel = island.getIslandLevel();
        
        long usedPoints = (long)(islandLevel - 1) * 100L;
        long currentLevelExp = points - usedPoints;
        long nextLevelReq = 100L;
        int progressPercent = (int) Math.min(100, (currentLevelExp * 100) / Math.max(1, nextLevelReq));

        // Slot 22: Core Level Info (Center)
        ItemStack core = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(Component.text("⭐ Island Level " + islandLevel, NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.empty(),
                    Component.text("  Progress to Level " + (islandLevel + 1) + ":", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("  " + currentLevelExp + " / " + nextLevelReq + " pts  (" + progressPercent + "%)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("  🏆 Total Points: ", NamedTextColor.GRAY).append(Component.text(String.format("%,d", points), NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Place point blocks on your island", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("to permanently earn points.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .flag(ItemFlag.HIDE_ENCHANTS)
                .build();
        inventory.setItem(22, core);

        // Slot 19: Upgrade Stats (Left)
        ItemStack stats = new ItemBuilder(Material.END_CRYSTAL)
                .name(Component.text("📈 Upgrade Stats", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.empty(),
                    Component.text("  📏 Border: ", NamedTextColor.GRAY).append(Component.text("Level " + island.getBorderLevel(), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false),
                    Component.text("      Size: " + island.getBorderSize() + "x" + island.getBorderSize() + " blocks", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("  ⛏ Ore Generator: ", NamedTextColor.GRAY).append(Component.text("Level " + island.getGeneratorLevel(), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Use /is upgrades to level these up.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(19, stats);

        // Slot 25: Bank (Right)
        ItemStack bank = new ItemBuilder(Material.GOLD_BLOCK)
                .name(Component.text("💰 Island Bank", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.empty(),
                    Component.text("  Current Balance:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("  $" + String.format("%,.2f", island.getBalance()), NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Used for upgrades and transactions.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(25, bank);

        // Progress Bar on Row 4 (Slots 37-43) - 7 slots wide
        int filledSlots = (progressPercent * 7) / 100;
        ItemStack filledPane = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name(Component.text("█ Progress: " + progressPercent + "%", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).build();
        ItemStack emptyPane = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(Component.text("░ Not yet filled", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)).build();

        for (int i = 0; i < 7; i++) {
            if (i < filledSlots) {
                inventory.setItem(37 + i, filledPane);
            } else {
                inventory.setItem(37 + i, emptyPane);
            }
        }
        
        // Slot 49: Close Button
        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(49, close);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player clicker = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (event.getRawSlot() == 49) {
            clicker.closeInventory();
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}
