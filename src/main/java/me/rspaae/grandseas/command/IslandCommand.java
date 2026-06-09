package me.rspaae.grandseas.command;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.gui.ConfirmationGUI;
import me.rspaae.grandseas.gui.MainMenuGUI;
import me.rspaae.grandseas.gui.ThemeSelectionGUI;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class IslandCommand implements TabExecutor {
    private final GrandSeas plugin;
    // Map of UUID -> timestamp of last deletion
    private final Map<UUID, Long> deleteCooldowns = new HashMap<>();

    public IslandCommand(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
            if (island == null) {
                // Cek cooldown 5 menit
                long cooldownTime = 5 * 60 * 1000L; // 5 minutes in ms
                if (deleteCooldowns.containsKey(player.getUniqueId())) {
                    long timeLeft = (deleteCooldowns.get(player.getUniqueId()) + cooldownTime) - System.currentTimeMillis();
                    if (timeLeft > 0) {
                        long minutes = timeLeft / 60000;
                        long seconds = (timeLeft % 60000) / 1000;
                        player.sendMessage(Component.text("⏳ Kamu baru saja menghapus pulaumu! Tunggu " + minutes + " menit " + seconds + " detik sebelum membuat pulau baru.", NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return true;
                    }
                }

                // No island — show theme selection
                ThemeSelectionGUI gui = new ThemeSelectionGUI(plugin);
                player.openInventory(gui.getInventory());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            } else {
                // Has island — open main menu instead of just teleporting
                MainMenuGUI gui = new MainMenuGUI(plugin, player);
                player.openInventory(gui.getInventory());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "settings":
                handleSettings(player);
                break;
            case "upgrades":
                handleUpgrades(player);
                break;
            case "balance":
                handleBalance(player);
                break;
            case "rename":
                handleRename(player, args);
                break;
            case "chat":
                handleChat(player);
                break;
            case "pay":
                handlePay(player, args);
                break;
            case "transfer":
                handleTransferOwnership(player, args);
                break;
            case "visit":
                handleVisit(player, args);
                break;
            case "ban":
                handleBan(player, args);
                break;
            case "unban":
                handleUnban(player, args);
                break;
            case "banlist":
                handleBanList(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "trust":
                handleTrust(player, args);
                break;
            case "untrust":
                handleUntrust(player, args);
                break;
            case "trustlist":
                handleTrustList(player);
                break;
            case "coop":
                handleCoop(player, args);
                break;
            case "team":
                handleTeam(player, args);
                break;
            case "reload":
                handleReload(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "top":
                handleTop(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage(Component.text("Unknown subcommand. Use ", NamedTextColor.RED)
                        .append(Component.text("/is help", NamedTextColor.YELLOW))
                        .append(Component.text(" for a list of commands.", NamedTextColor.RED)));
                break;
        }

        return true;
    }

    // ==================== Menus (Settings & Upgrades) ====================

    private void handleSettings(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island!", NamedTextColor.RED));
            return;
        }
        MainMenuGUI gui = new MainMenuGUI(plugin, player);
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private void handleUpgrades(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island!", NamedTextColor.RED));
            return;
        }
        me.rspaae.grandseas.gui.UpgradesGUI gui = new me.rspaae.grandseas.gui.UpgradesGUI(plugin, player);
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ==================== Economy (Island) ====================

    private void handleBalance(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island! Use ", NamedTextColor.RED)
                    .append(Component.text("/is create", NamedTextColor.YELLOW)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        player.sendMessage(Component.text("💳 Island Bank Balance: ", NamedTextColor.GRAY)
                .append(Component.text("$" + String.format("%,.2f", island.getBalance()), NamedTextColor.GOLD)));
    }

    private void handlePay(Player player, String[] args) {
        Island senderIsland = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (senderIsland == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }

        // Only owner can transfer funds
        if (!senderIsland.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("❌ Only the island owner can transfer funds!", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /is pay <player> <amount>", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text("❌ Player " + targetName + " not found.", NamedTextColor.RED));
            return;
        }

        Island targetIsland = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (targetIsland == null) {
            player.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }

        if (senderIsland.getOwner().equals(targetIsland.getOwner())) {
            player.sendMessage(Component.text("❌ You can't pay your own island.", NamedTextColor.RED));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
            return;
        }

        if (senderIsland.getBalance() >= amount) {
            senderIsland.removeBalance(amount);
            targetIsland.addBalance(amount);
            plugin.getIslandManager().saveIslands();

            player.sendMessage(Component.text("✅ Successfully paid $" + String.format("%,.2f", amount) + " to " + targetIsland.getName() + "'s bank.", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            Player targetOnline = Bukkit.getPlayer(target.getUniqueId());
            if (targetOnline != null && targetOnline.isOnline()) {
                targetOnline.sendMessage(Component.text("💸 Your island bank received $" + String.format("%,.2f", amount) + " from " + senderIsland.getName() + ".", NamedTextColor.GREEN));
                targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        } else {
            player.sendMessage(Component.text("❌ Insufficient island bank balance!", NamedTextColor.RED));
        }
    }

    private void handleTransferOwnership(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is transfer <player>", NamedTextColor.RED));
            return;
        }

        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ Only the island owner can transfer ownership!", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text("❌ Player " + targetName + " not found.", NamedTextColor.RED));
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("❌ You are already the owner of this island!", NamedTextColor.RED));
            return;
        }

        if (!island.isMemberOrOwner(targetUUID)) {
            player.sendMessage(Component.text("❌ Player " + targetName + " must be a member of your island first!", NamedTextColor.RED));
            return;
        }

        ConfirmationGUI gui = new ConfirmationGUI(
                "Transfer Ownership",
                "" + targetName + " will become the new owner!",
                () -> {
                    boolean success = plugin.getIslandManager().promoteToOwner(player.getUniqueId(), targetUUID);
                    if (success) {
                        player.sendMessage(Component.text("👑 Island ownership transferred to ", NamedTextColor.GREEN)
                                .append(Component.text(targetName, NamedTextColor.GOLD)));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                        Player targetOnline = Bukkit.getPlayer(targetUUID);
                        if (targetOnline != null && targetOnline.isOnline()) {
                            targetOnline.sendMessage(Component.text("👑 You are now the new island owner!", NamedTextColor.GOLD));
                            targetOnline.playSound(targetOnline.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                        }
                    } else {
                        player.sendMessage(Component.text("❌ Failed to transfer ownership.", NamedTextColor.RED));
                    }
                },
                () -> {
                    player.sendMessage(Component.text("✅ Ownership transfer cancelled.", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
        );
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    private void handleRename(Player player, String[] args) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("❌ Only the island owner can rename the island!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is rename <new_name>", NamedTextColor.RED));
            return;
        }

        StringBuilder newNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            newNameBuilder.append(args[i]).append(" ");
        }
        String newName = newNameBuilder.toString().trim();
        
        // Basic filter
        if (newName.length() > 24) {
            player.sendMessage(Component.text("❌ Island name max length is 24 characters!", NamedTextColor.RED));
            return;
        }
        
        // Replace color codes
        newName = org.bukkit.ChatColor.translateAlternateColorCodes('&', newName);

        island.setName(newName);
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("✅ Island renamed to: " + newName, NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
    }

    private void handleChat(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }

        boolean isEnabled = plugin.getIslandManager().toggleIslandChat(player.getUniqueId());
        if (isEnabled) {
            player.sendMessage(Component.text("💬 Island Chat: ", NamedTextColor.AQUA).append(Component.text("ENABLED", NamedTextColor.GREEN, TextDecoration.BOLD)));
            player.sendMessage(Component.text("All your messages will now only be seen by your island members.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("💬 Island Chat: ", NamedTextColor.AQUA).append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
            player.sendMessage(Component.text("You are back in global chat.", NamedTextColor.GRAY));
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
    }

    // ==================== Home & SetHome ====================

    private void handleHome(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island! Use ", NamedTextColor.RED)
                    .append(Component.text("/is create", NamedTextColor.YELLOW))
                    .append(Component.text(" to make one.", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        Location dest = island.getHome() != null ? island.getHome() : island.getCenter();
        if (dest != null) {
            player.teleport(dest);
            plugin.getIslandManager().updatePlayerBorder(player, dest);
            player.sendMessage(Component.text("🏝 Teleported to your island!", NamedTextColor.GREEN));
            player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } else {
            player.sendMessage(Component.text("❌ Island location not found.", NamedTextColor.RED));
        }
    }

    private void handleSetHome(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        // Harus berada di dalam island world
        if (!player.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) {
            player.sendMessage(Component.text("❌ You must be in the island world to set home!", NamedTextColor.RED));
            return;
        }
        // Harus berada di dalam border islandnya sendiri
        Island islandAt = plugin.getIslandManager().getIslandAt(player.getLocation());
        if (islandAt == null || !islandAt.getOwner().equals(island.getOwner())) {
            player.sendMessage(Component.text("❌ You must be inside your own island border!", NamedTextColor.RED));
            return;
        }
        island.setHome(player.getLocation());
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("✅ Island home set to this location!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
    }

    // ==================== Reload ====================

    private void handleReload(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Island ownerIsland = plugin.getIslandManager().getIslandByOwner(island.getOwner());
        if (ownerIsland == null) return;

        new me.rspaae.grandseas.task.IslandCalculationTask(plugin, ownerIsland, player).start();
    }

    // ==================== Info ====================

    private void handleInfo(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island! Use ", NamedTextColor.RED)
                    .append(Component.text("/is create", NamedTextColor.YELLOW))
                    .append(Component.text(" to make one.", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Open the modern GUI instead of sending chat/title
        me.rspaae.grandseas.gui.IslandLevelGUI gui = new me.rspaae.grandseas.gui.IslandLevelGUI(plugin, player, island);
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    // ==================== Top ====================

    private void handleTop(Player player) {
        player.openInventory(new me.rspaae.grandseas.gui.IslandTopGUI(plugin, player).getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    // ==================== Delete ====================

    private void handleDelete(Player player) {
        // Cek cooldown 5 menit
        long cooldownTime = 5 * 60 * 1000L; // 5 minutes in ms
        if (deleteCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (deleteCooldowns.get(player.getUniqueId()) + cooldownTime) - System.currentTimeMillis();
            if (timeLeft > 0) {
                long minutes = timeLeft / 60000;
                long seconds = (timeLeft % 60000) / 1000;
                player.sendMessage(Component.text("⏳ You must wait " + minutes + "m " + seconds + "s before deleting your island again!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't own an island!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Open confirmation GUI
        ConfirmationGUI gui = new ConfirmationGUI(
                "Delete Island",
                "This will permanently destroy your island and remove all members!",
                () -> {
                    plugin.getIslandManager().deleteIsland(player.getUniqueId());
                    deleteCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    player.sendMessage(Component.text("⚠ Your island has been deleted.", NamedTextColor.YELLOW));
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                },
                () -> {
                    player.sendMessage(Component.text("✅ Island deletion cancelled.", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
        );
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    // ==================== Team Subcommands ====================

    private void handleTeam(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/is team <invite|accept|deny|kick|leave|promote>", NamedTextColor.YELLOW)));
            return;
        }
        
        String teamSub = args[1].toLowerCase();
        switch (teamSub) {
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "deny":
                handleDeny(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            default:
                player.sendMessage(Component.text("Unknown team command. Usage: ", NamedTextColor.RED)
                        .append(Component.text("/is team <invite|accept|deny|kick|leave|promote>", NamedTextColor.YELLOW)));
                break;
        }
    }

    // ==================== Invite ====================

    private void handleInvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/is team invite <player>", NamedTextColor.YELLOW)));
            return;
        }

        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("Only the island owner can invite players!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player ", NamedTextColor.RED)
                    .append(Component.text(targetName, NamedTextColor.YELLOW))
                    .append(Component.text(" not found or not online.", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can't invite yourself!", NamedTextColor.RED));
            return;
        }

        // Check if target already has an island
        Island targetIsland = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (targetIsland != null) {
            player.sendMessage(Component.text("That player already has an island.", NamedTextColor.RED));
            return;
        }

        // Check if already has pending invite
        if (plugin.getIslandManager().hasPendingInvite(target.getUniqueId())) {
            player.sendMessage(Component.text("That player already has a pending invite.", NamedTextColor.RED));
            return;
        }

        // Check team size
        int maxMembers = plugin.getConfigManager().getSettings().getMaxTeamSize();
        if (island.getMembers().size() >= maxMembers) {
            player.sendMessage(Component.text("Your island has reached the maximum member limit (", NamedTextColor.RED)
                    .append(Component.text(String.valueOf(maxMembers), NamedTextColor.YELLOW))
                    .append(Component.text(").", NamedTextColor.RED)));
            return;
        }

        boolean success = plugin.getIslandManager().invitePlayer(player, target);
        if (success) {
            player.sendMessage(Component.text("✉ Invitation sent to ", NamedTextColor.GREEN)
                    .append(Component.text(target.getName(), NamedTextColor.WHITE))
                    .append(Component.text("! They have ", NamedTextColor.GREEN))
                    .append(Component.text("60 seconds", NamedTextColor.YELLOW))
                    .append(Component.text(" to accept.", NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // Notify target with fancy message
            target.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            target.sendMessage(Component.text("✉ ", NamedTextColor.GREEN)
                    .append(Component.text(player.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" invited you to their island!", NamedTextColor.GREEN)));
            target.sendMessage(Component.text("Type ", NamedTextColor.YELLOW)
                    .append(Component.text("/is team accept", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" to join or ", NamedTextColor.YELLOW)
                    .append(Component.text("/is team deny", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" to decline.", NamedTextColor.YELLOW))));
            target.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }

    // ==================== Accept ====================

    private void handleAccept(Player player) {
        if (!plugin.getIslandManager().hasPendingInvite(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have any pending invitations.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        UUID inviterUuid = plugin.getIslandManager().getInviter(player.getUniqueId());
        boolean success = plugin.getIslandManager().acceptInvite(player);

        if (success && inviterUuid != null) {
            Player inviter = Bukkit.getPlayer(inviterUuid);
            String inviterName = inviter != null ? inviter.getName() : "Unknown";

            player.sendMessage(Component.text("✅ You joined ", NamedTextColor.GREEN)
                    .append(Component.text(inviterName, NamedTextColor.WHITE))
                    .append(Component.text("'s island!", NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);

            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(Component.text("✅ ", NamedTextColor.GREEN)
                        .append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" has joined your island!", NamedTextColor.GREEN)));
                inviter.playSound(inviter.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
            }
        } else {
            player.sendMessage(Component.text("Failed to accept invitation. It may have expired or the island is full.", NamedTextColor.RED));
        }
    }

    // ==================== Deny ====================

    private void handleDeny(Player player) {
        if (!plugin.getIslandManager().hasPendingInvite(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have any pending invitations.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        UUID inviterUuid = plugin.getIslandManager().getInviter(player.getUniqueId());
        plugin.getIslandManager().denyInvite(player.getUniqueId());

        player.sendMessage(Component.text("You declined the invitation.", NamedTextColor.YELLOW));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);

        if (inviterUuid != null) {
            Player inviter = Bukkit.getPlayer(inviterUuid);
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(Component.text(player.getName(), NamedTextColor.YELLOW)
                        .append(Component.text(" declined your invitation.", NamedTextColor.YELLOW)));
            }
        }
    }

    // ==================== Kick ====================

    private void handleKick(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/is team kick <player>", NamedTextColor.YELLOW)));
            return;
        }

        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("Only the island owner can kick members!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            // Try to find offline player in members
            targetUuid = null;
            for (UUID memberId : island.getMembers()) {
                if (Bukkit.getOfflinePlayer(memberId).getName() != null &&
                        Bukkit.getOfflinePlayer(memberId).getName().equalsIgnoreCase(targetName)) {
                    targetUuid = memberId;
                    break;
                }
            }
        }

        if (targetUuid == null) {
            player.sendMessage(Component.text(targetName + " is not a member of your island.", NamedTextColor.RED));
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can't kick yourself! Use ", NamedTextColor.RED)
                    .append(Component.text("/is team leave", NamedTextColor.YELLOW))
                    .append(Component.text(" instead.", NamedTextColor.RED)));
            return;
        }

        boolean success = plugin.getIslandManager().kickMember(player.getUniqueId(), targetUuid);
        if (success) {
            player.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW)
                    .append(Component.text(targetName, NamedTextColor.WHITE))
                    .append(Component.text(" has been kicked from your island.", NamedTextColor.YELLOW)));
            player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);

            if (target != null && target.isOnline()) {
                target.sendMessage(Component.text("⚠ You have been kicked from ", NamedTextColor.RED)
                        .append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text("'s island.", NamedTextColor.RED)));
                target.playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);
            }
        } else {
            player.sendMessage(Component.text(targetName + " is not a member of your island.", NamedTextColor.RED));
        }
    }

    // ==================== Leave ====================

    private void handleLeave(Player player) {
        Island island = plugin.getIslandManager().getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You're not part of any island!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You're the island owner! Use ", NamedTextColor.RED)
                    .append(Component.text("/is team promote <player>", NamedTextColor.YELLOW))
                    .append(Component.text(" first, or ", NamedTextColor.RED))
                    .append(Component.text("/is delete", NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.RED)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        UUID ownerUuid = island.getOwner();
        String ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
        if (ownerName == null) ownerName = "Unknown";

        // Confirmation
        ConfirmationGUI gui = new ConfirmationGUI(
                "Leave Island",
                "You will lose access to " + ownerName + "'s island.",
                () -> {
                    boolean success = plugin.getIslandManager().leaveIsland(player.getUniqueId());
                    if (success) {
                        player.sendMessage(Component.text("You left ", NamedTextColor.YELLOW)
                                .append(Component.text(Bukkit.getOfflinePlayer(ownerUuid).getName() != null ?
                                        Bukkit.getOfflinePlayer(ownerUuid).getName() : "Unknown", NamedTextColor.WHITE))
                                .append(Component.text("'s island.", NamedTextColor.YELLOW)));
                        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);

                        Player owner = Bukkit.getPlayer(ownerUuid);
                        if (owner != null && owner.isOnline()) {
                            owner.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW)
                                    .append(Component.text(player.getName(), NamedTextColor.WHITE))
                                    .append(Component.text(" has left your island.", NamedTextColor.YELLOW)));
                        }
                    }
                },
                () -> {
                    player.sendMessage(Component.text("Cancelled.", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
        );
        player.openInventory(gui.getInventory());
    }

    // ==================== Promote ====================

    private void handlePromote(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text("/is team promote <player>", NamedTextColor.YELLOW)));
            return;
        }

        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("Only the island owner can promote members!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = null;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            for (UUID memberId : island.getMembers()) {
                if (Bukkit.getOfflinePlayer(memberId).getName() != null &&
                        Bukkit.getOfflinePlayer(memberId).getName().equalsIgnoreCase(targetName)) {
                    targetUuid = memberId;
                    break;
                }
            }
        }

        if (targetUuid == null || !island.getMembers().contains(targetUuid)) {
            player.sendMessage(Component.text(targetName + " is not a member of your island.", NamedTextColor.RED));
            return;
        }

        UUID finalTargetUuid = targetUuid;
        ConfirmationGUI gui = new ConfirmationGUI(
                "Transfer Ownership",
                "This will make " + targetName + " the new island owner!",
                () -> {
                    boolean success = plugin.getIslandManager().promoteToOwner(player.getUniqueId(), finalTargetUuid);
                    if (success) {
                        player.sendMessage(Component.text("👑 ", NamedTextColor.GREEN)
                                .append(Component.text(targetName, NamedTextColor.WHITE))
                                .append(Component.text(" is now the island owner!", NamedTextColor.GREEN)));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);

                        Player targetPlayer = Bukkit.getPlayer(finalTargetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.sendMessage(Component.text("👑 You are now the island owner!", NamedTextColor.GREEN));
                            targetPlayer.playSound(targetPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                        }
                    }
                },
                () -> {
                    player.sendMessage(Component.text("Promotion cancelled.", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
        );
        player.openInventory(gui.getInventory());
    }

    // ==================== Visit ====================

    private void handleVisit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is visit <player>", NamedTextColor.RED));
            return;
        }
        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island targetIsland = plugin.getIslandManager().getIslandByOwner(target.getUniqueId());

        if (targetIsland == null) {
            player.sendMessage(Component.text("❌ Player " + targetName + " doesn't have an island.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (targetIsland.isBanned(player.getUniqueId())) {
            player.sendMessage(Component.text("⛔ You are banned from " + targetName + "'s island!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (!targetIsland.isAllowVisitors() && !targetIsland.isMemberOrOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("🔒 " + targetName + "'s island is currently closed to visitors.", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        plugin.getIslandManager().teleportToIsland(player, targetIsland);
        player.sendMessage(Component.text("🏝 Visiting " + targetName + "'s island!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Notify owner if online
        Player targetOnline = Bukkit.getPlayer(target.getUniqueId());
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(Component.text("👁 " + player.getName() + " is visiting your island!", NamedTextColor.AQUA));
        }
    }

    // ==================== Ban ====================

    private void handleBan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is ban <player>", NamedTextColor.RED));
            return;
        }
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("❌ You can't ban yourself!", NamedTextColor.RED));
            return;
        }
        if (island.isBanned(targetUUID)) {
            player.sendMessage(Component.text("⚠ " + targetName + " is already banned.", NamedTextColor.YELLOW));
            return;
        }

        island.banPlayer(targetUUID);
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("🚫 " + targetName + " has been banned from your island.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);

        // Kick the banned player if currently on the island
        Player targetOnline = Bukkit.getPlayer(targetUUID);
        if (targetOnline != null && targetOnline.isOnline()) {
            Island playerIsland = plugin.getIslandManager().getIslandAt(targetOnline.getLocation());
            if (playerIsland != null && playerIsland.getOwner().equals(player.getUniqueId())) {
                Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                targetOnline.teleport(spawn);
                targetOnline.sendMessage(Component.text("⛔ You were banned and kicked from " + player.getName() + "'s island!", NamedTextColor.RED));
                targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void handleUnban(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is unban <player>", NamedTextColor.RED));
            return;
        }
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (!island.isBanned(targetUUID)) {
            player.sendMessage(Component.text("⚠ " + targetName + " is not banned.", NamedTextColor.YELLOW));
            return;
        }

        island.unbanPlayer(targetUUID);
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("✅ " + targetName + " has been unbanned from your island.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
    }

    private void handleBanList(Player player) {
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        if (island.getBannedPlayers().isEmpty()) {
            player.sendMessage(Component.text("✅ There are no banned players on your island.", NamedTextColor.GREEN));
            return;
        }
        player.sendMessage(Component.text("🚫 Island Ban List:", NamedTextColor.RED));
        int i = 1;
        for (UUID bannedId : island.getBannedPlayers()) {
            String name = Bukkit.getOfflinePlayer(bannedId).getName();
            player.sendMessage(Component.text(" " + i + ". " + (name != null ? name : bannedId.toString()), NamedTextColor.GRAY));
            i++;
        }
    }

    // ==================== Trust ====================

    private void handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is trust <player>", NamedTextColor.RED));
            return;
        }
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ Only the island owner can trust players!", NamedTextColor.RED));
            return;
        }
        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("❌ You are already the owner of this island!", NamedTextColor.RED));
            return;
        }
        if (island.isTrusted(targetUUID)) {
            player.sendMessage(Component.text("⚠ " + targetName + " is already trusted.", NamedTextColor.YELLOW));
            return;
        }

        int maxTrusted = plugin.getConfigManager().getSettings().getMaxTrustedSize();
        if (island.getTrustedPlayers().size() >= maxTrusted) {
            player.sendMessage(Component.text("❌ Your trusted list is full (Max " + maxTrusted + ")!", NamedTextColor.RED));
            return;
        }

        island.addTrusted(targetUUID);
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("🤝 " + targetName + " has been added to your trusted list.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        
        Player targetOnline = Bukkit.getPlayer(targetUUID);
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(Component.text("🤝 You have been trusted on " + player.getName() + "'s island!", NamedTextColor.GREEN));
            targetOnline.playSound(targetOnline.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
    }

    private void handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /is untrust <player>", NamedTextColor.RED));
            return;
        }
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        String targetName = args[1];
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (!island.isTrusted(targetUUID)) {
            player.sendMessage(Component.text("⚠ " + targetName + " is not trusted.", NamedTextColor.YELLOW));
            return;
        }

        island.removeTrusted(targetUUID);
        plugin.getIslandManager().saveIslands();
        player.sendMessage(Component.text("❌ " + targetName + " has been removed from your trusted list.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
    }

    private void handleTrustList(Player player) {
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }
        if (island.getTrustedPlayers().isEmpty()) {
            player.sendMessage(Component.text("✅ There are no players on your trusted list.", NamedTextColor.GREEN));
            return;
        }
        player.sendMessage(Component.text("🤝 Trusted Players:", NamedTextColor.GREEN));
        int i = 1;
        for (UUID trustedId : island.getTrustedPlayers()) {
            String name = Bukkit.getOfflinePlayer(trustedId).getName();
            player.sendMessage(Component.text(" " + i + ". " + (name != null ? name : trustedId.toString()), NamedTextColor.GRAY));
            i++;
        }
    }

    // ==================== Co-op ====================

    private void handleCoop(Player player, String[] args) {
        Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("❌ You don't have an island!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            player.sendMessage(Component.text("  🤝 Co-op Commands", NamedTextColor.AQUA));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
            sendHelpLine(player, "/is coop add <player> [minutes]", "Grant temporary co-op access (default: 60 min)");
            sendHelpLine(player, "/is coop remove <player>", "Revoke a player's co-op access");
            sendHelpLine(player, "/is coop list", "View all active co-op players");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add": {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /is coop add <player> [minutes]", NamedTextColor.RED));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(Component.text("❌ Player '" + args[2] + "' is not online!", NamedTextColor.RED));
                    return;
                }
                if (target.equals(player)) {
                    player.sendMessage(Component.text("❌ You can't co-op yourself!", NamedTextColor.RED));
                    return;
                }
                if (island.isMemberOrOwner(target.getUniqueId())) {
                    player.sendMessage(Component.text("❌ " + target.getName() + " is already a team member!", NamedTextColor.RED));
                    return;
                }
                if (island.isBanned(target.getUniqueId())) {
                    player.sendMessage(Component.text("❌ " + target.getName() + " is banned from your island!", NamedTextColor.RED));
                    return;
                }

                long durationMs = me.rspaae.grandseas.model.Island.COOP_DEFAULT_DURATION_MS;
                if (args.length >= 4) {
                    try {
                        long minutes = Long.parseLong(args[3]);
                        if (minutes < 1 || minutes > 1440) {
                            player.sendMessage(Component.text("❌ Duration must be between 1 and 1440 minutes.", NamedTextColor.RED));
                            return;
                        }
                        durationMs = java.util.concurrent.TimeUnit.MINUTES.toMillis(minutes);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid duration. Use a number (in minutes).", NamedTextColor.RED));
                        return;
                    }
                }

                island.addCoop(target.getUniqueId(), durationMs);
                plugin.getIslandManager().saveIslands();

                long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMs);
                player.sendMessage(Component.text("✅ " + target.getName() + " has been granted co-op access for " + minutes + " minute(s).", NamedTextColor.GREEN));
                target.sendMessage(Component.text("🤝 You have been granted co-op access to " + player.getName() + "'s island for " + minutes + " minute(s)!", NamedTextColor.GREEN));

                // Audit log
                plugin.getIslandManager().addAuditLog(island.getOwner(),
                        new me.rspaae.grandseas.model.AuditLog(target.getUniqueId(), target.getName(),
                                me.rspaae.grandseas.model.AuditLog.Action.COOP_ADD,
                                minutes + " min granted by " + player.getName()));
                break;
            }
            case "remove": {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /is coop remove <player>", NamedTextColor.RED));
                    return;
                }
                // Support offline players for removal
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                if (!island.isCoop(target.getUniqueId())) {
                    player.sendMessage(Component.text("❌ " + args[2] + " is not a co-op player on your island!", NamedTextColor.RED));
                    return;
                }
                island.removeCoop(target.getUniqueId());
                plugin.getIslandManager().saveIslands();
                player.sendMessage(Component.text("✅ Co-op access for " + args[2] + " has been revoked.", NamedTextColor.GREEN));

                // Notify the removed player if online
                Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage(Component.text("⚠ Your co-op access to " + player.getName() + "'s island has been revoked.", NamedTextColor.YELLOW));
                    // Kick from island if currently there
                    if (onlineTarget.getWorld().getName().equals(plugin.getIslandManager().getWorldName())) {
                        Island currentIsland = plugin.getIslandManager().getIslandAt(onlineTarget.getLocation());
                        if (currentIsland != null && currentIsland.getOwner().equals(island.getOwner())) {
                            onlineTarget.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                            onlineTarget.setWorldBorder(null);
                        }
                    }
                }

                // Audit log
                plugin.getIslandManager().addAuditLog(island.getOwner(),
                        new me.rspaae.grandseas.model.AuditLog(target.getUniqueId(),
                                target.getName() != null ? target.getName() : args[2],
                                me.rspaae.grandseas.model.AuditLog.Action.COOP_REMOVE,
                                "Removed by " + player.getName()));
                break;
            }
            case "list": {
                if (island.getCoopPlayers().isEmpty()) {
                    player.sendMessage(Component.text("✅ There are no active co-op players on your island.", NamedTextColor.GREEN));
                    return;
                }
                player.sendMessage(Component.text("🤝 Active Co-op Players:", NamedTextColor.AQUA));
                long now = System.currentTimeMillis();
                int i = 1;
                for (java.util.Map.Entry<UUID, Long> entry : island.getCoopPlayers().entrySet()) {
                    long remaining = entry.getValue() - now;
                    if (remaining <= 0) continue;
                    long mins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remaining);
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    player.sendMessage(Component.text(" " + i + ". ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(name != null ? name : "Unknown", NamedTextColor.WHITE))
                            .append(Component.text(" — " + mins + " min remaining", NamedTextColor.GRAY)));
                    i++;
                }
                break;
            }
            default:
                player.sendMessage(Component.text("Usage: /is coop <add|remove|list>", NamedTextColor.RED));
        }
    }

    // ==================== Help ====================

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  🏝 GrandSeas Commands", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        player.sendMessage(Component.empty());

        sendHelpLine(player, "/is", "Teleport to your island");
        sendHelpLine(player, "/is create", "Create a new island");
        sendHelpLine(player, "/is settings", "Open Island Control Panel");
        sendHelpLine(player, "/is upgrades", "Open Upgrades Menu");
        sendHelpLine(player, "/is balance", "View island bank balance");
        sendHelpLine(player, "/is chat", "Toggle island-only chat");
        sendHelpLine(player, "/is rename <name>", "Rename your island");
        sendHelpLine(player, "/is pay <player> <amount>", "Transfer bank funds");
        sendHelpLine(player, "/is transfer <player>", "Transfer island ownership");
        sendHelpLine(player, "/is level", "View island stats");
        sendHelpLine(player, "/is reload", "Refresh island points");
        sendHelpLine(player, "/is top", "View leaderboard");
        sendHelpLine(player, "/is visit <player>", "Visit an island");
        sendHelpLine(player, "/is trust <player>", "Trust a player");
        sendHelpLine(player, "/is untrust <player>", "Untrust a player");
        sendHelpLine(player, "/is trustlist", "View trusted players");
        sendHelpLine(player, "/is coop add <player> [min]", "Grant temporary co-op access");
        sendHelpLine(player, "/is coop remove <player>", "Revoke co-op access");
        sendHelpLine(player, "/is coop list", "View active co-op players");
        sendHelpLine(player, "/is ban <player>", "Ban player from island");
        sendHelpLine(player, "/is unban <player>", "Unban player");
        sendHelpLine(player, "/is banlist", "View banned players");
        sendHelpLine(player, "/is delete", "Delete your island");
        player.sendMessage(Component.empty());

        sendHelpLine(player, "/is team invite <player>", "Invite a player to your team");
        sendHelpLine(player, "/is team accept", "Accept a team invitation");
        sendHelpLine(player, "/is team deny", "Decline a team invitation");
        sendHelpLine(player, "/is team kick <player>", "Kick a member from your team");
        sendHelpLine(player, "/is team leave", "Leave your current team");
        sendHelpLine(player, "/is team promote <player>", "Transfer team ownership");

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
    }

    private void sendHelpLine(Player player, String cmd, String desc) {
        player.sendMessage(
                Component.text(" " + cmd, NamedTextColor.GOLD)
                        .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(desc, NamedTextColor.GRAY))
        );
    }

    // ==================== Tab Complete ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        Player player = (Player) sender;

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String[] subs = {"create", "settings", "upgrades", "balance", "chat", "rename", "pay", "transfer", "visit", "trust", "untrust", "trustlist", "coop", "ban", "unban", "banlist", "delete", "team", "top", "reload", "info", "sethome", "help"};
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            String input = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            String input = args[1].toLowerCase();
            String[] subs = {"invite", "accept", "deny", "kick", "leave", "promote"};
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("team")) {
            String sub = args[1].toLowerCase();
            String input = args[2].toLowerCase();

            if (sub.equals("invite")) {
                // Show online players who don't have an island
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player) && online.getName().toLowerCase().startsWith(input)) {
                        if (plugin.getIslandManager().getIsland(online.getUniqueId()) == null) {
                            completions.add(online.getName());
                        }
                    }
                }
            } else if (sub.equals("kick") || sub.equals("promote")) {
                // Show island members
                Island island = plugin.getIslandManager().getIslandByOwner(player.getUniqueId());
                if (island != null) {
                    for (UUID memberId : island.getMembers()) {
                        String name = Bukkit.getOfflinePlayer(memberId).getName();
                        if (name != null && name.toLowerCase().startsWith(input)) {
                            completions.add(name);
                        }
                    }
                }
            }
        }

        return completions;
    }
}
