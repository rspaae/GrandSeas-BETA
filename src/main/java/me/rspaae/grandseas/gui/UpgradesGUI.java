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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class UpgradesGUI extends AbstractGUI {
    
    private final GrandSeas plugin;
    private final Player player;
    private final Island island;

    public UpgradesGUI(GrandSeas plugin, Player player) {
        super(27, "⬆ Island Upgrades");
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

        int islandLevel = island.getIslandLevel();

        // ==========================================
        // 1. Border Upgrade Button (Slot 11)
        // ==========================================
        int borderLvl = island.getBorderLevel();
        boolean borderMax = borderLvl >= Island.getMaxBorderLevel();
        int nextBorderLvl = borderLvl + 1;
        
        long requiredBorderLevel = 0;
        double requiredBorderBalance = 0.0;
        
        if (!borderMax) {
            String reqStr = plugin.getConfigManager().getSettings().getUpgradeBorderReq(nextBorderLvl);
            try {
                String[] split = reqStr.split(",");
                requiredBorderLevel = Long.parseLong(split[0]);
                if (split.length > 1) requiredBorderBalance = Double.parseDouble(split[1]);
            } catch (Exception e) {
                requiredBorderLevel = 99;
                requiredBorderBalance = 999999999.0;
            }
        }

        boolean borderLevelEnough = islandLevel >= requiredBorderLevel;
        boolean borderBalanceEnough = island.getBalance() >= requiredBorderBalance;
        boolean borderAfford = borderLevelEnough && borderBalanceEnough;

        ItemBuilder borderBuilder;
        if (borderMax) {
            borderBuilder = new ItemBuilder(Material.BEACON)
                    .name(Component.text("Border: MAX LEVEL", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .lore(
                        Component.empty(),
                        Component.text("Current Size: ", NamedTextColor.GRAY).append(Component.text(island.getBorderSize() + "x" + island.getBorderSize(), NamedTextColor.AQUA)),
                        Component.text("Island Level: ", NamedTextColor.GRAY).append(Component.text(islandLevel, NamedTextColor.YELLOW)),
                        Component.empty(),
                        Component.text("You've maxed out this upgrade!", NamedTextColor.GRAY)
                    );
        } else {
            borderBuilder = new ItemBuilder(borderAfford ? Material.DIAMOND_BLOCK : Material.COAL_BLOCK)
                    .name(Component.text("Upgrade Border to Level " + nextBorderLvl, borderAfford ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                    .lore(
                        Component.empty(),
                        Component.text("Current Size: ", NamedTextColor.GRAY).append(Component.text(island.getBorderSize() + "x" + island.getBorderSize(), NamedTextColor.AQUA)),
                        Component.text("Next Size: ", NamedTextColor.GRAY).append(Component.text((island.getBorderSize() + Island.getBorderSizePerLevel()) + "x" + (island.getBorderSize() + Island.getBorderSizePerLevel()), NamedTextColor.GREEN)),
                        Component.empty(),
                        Component.text("Requirements:", NamedTextColor.GOLD),
                        Component.text("▶ Island Level: ", NamedTextColor.GRAY)
                            .append(Component.text(islandLevel, borderLevelEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" / ", NamedTextColor.GRAY))
                            .append(Component.text(requiredBorderLevel, NamedTextColor.YELLOW)),
                        Component.text("▶ Bank Cost: ", NamedTextColor.GRAY)
                            .append(Component.text("$" + String.format("%,.1f", island.getBalance()), borderBalanceEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" / ", NamedTextColor.GRAY))
                            .append(Component.text("$" + String.format("%,.1f", requiredBorderBalance), NamedTextColor.GOLD)),
                        Component.empty()
                    );
                    
            if (borderAfford) borderBuilder.appendLore(Component.text("▶ Click to upgrade!", NamedTextColor.AQUA));
            else if (!borderLevelEnough) borderBuilder.appendLore(Component.text("❌ Your island level isn't high enough!", NamedTextColor.RED));
            else borderBuilder.appendLore(Component.text("❌ Your bank balance is insufficient!", NamedTextColor.RED));
        }
        inventory.setItem(11, borderBuilder.build());

        // ==========================================
        // 2. Ore Generator Upgrade Button (Slot 15)
        // ==========================================
        int genLvl = island.getGeneratorLevel();
        boolean genMax = genLvl >= Island.getMaxGeneratorLevel();
        int nextGenLvl = genLvl + 1;
        
        long requiredGenLevel = 0;
        double requiredGenBalance = 0.0;
        
        if (!genMax) {
            String reqStr = plugin.getConfigManager().getSettings().getUpgradeGeneratorReq(nextGenLvl);
            try {
                String[] split = reqStr.split(",");
                requiredGenLevel = Long.parseLong(split[0]);
                if (split.length > 1) requiredGenBalance = Double.parseDouble(split[1]);
            } catch (Exception e) {
                requiredGenLevel = 99;
                requiredGenBalance = 999999999.0;
            }
        }

        boolean genLevelEnough = islandLevel >= requiredGenLevel;
        boolean genBalanceEnough = island.getBalance() >= requiredGenBalance;
        boolean genAfford = genLevelEnough && genBalanceEnough;

        ItemBuilder genBuilder;
        if (genMax) {
            genBuilder = new ItemBuilder(Material.EMERALD_ORE)
                    .name(Component.text("Ore Generator: MAX LEVEL", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .lore(
                        Component.empty(),
                        Component.text("Current Level: ", NamedTextColor.GRAY).append(Component.text(genLvl, NamedTextColor.AQUA)),
                        Component.text("Island Level: ", NamedTextColor.GRAY).append(Component.text(islandLevel, NamedTextColor.YELLOW)),
                        Component.empty(),
                        Component.text("You've maxed out this upgrade!", NamedTextColor.GRAY)
                    );
        } else {
            genBuilder = new ItemBuilder(genAfford ? Material.GOLD_ORE : Material.IRON_ORE)
                    .name(Component.text("Upgrade Ore Generator to Level " + nextGenLvl, genAfford ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                    .lore(
                        Component.empty(),
                        Component.text("Increases the spawn rate of valuable", NamedTextColor.GRAY),
                        Component.text("ores from your cobblestone generator.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Requirements:", NamedTextColor.GOLD),
                        Component.text("▶ Island Level: ", NamedTextColor.GRAY)
                            .append(Component.text(islandLevel, genLevelEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" / ", NamedTextColor.GRAY))
                            .append(Component.text(requiredGenLevel, NamedTextColor.YELLOW)),
                        Component.text("▶ Bank Cost: ", NamedTextColor.GRAY)
                            .append(Component.text("$" + String.format("%,.1f", island.getBalance()), genBalanceEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" / ", NamedTextColor.GRAY))
                            .append(Component.text("$" + String.format("%,.1f", requiredGenBalance), NamedTextColor.GOLD)),
                        Component.empty()
                    );
                    
            if (genAfford) genBuilder.appendLore(Component.text("▶ Click to upgrade!", NamedTextColor.AQUA));
            else if (!genLevelEnough) genBuilder.appendLore(Component.text("❌ Your island level isn't high enough!", NamedTextColor.RED));
            else genBuilder.appendLore(Component.text("❌ Your bank balance is insufficient!", NamedTextColor.RED));
        }
        inventory.setItem(15, genBuilder.build());

        // Back Button (Slot 22)
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name(Component.text("Back to Main Menu", NamedTextColor.WHITE, TextDecoration.BOLD))
                .build();
        inventory.setItem(22, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player clicker = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().contains("GLASS_PANE")) return;

        int slot = event.getRawSlot();
        
        if (slot == 22) {
            clicker.openInventory(new MainMenuGUI(plugin, clicker).getInventory());
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        if (island == null) return;
        
        if (!island.getOwner().equals(clicker.getUniqueId())) {
            clicker.sendMessage(Component.text("Only the island owner can purchase upgrades!", NamedTextColor.RED));
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (slot == 11) { // Border Upgrade
            int currentLevel = island.getBorderLevel();
            if (currentLevel >= Island.getMaxBorderLevel()) {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            String reqStr = plugin.getConfigManager().getSettings().getUpgradeBorderReq(currentLevel + 1);
            long requiredLevel = 99;
            double requiredBalance = 999999999.0;
            try {
                String[] split = reqStr.split(",");
                requiredLevel = Long.parseLong(split[0]);
                if (split.length > 1) requiredBalance = Double.parseDouble(split[1]);
            } catch (Exception ignored) {}

            if (island.getIslandLevel() >= requiredLevel && island.getBalance() >= requiredBalance) {
                boolean upgraded = island.upgradeBorder();
                if (upgraded) {
                    island.removeBalance(requiredBalance);
                    plugin.getIslandManager().saveIslands();

                    for (java.util.UUID memberId : island.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && member.isOnline() && island.isWithinBorder(member.getLocation())) {
                            plugin.getIslandManager().updatePlayerBorder(member, member.getLocation());
                        }
                    }
                    Player owner = Bukkit.getPlayer(island.getOwner());
                    if (owner != null && owner.isOnline() && island.isWithinBorder(owner.getLocation())) {
                        plugin.getIslandManager().updatePlayerBorder(owner, owner.getLocation());
                    }
                    
                    clicker.playSound(clicker.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    clicker.sendMessage(Component.text("🎊 Border upgraded to Level " + island.getBorderLevel() + "!", NamedTextColor.GREEN));
                    initializeItems();
                } else {
                    clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    clicker.sendMessage(Component.text("Border has reached the maximum level!", NamedTextColor.RED));
                }
            } else {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        } else if (slot == 15) { // Ore Generator Upgrade
            int currentLevel = island.getGeneratorLevel();
            if (currentLevel >= Island.getMaxGeneratorLevel()) {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            String reqStr = plugin.getConfigManager().getSettings().getUpgradeGeneratorReq(currentLevel + 1);
            long requiredLevel = 99;
            double requiredBalance = 999999999.0;
            try {
                String[] split = reqStr.split(",");
                requiredLevel = Long.parseLong(split[0]);
                if (split.length > 1) requiredBalance = Double.parseDouble(split[1]);
            } catch (Exception ignored) {}

            if (island.getIslandLevel() >= requiredLevel && island.getBalance() >= requiredBalance) {
                boolean upgraded = island.upgradeGenerator();
                if (upgraded) {
                    island.removeBalance(requiredBalance);
                    plugin.getIslandManager().saveIslands();
                    clicker.playSound(clicker.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    clicker.sendMessage(Component.text("🎊 Ore Generator upgraded to Level " + island.getGeneratorLevel() + "!", NamedTextColor.GREEN));
                    initializeItems();
                } else {
                    clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    clicker.sendMessage(Component.text("Generator has reached the maximum level!", NamedTextColor.RED));
                }
            } else {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
}
