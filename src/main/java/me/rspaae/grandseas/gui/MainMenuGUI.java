package me.rspaae.grandseas.gui;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import me.rspaae.grandseas.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainMenuGUI extends AbstractGUI {
    
    private final GrandSeas plugin;
    private final Player player;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy");

    public MainMenuGUI(GrandSeas plugin, Player player) {
        super(54, "🏝 GrandSeas Control Panel");
        this.plugin = plugin;
        this.player = player;
        initializeItems(); 
    }

    @Override
    protected void initializeItems() {
        // Cyan glass outer border, black glass inner fill
        fillBorders(Material.CYAN_STAINED_GLASS_PANE);
        fillEmpty(Material.BLACK_STAINED_GLASS_PANE);

        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        String role = island != null && island.getOwner().equals(player.getUniqueId()) ? "Owner" : "Member";
        
        // Center info item with glow
        ItemBuilder infoBuilder = new ItemBuilder(Material.SUNFLOWER)
                .name(Component.text(island != null ? island.getName() : "Your Island", NamedTextColor.GOLD, TextDecoration.BOLD))
                .enchant(Enchantment.UNBREAKING, 1)
                .flag(ItemFlag.HIDE_ENCHANTS);
                
        if (island != null) {
            String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
            int members = island.getMembers().size() + 1;
            int maxMembers = plugin.getConfigManager().getSettings().getMaxTeamSize();
            int islandLevel = island.getIslandLevel();
            
            infoBuilder.lore(
                Component.empty(),
                Component.text("⭐ Island Level: ", NamedTextColor.GRAY).append(Component.text(islandLevel, NamedTextColor.GREEN, TextDecoration.BOLD)),
                Component.text("Owner: ", NamedTextColor.GRAY).append(Component.text(ownerName != null ? ownerName : "Unknown", NamedTextColor.WHITE)),
                Component.text("Your Role: ", NamedTextColor.GRAY).append(Component.text(role, NamedTextColor.WHITE)),
                Component.text("Theme: ", NamedTextColor.GRAY).append(Component.text(island.getTheme().getDisplayName(), NamedTextColor.YELLOW)),
                Component.empty(),
                Component.text("📏 Border: ", NamedTextColor.GRAY).append(Component.text("Lv." + island.getBorderLevel() + " (" + island.getBorderSize() + "x" + island.getBorderSize() + ")", NamedTextColor.AQUA)),
                Component.text("⛏ Generator: ", NamedTextColor.GRAY).append(Component.text("Lv." + island.getGeneratorLevel(), NamedTextColor.AQUA)),
                Component.text("👥 Members: ", NamedTextColor.GRAY).append(Component.text(members + "/" + maxMembers, NamedTextColor.AQUA)),
                Component.text("💰 Island Bank: ", NamedTextColor.GRAY).append(Component.text("$" + String.format("%,.1f", island.getBalance()), NamedTextColor.GOLD)),
                Component.empty(),
                Component.text("Created: ", NamedTextColor.GRAY).append(Component.text(DATE_FORMAT.format(new Date(island.getCreatedAt())), NamedTextColor.WHITE))
            );
        }
        inventory.setItem(13, infoBuilder.build());

        // Settings (Slot 29)
        ItemStack settings = new ItemBuilder(Material.COMPARATOR)
                .name(Component.text("Island Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(Component.text("Manage visitor rules, PvP, mobs and more.", NamedTextColor.GRAY))
                .build();
        inventory.setItem(29, settings);

        // Teleport Home (Slot 31)
        ItemStack home = new ItemBuilder(Material.ENDER_PEARL)
                .name(Component.text("Teleport Home", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(Component.text("Warp back to your island.", NamedTextColor.GRAY))
                .build();
        inventory.setItem(31, home);

        // Team Management (Slot 33)
        ItemStack team = new ItemBuilder(Material.PLAYER_HEAD)
                .name(Component.text("Team Management", NamedTextColor.BLUE, TextDecoration.BOLD))
                .lore(Component.text("Invite, kick, and manage your crew.", NamedTextColor.GRAY))
                .build();
        inventory.setItem(33, team);

        // Border Upgrades (Slot 38)
        ItemStack upgrade = new ItemBuilder(Material.BEACON)
                .name(Component.text("⬆ Island Upgrades", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(
                    Component.text("Expand your border & boost your generator.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("▶ Click to open", NamedTextColor.YELLOW)
                )
                .build();
        inventory.setItem(38, upgrade);

        // Leaderboard (Slot 40)
        ItemStack top = new ItemBuilder(Material.NETHER_STAR)
                .name(Component.text("🏆 Leaderboard", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(
                    Component.text("See the strongest islands on the server.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("▶ Click to open", NamedTextColor.YELLOW)
                )
                .build();
        inventory.setItem(40, top);

        // Island Info (Slot 42)
        ItemStack level = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(Component.text("⭐ Island Info", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(
                    Component.text("Check your island level, points & progress.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("▶ Click to open", NamedTextColor.YELLOW)
                )
                .build();
        inventory.setItem(42, level);
        
        // Close Button (Slot 49)
        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD))
                .build();
        inventory.setItem(49, close);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player clicker = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().contains("GLASS_PANE") || clicked.getType() == Material.SUNFLOWER) return;

        int slot = event.getRawSlot();
        
        switch (slot) {
            case 29: // Settings
                clicker.openInventory(new IslandSettingsGUI(plugin, clicker).getInventory());
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                break;
            case 31: // Home
                clicker.closeInventory();
                clicker.performCommand("is");
                break;
            case 33: // Team
                clicker.openInventory(new TeamMenuGUI(plugin, clicker).getInventory());
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                break;
            case 38: // Upgrades
                clicker.openInventory(new UpgradesGUI(plugin, clicker).getInventory());
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                break;
            case 40: // Leaderboard
                clicker.openInventory(new IslandTopGUI(plugin, clicker).getInventory());
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                break;
            case 42: // Info
                clicker.closeInventory();
                clicker.performCommand("is info");
                break;
            case 49: // Close
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                clicker.closeInventory();
                break;
        }
    }
}
