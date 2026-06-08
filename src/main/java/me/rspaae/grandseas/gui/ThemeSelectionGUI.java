package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.IslandTheme;
import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ThemeSelectionGUI extends AbstractGUI {

    private final GrandSeas plugin;

    public ThemeSelectionGUI(GrandSeas plugin) {
        super(54, "✨ Pilih Tema Pulau Pilihanmu");
        this.plugin = plugin;
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillEmpty(Material.BLACK_STAINED_GLASS_PANE);

        // Premium Border Pattern
        int[] cyanBorders = {0, 1, 7, 8, 9, 17, 36, 44, 45, 46, 52, 53};
        int[] blueBorders = {2, 3, 4, 5, 6, 18, 26, 27, 35, 47, 48, 49, 50, 51};
        
        for (int slot : cyanBorders) {
            inventory.setItem(slot, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name(Component.empty()).build());
        }
        for (int slot : blueBorders) {
            inventory.setItem(slot, new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name(Component.empty()).build());
        }

        // Info item in slot 13 (top center)
        ItemStack info = new ItemBuilder(Material.NETHER_STAR)
                .name(Component.text("Pilihan Tema Pulau", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.empty(),
                    Component.text("Pilih tema awal untuk pulaumu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Setiap tema memiliki bentuk dan", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("sumber daya yang berbeda!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty()
                )
                .build();
        inventory.setItem(13, info);

        // Place 3 themes in slots 29, 31, 33 (Row 4, nicely spaced)
        int[] slots = {29, 31, 33};
        IslandTheme[] themes = IslandTheme.values();

        for (int i = 0; i < themes.length && i < slots.length; i++) {
            IslandTheme theme = themes[i];
            
            ItemStack themeItem = new ItemBuilder(theme.getIcon())
                    .name(Component.text("✦ " + theme.getDisplayName() + " ✦", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                    .lore(
                        Component.empty(),
                        Component.text(theme.getDescription(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("▶ Klik untuk memilih tema ini!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    )
                    .build();
                    
            inventory.setItem(slots[i], themeItem);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (clickedItem.getType().name().contains("GLASS_PANE") || clickedItem.getType() == Material.NETHER_STAR) return;

        // Determine which theme was clicked
        IslandTheme selectedTheme = null;
        for (IslandTheme theme : IslandTheme.values()) {
            if (theme.getIcon() == clickedItem.getType()) {
                selectedTheme = theme;
                break;
            }
        }

        if (selectedTheme != null) {
            player.closeInventory();

            // Safety: double-check they don't already have an island
            if (plugin.getIslandManager().getIsland(player.getUniqueId()) != null) {
                player.sendMessage(Component.text("You already have an island!", NamedTextColor.RED));
                return;
            }

            // Create the island!
            plugin.getIslandManager().createIsland(player, selectedTheme);
        }
    }
}
