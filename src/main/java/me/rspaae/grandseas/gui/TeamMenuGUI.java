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

import java.util.UUID;

public class TeamMenuGUI extends AbstractGUI {
    
    private final GrandSeas plugin;
    private final Player player;
    private final Island island;

    public TeamMenuGUI(GrandSeas plugin, Player player) {
        super(54, "👥 Team Management");
        this.plugin = plugin;
        this.player = player;
        this.island = plugin.getIslandManager().getIsland(player.getUniqueId());
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillBorders(Material.BLUE_STAINED_GLASS_PANE);
        fillEmpty(Material.GRAY_STAINED_GLASS_PANE);

        if (island == null) return;
        
        boolean isOwner = island.getOwner().equals(player.getUniqueId());
        int maxMembers = plugin.getConfigManager().getSettings().getMaxTeamSize();
        int currentMembers = island.getMembers().size() + 1; // +1 for owner

        // Info (Slot 4)
        ItemStack info = new ItemBuilder(Material.OAK_SIGN)
                .name(Component.text("Island Team", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(
                    Component.text("Members: ", NamedTextColor.GRAY)
                            .append(Component.text(currentMembers + "/" + maxMembers, NamedTextColor.WHITE)),
                    Component.empty(),
                    Component.text("Use ", NamedTextColor.GRAY).append(Component.text("/is team invite <player>", NamedTextColor.YELLOW))
                )
                .build();
        inventory.setItem(4, info);

        // Populate team members (Slots 19 to 34 roughly)
        int slot = 19;
        
        // Show owner first
        inventory.setItem(slot++, createPlayerHead(island.getOwner(), "Owner", isOwner, false));
        
        // Handle row breaks to keep it inside the borders
        for (UUID memberId : island.getMembers()) {
            if (slot == 26 || slot == 35) slot++; // skip border slots
            if (slot > 43) break; // max slots for members
            
            inventory.setItem(slot++, createPlayerHead(memberId, "Member", isOwner, memberId.equals(player.getUniqueId())));
        }

        // Action Button (Slot 49)
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name(Component.text("Back to Main Menu", NamedTextColor.WHITE, TextDecoration.BOLD))
                .build();
        inventory.setItem(49, back);
    }
    
    private ItemStack createPlayerHead(UUID uuid, String role, boolean viewerIsOwner, boolean isSelf) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(op);
            meta.displayName(Component.text(op.getName() != null ? op.getName() : "Unknown", NamedTextColor.YELLOW, TextDecoration.BOLD));
            
            ItemBuilder builder = new ItemBuilder(head).name(meta.displayName());
            builder.appendLore(Component.text("Role: ", NamedTextColor.GRAY).append(Component.text(role, NamedTextColor.WHITE)));
            builder.appendLore(Component.empty());
            
            if (role.equals("Member")) {
                if (viewerIsOwner) {
                    builder.appendLore(Component.text("▶ Shift-Left Click to Promote", NamedTextColor.GREEN));
                    builder.appendLore(Component.text("▶ Right Click to Kick", NamedTextColor.RED));
                } else if (isSelf) {
                    builder.appendLore(Component.text("▶ Right Click to Leave", NamedTextColor.RED));
                }
            } else if (role.equals("Owner") && isSelf) {
                builder.appendLore(Component.text("You are the owner.", NamedTextColor.GRAY));
            }
            
            head = builder.build();
            head.setItemMeta(meta); // set the skull owner back
        }
        return head;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player clicker = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getRawSlot();
        
        if (slot == 49) {
            clicker.openInventory(new MainMenuGUI(plugin, clicker).getInventory());
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        } 
        
        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null) return;
            
            String targetName = meta.getOwningPlayer().getName();
            if (targetName == null) return;
            
            boolean isOwner = island.getOwner().equals(clicker.getUniqueId());
            boolean isSelf = meta.getOwningPlayer().getUniqueId().equals(clicker.getUniqueId());
            boolean targetIsOwner = island.getOwner().equals(meta.getOwningPlayer().getUniqueId());
            
            if (targetIsOwner) return; // Cannot kick/promote the owner
            
            if (isOwner) {
                if (event.isShiftClick() && event.isLeftClick()) {
                    // Promote
                    clicker.closeInventory();
                    clicker.performCommand("is team promote " + targetName);
                } else if (event.isRightClick()) {
                    // Kick
                    clicker.closeInventory();
                    clicker.performCommand("is team kick " + targetName);
                }
            } else if (isSelf && event.isRightClick()) {
                // Leave
                clicker.closeInventory();
                clicker.performCommand("is team leave");
            }
        }
    }
}
