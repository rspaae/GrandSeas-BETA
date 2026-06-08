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
import org.bukkit.inventory.ItemStack;

public class IslandSettingsGUI extends AbstractGUI {
    
    private final GrandSeas plugin;
    private final Player player;
    private final Island island;

    public IslandSettingsGUI(GrandSeas plugin, Player player) {
        super(54, "⚙ Island Settings");
        this.plugin = plugin;
        this.player = player;
        this.island = plugin.getIslandManager().getIsland(player.getUniqueId());
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        fillBorders(Material.BLACK_STAINED_GLASS_PANE);
        fillEmpty(Material.GRAY_STAINED_GLASS_PANE);

        if (island == null) return;

        // Visitor Teleporting (Slot 20)
        boolean allowVisitors = island.isAllowVisitors();
        ItemStack visitor = new ItemBuilder(Material.ENDER_PEARL)
                .name(Component.text("Visitor Teleporting", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Allow others to /is visit your island?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(allowVisitors ? "Allowed ✔" : "Denied ✘", allowVisitors ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(20, visitor);

        // Mob Spawning (Slot 22)
        boolean mobSpawning = island.isMobSpawning();
        ItemStack mobs = new ItemBuilder(Material.CREEPER_HEAD)
                .name(Component.text("Mob Spawning", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Allow hostile mobs to spawn naturally?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(mobSpawning ? "Allowed ✔" : "Denied ✘", mobSpawning ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(22, mobs);

        // PvP (Slot 24)
        boolean pvp = island.isPvpEnabled();
        ItemStack pvpItem = new ItemBuilder(Material.IRON_SWORD)
                .name(Component.text("PvP Combat", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Allow players to fight each other?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(pvp ? "Allowed ✔" : "Denied ✘", pvp ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(24, pvpItem);

        // Show Balance (Slot 30)
        boolean showBal = island.isShowBalanceOnTop();
        ItemStack balItem = new ItemBuilder(Material.GOLD_INGOT)
                .name(Component.text("Leaderboard: Show Bank Balance", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Show your island bank on /is top?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(showBal ? "Shown ✔" : "Hidden ✘", showBal ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(30, balItem);

        // Show Members (Slot 32)
        boolean showMem = island.isShowMembersOnTop();
        ItemStack memItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name(Component.text("Leaderboard: Show Members", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Show your team members on /is top?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(showMem ? "Shown ✔" : "Hidden ✘", showMem ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(32, memItem);

        // Info (Slot 4)
        ItemStack info = new ItemBuilder(Material.BOOK)
                .name(Component.text("Island Settings", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Only the island owner can change these.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(4, info);

        // Trusted Build (Slot 38)
        boolean trustedBuild = (Boolean) island.getSettings().getOrDefault(Island.SETTING_TRUSTED_BUILD, true);
        ItemStack trustedBuildItem = new ItemBuilder(Material.DIAMOND_PICKAXE)
                .name(Component.text("Trusted: Can Build", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Can trusted players place and break blocks?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(trustedBuild ? "Allowed ✔" : "Denied ✘", trustedBuild ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(38, trustedBuildItem);

        // Trusted Interact (Slot 42)
        boolean trustedInteract = (Boolean) island.getSettings().getOrDefault(Island.SETTING_TRUSTED_INTERACT, true);
        ItemStack trustedInteractItem = new ItemBuilder(Material.CHEST)
                .name(Component.text("Trusted: Can Interact", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                    Component.text("Can trusted players open chests, buttons", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("and point storage blocks?", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.GRAY)
                            .append(Component.text(trustedInteract ? "Allowed ✔" : "Denied ✘", trustedInteract ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.empty(),
                    Component.text("▶ Click to toggle", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                )
                .build();
        inventory.setItem(42, trustedInteractItem);

        // Back Button (Slot 49)
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name(Component.text("Back to Main Menu", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .build();
        inventory.setItem(49, back);
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

        if (island == null) return;
        
        if (!island.getOwner().equals(clicker.getUniqueId())) {
            clicker.sendMessage(Component.text("Only the island owner can change these settings!", NamedTextColor.RED));
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        boolean toggled = false;
        String settingName = "";
        
        if (slot == 20) {
            toggled = island.toggleSetting(Island.SETTING_ALLOW_VISITORS, true);
            settingName = "Visitor Teleporting";
        } else if (slot == 22) {
            toggled = island.toggleSetting(Island.SETTING_MOB_SPAWNING, false);
            settingName = "Mob Spawning";
        } else if (slot == 24) {
            toggled = island.toggleSetting(Island.SETTING_PVP, false);
            settingName = "PvP Combat";
        } else if (slot == 30) {
            toggled = island.toggleSetting(Island.SETTING_SHOW_BALANCE, true);
            settingName = "Show Balance";
        } else if (slot == 32) {
            toggled = island.toggleSetting(Island.SETTING_SHOW_MEMBERS, true);
            settingName = "Show Members";
        } else if (slot == 38) {
            toggled = island.toggleSetting(Island.SETTING_TRUSTED_BUILD, true);
            settingName = "Trusted Build";
        } else if (slot == 42) {
            toggled = island.toggleSetting(Island.SETTING_TRUSTED_INTERACT, true);
            settingName = "Trusted Interact";
        }

        if (!settingName.isEmpty()) {
            plugin.getIslandManager().saveIslands();
            
            // Re-render GUI
            initializeItems();
            
            clicker.playSound(clicker.getLocation(), toggled ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.2f);
        }
    }
}
