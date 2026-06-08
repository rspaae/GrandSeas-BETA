package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractGUI implements InventoryHolder {
    protected final Inventory inventory;

    public AbstractGUI(int size, String title) {
        // Size must be a multiple of 9
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
    }
    
    /**
     * Called after construction to populate the items.
     * We don't call this in the abstract constructor to allow subclasses
     * to initialize their fields first if needed.
     */
    public void open() {
        initializeItems();
    }

    /**
     * Populate the inventory with items.
     */
    protected abstract void initializeItems();

    /**
     * Handle click events within this GUI.
     * @param event The click event
     */
    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ================== Helper Methods for Premium UI ==================

    protected void fillBorders(Material borderMaterial) {
        ItemStack border = new ItemBuilder(borderMaterial).name(Component.empty()).build();
        int size = inventory.getSize();
        int rows = size / 9;

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem((rows - 1) * 9 + i, border);
        }

        // Left and right columns
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    protected void fillEmpty(Material fillMaterial) {
        ItemStack filler = new ItemBuilder(fillMaterial).name(Component.empty()).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }
}
