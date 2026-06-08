package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class IslandTopGUI extends AbstractGUI {

    private final GrandSeas plugin;
    private final Player player;

    public IslandTopGUI(GrandSeas plugin, Player player) {
        super(45, "✦ Island Leaderboard");
        this.plugin = plugin;
        this.player = player;
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillBorders(Material.BLACK_STAINED_GLASS_PANE);
        fillEmpty(Material.BLACK_STAINED_GLASS_PANE);

        // Sort by island level descending, then by points
        List<Island> sortedIslands = new ArrayList<>(plugin.getIslandManager().getAllIslands().values());
        sortedIslands.sort((a, b) -> {
            int cmp = Integer.compare(b.getIslandLevel(), a.getIslandLevel());
            return cmp != 0 ? cmp : Long.compare(b.getPoints(), a.getPoints());
        });

        // Centered display slots for top 10
        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34, 29, 33};

        String[] rankPrefix = {"✦ #1", "✦ #2", "✦ #3", "#4", "#5", "#6", "#7", "#8", "#9", "#10"};
        NamedTextColor[] rankColors = {
            NamedTextColor.GOLD,
            NamedTextColor.GRAY,
            NamedTextColor.GOLD,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE,
            NamedTextColor.WHITE
        };

        for (int i = 0; i < slots.length && i < sortedIslands.size(); i++) {
            Island island = sortedIslands.get(i);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) continue;

            meta.setOwningPlayer(owner);
            meta.displayName(Component.text(rankPrefix[i] + " " + island.getName(), rankColors[i], TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  ⭐ Island Level  ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(island.getIslandLevel() + "", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("  🏆 Total Points  ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(String.format("%,d", island.getPoints()), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)));
            if (island.isShowBalanceOnTop()) {
                lore.add(Component.text("  💰 Bank  ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("$" + String.format("%,.1f", island.getBalance()), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)));
            } else {
                lore.add(Component.text("  💰 Bank  ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Hidden", NamedTextColor.GRAY, TextDecoration.OBFUSCATED)
                        .decoration(TextDecoration.ITALIC, false)));
            }

            if (island.isShowMembersOnTop()) {
                lore.add(Component.text("  👥 Members (" + (island.getMembers().size() + 1) + ")", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("    - ", NamedTextColor.GRAY).append(Component.text(ownerName + " (Owner)", NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false));
                int count = 0;
                for (java.util.UUID memberId : island.getMembers()) {
                    if (count >= 5) {
                        lore.add(Component.text("    - ", NamedTextColor.GRAY).append(Component.text("...and more", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false));
                        break;
                    }
                    String mName = Bukkit.getOfflinePlayer(memberId).getName();
                    lore.add(Component.text("    - ", NamedTextColor.GRAY).append(Component.text(mName != null ? mName : "Unknown", NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
                    count++;
                }
            } else {
                lore.add(Component.text("  👥 Members (" + (island.getMembers().size() + 1) + ")", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("    - ", NamedTextColor.GRAY).append(Component.text("Hidden", NamedTextColor.GRAY, TextDecoration.OBFUSCATED)).decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("  " + island.getTheme().getDisplayName(), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            skull.setItemMeta(meta);
            inventory.setItem(slots[i], skull);
        }

        // Title item center top
        ItemStack info = new ItemBuilder(Material.NETHER_STAR)
                .name(Component.text("  Island Leaderboard  ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("The top islands on this server!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Store valuable blocks on your island", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("to climb the ranks.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(4, info);

        // Close button
        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(40, close);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player clicker = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (event.getRawSlot() == 40) {
            clicker.closeInventory();
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
}
