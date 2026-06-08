package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class ConfirmationGUI extends AbstractGUI {
    
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final String actionName;
    private final String description;

    public ConfirmationGUI(String actionName, String description, Runnable onConfirm, Runnable onCancel) {
        super(27, "⚠ Confirm: " + actionName);
        this.actionName = actionName;
        this.description = description;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillBorders(Material.BLACK_STAINED_GLASS_PANE);
        
        // Info book in center (Slot 13)
        ItemStack info = new ItemBuilder(Material.PAPER)
                .name(Component.text("⚠ " + actionName, NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.empty(),
                    Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("This action cannot be undone!", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)
                )
                .enchant(Enchantment.UNBREAKING, 1)
                .flag(ItemFlag.HIDE_ENCHANTS)
                .build();
        inventory.setItem(13, info);

        // Confirm (Slots 10, 11, 19, 20)
        ItemStack confirm = new ItemBuilder(Material.LIME_CONCRETE)
                .name(Component.text("✅ CONFIRM", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Click to proceed.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(10, confirm);
        inventory.setItem(11, confirm);
        inventory.setItem(19, confirm);
        inventory.setItem(20, confirm);

        // Cancel (Slots 15, 16, 24, 25)
        ItemStack cancel = new ItemBuilder(Material.RED_CONCRETE)
                .name(Component.text("❌ CANCEL", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Click to cancel.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(15, cancel);
        inventory.setItem(16, cancel);
        inventory.setItem(24, cancel);
        inventory.setItem(25, cancel);
        
        // Background for the rest
        fillEmpty(Material.GRAY_STAINED_GLASS_PANE);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        
        int slot = event.getRawSlot();
        if (slot == 10 || slot == 11 || slot == 19 || slot == 20) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            if (onConfirm != null) onConfirm.run();
        } else if (slot == 15 || slot == 16 || slot == 24 || slot == 25) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            if (onCancel != null) onCancel.run();
        }
    }
}
