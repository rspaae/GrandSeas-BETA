package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.model.PointBlock;
import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;

public class PointStorageGUI extends AbstractGUI {

    private final GrandSeas plugin;
    private final PointBlock pointBlock;
    private final Island island;

    public PointStorageGUI(GrandSeas plugin, PointBlock pointBlock, Island island) {
        super(45, "📦 Storage: " + pointBlock.getMaterial().name().replace("_", " "));
        this.plugin = plugin;
        this.pointBlock = pointBlock;
        this.island = island;
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillEmpty(Material.BLACK_STAINED_GLASS_PANE);

        Material mat = pointBlock.getMaterial();
        long amount = pointBlock.getAmount();

        // Info Item (Center)
        ItemStack info = new ItemBuilder(mat)
                .name(Component.text("✦ " + mat.name().replace("_", " ") + " ✦", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.empty(),
                        Component.text("Stored: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(amount, NamedTextColor.AQUA)),
                        Component.empty()
                )
                .build();
        inventory.setItem(22, info);

        // Insert Hand
        ItemStack insertHand = new ItemBuilder(Material.HOPPER)
                .name(Component.text("⬇ Deposit from Hand", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Deposit the " + mat.name().replace("_", " ") + " you're holding.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ).build();
        inventory.setItem(11, insertHand);

        // Insert All
        ItemStack insertAll = new ItemBuilder(Material.CHEST)
                .name(Component.text("⬇ Deposit All", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Deposit all " + mat.name().replace("_", " ") + " from your inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ).build();
        inventory.setItem(15, insertAll);

        // Withdraw Stack
        ItemStack withdrawStack = new ItemBuilder(Material.DROPPER)
                .name(Component.text("⬆ Withdraw Stack (64)", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Take 64 blocks from storage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ).build();
        inventory.setItem(29, withdrawStack);

        // Withdraw All
        ItemStack withdrawAll = new ItemBuilder(Material.DISPENSER)
                .name(Component.text("⬆ Withdraw All", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Take as many blocks as your inventory allows.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ).build();
        inventory.setItem(33, withdrawAll);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null) return;
        Material type = event.getCurrentItem().getType();
        if (type == Material.BLACK_STAINED_GLASS_PANE) return;

        Material blockType = pointBlock.getMaterial();
        int blockValue = plugin.getConfigManager().getSettings().getPointsBlocksSection().getInt(blockType.name(), 0);

        long amountChanged = 0;

        if (event.getRawSlot() == 11) {
            // Insert Hand
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == blockType) {
                amountChanged = hand.getAmount();
                player.getInventory().setItemInMainHand(null);
                pointBlock.addAmount(amountChanged);
            } else {
                player.sendMessage(Component.text("You're not holding " + blockType.name().replace("_", " ") + "!", NamedTextColor.RED));
                return;
            }
        } else if (event.getRawSlot() == 15) {
            // Insert All
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == blockType) {
                    amountChanged += item.getAmount();
                    player.getInventory().setItem(i, null);
                }
            }
            if (amountChanged > 0) {
                pointBlock.addAmount(amountChanged);
            } else {
                player.sendMessage(Component.text("You don't have any " + blockType.name().replace("_", " ") + " in your inventory!", NamedTextColor.RED));
                return;
            }
        } else if (event.getRawSlot() == 29) {
            // Withdraw Stack
            if (pointBlock.getAmount() <= 1) { // Keep 1 as physical block
                player.sendMessage(Component.text("Nothing left to withdraw! (1 block reserved as the physical block)", NamedTextColor.RED));
                return;
            }
            long toTake = Math.min(64, pointBlock.getAmount() - 1);
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(blockType, (int) toTake));
            if (!left.isEmpty()) {
                long couldnTake = left.get(0).getAmount();
                toTake -= couldnTake;
            }
            if (toTake > 0) {
                amountChanged = -toTake;
                pointBlock.removeAmount(toTake);
            } else {
                player.sendMessage(Component.text("Your inventory is full!", NamedTextColor.RED));
                return;
            }
        } else if (event.getRawSlot() == 33) {
            // Withdraw All
            if (pointBlock.getAmount() <= 1) {
                player.sendMessage(Component.text("Nothing left to withdraw! (1 block reserved as the physical block)", NamedTextColor.RED));
                return;
            }
            long takenTotal = 0;
            while (pointBlock.getAmount() > 1) {
                long toTake = Math.min(64, pointBlock.getAmount() - 1);
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(blockType, (int) toTake));
                if (!left.isEmpty()) {
                    long couldnTake = left.get(0).getAmount();
                    long took = toTake - couldnTake;
                    takenTotal += took;
                    pointBlock.removeAmount(took);
                    break; // Inv full
                } else {
                    takenTotal += toTake;
                    pointBlock.removeAmount(toTake);
                }
            }
            if (takenTotal > 0) {
                amountChanged = -takenTotal;
            } else {
                player.sendMessage(Component.text("Your inventory is full!", NamedTextColor.RED));
                return;
            }
        }

        if (amountChanged != 0) {
            // Update Hologram
            plugin.getPointBlockManager().updateHologram(pointBlock);
            plugin.getPointBlockManager().save();

            // Instant calculation: add/subtract points based on blockValue
            // amountChanged is positive for insert, negative for withdraw.
            long pointsDelta = amountChanged * blockValue;
            island.setPoints(island.getPoints() + pointsDelta);
            plugin.getIslandManager().saveIslands();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            initializeItems(); // refresh GUI
        }
    }
}
