package me.rspaae.grandseas.command;

import me.rspaae.grandseas.GrandSeas;
import me.rspaae.grandseas.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class IslandAdminCommand implements TabExecutor {

    private final GrandSeas plugin;

    public IslandAdminCommand(GrandSeas plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grandseas.admin")) {
            sender.sendMessage(Component.text("⛔ You don't have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "tp":
            case "teleport":
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /isadmin tp <player>", NamedTextColor.RED)); return true; }
                handleTp(sender, args[1]);
                break;
            case "delete":
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /isadmin delete <player>", NamedTextColor.RED)); return true; }
                handleDelete(sender, args[1]);
                break;
            case "info":
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /isadmin info <player>", NamedTextColor.RED)); return true; }
                handleInfo(sender, args[1]);
                break;
            case "setpoints":
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /isadmin setpoints <player> <amount>", NamedTextColor.RED)); return true; }
                handleSetPoints(sender, args[1], args[2]);
                break;
            case "eco":
                if (args.length < 4) { sender.sendMessage(Component.text("Usage: /isadmin eco <give|take|set> <player> <amount>", NamedTextColor.RED)); return true; }
                handleEco(sender, args[1], args[2], args[3]);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "backup":
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /isadmin backup <player>", NamedTextColor.RED)); return true; }
                handleBackup(sender, args[1]);
                break;
            case "bypass":
                handleBypass(sender);
                break;
            case "audit":
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /isadmin audit <player>", NamedTextColor.RED)); return true; }
                handleAudit(sender, args[1]);
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void handleTp(CommandSender sender, String targetName) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can teleport."); return; }
        Player admin = (Player) sender;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            admin.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }

        plugin.getIslandManager().teleportToIsland(admin, island);
        admin.sendMessage(Component.text("✅ Teleported to " + targetName + "'s island.", NamedTextColor.GREEN));
        admin.playSound(admin.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void handleDelete(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }

        plugin.getIslandManager().deleteIsland(island);
        sender.sendMessage(Component.text("✅ " + targetName + "'s island has been deleted.", NamedTextColor.GREEN));

        // Notify online player
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            online.sendMessage(Component.text("⚠ Your island has been deleted by an Admin.", NamedTextColor.YELLOW));
            online.playSound(online.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1f);
        }
    }

    private void handleInfo(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Admin Info: " + island.getName(), NamedTextColor.AQUA, TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(" 👤 Owner: ", NamedTextColor.GRAY).append(Component.text(targetName, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" 🎨 Theme: ", NamedTextColor.GRAY).append(Component.text(island.getTheme().getDisplayName(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text(" 📌 Level: ", NamedTextColor.GRAY).append(Component.text(island.getBorderLevel() + "/" + Island.getMaxBorderLevel(), NamedTextColor.AQUA)));
        sender.sendMessage(Component.text(" 📏 Border: ", NamedTextColor.GRAY).append(Component.text(island.getBorderSize() + "x" + island.getBorderSize(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" ⭐ Points: ", NamedTextColor.GRAY).append(Component.text(String.format("%,d", island.getPoints()), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text(" 💰 Balance: ", NamedTextColor.GRAY).append(Component.text(String.format("%,.1f", island.getBalance()), NamedTextColor.GOLD)));
        sender.sendMessage(Component.text(" 👥 Members: ", NamedTextColor.GRAY).append(Component.text(island.getMembers().size() + 1 + "", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text(" 🚫 Banned: ", NamedTextColor.GRAY).append(Component.text(island.getBannedPlayers().size() + " player(s)", NamedTextColor.RED)));
        if (island.getCenter() != null) {
            sender.sendMessage(Component.text(" 📍 Center: ", NamedTextColor.GRAY).append(Component.text(
                    island.getCenter().getBlockX() + ", " + island.getCenter().getBlockY() + ", " + island.getCenter().getBlockZ(), NamedTextColor.WHITE)));
        }
        sender.sendMessage(Component.text(" 🚪 Visitor: ", NamedTextColor.GRAY).append(Component.text(island.isAllowVisitors() ? "Allowed" : "Closed", island.isAllowVisitors() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA));
    }

    private void handleSetPoints(CommandSender sender, String targetName, String amountStr) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }
        try {
            long amount = Long.parseLong(amountStr);
            island.setPoints(amount);
            plugin.getIslandManager().saveIslands();
            sender.sendMessage(Component.text("✅ " + targetName + "'s island points set to " + String.format("%,d", amount) + ".", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("❌ Invalid number!", NamedTextColor.RED));
        }
    }

    private void handleEco(CommandSender sender, String action, String targetName, String amountStr) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < 0) throw new NumberFormatException();

            if (action.equalsIgnoreCase("give")) {
                island.addBalance(amount);
                sender.sendMessage(Component.text("✅ Successfully added $" + String.format("%,.1f", amount) + " to " + targetName + "'s bank", NamedTextColor.GREEN));
            } else if (action.equalsIgnoreCase("take")) {
                if (island.getBalance() >= amount) {
                    island.removeBalance(amount);
                    sender.sendMessage(Component.text("✅ Successfully removed $" + String.format("%,.1f", amount) + " from " + targetName + "'s bank", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("❌ " + targetName + "'s island bank balance is insufficient!", NamedTextColor.RED));
                }
            } else if (action.equalsIgnoreCase("set")) {
                island.setBalance(amount);
                sender.sendMessage(Component.text("✅ Successfully set " + targetName + "'s bank balance to $" + String.format("%,.1f", amount), NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("❌ Invalid action. Use give/take/set.", NamedTextColor.RED));
                return;
            }
            plugin.getIslandManager().saveIslands();
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().loadFiles();
        sender.sendMessage(Component.text("✅ GrandSeas config reloaded successfully!", NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender, String[] args) {
        java.util.Collection<me.rspaae.grandseas.model.Island> all =
                plugin.getIslandManager().getAllIslands().values();
        if (all.isEmpty()) {
            sender.sendMessage(Component.text("ℹ No islands found.", NamedTextColor.GRAY));
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try { page = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
        }
        int perPage = 10;
        java.util.List<me.rspaae.grandseas.model.Island> sorted = new java.util.ArrayList<>(all);
        sorted.sort((a, b) -> Long.compare(b.getPoints(), a.getPoints()));

        int totalPages = (int) Math.ceil(sorted.size() / (double) perPage);
        page = Math.min(page, totalPages);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, sorted.size());

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text("  All Islands — Page " + page + "/" + totalPages, NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        for (int i = start; i < end; i++) {
            me.rspaae.grandseas.model.Island island = sorted.get(i);
            String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
            if (ownerName == null) ownerName = island.getOwner().toString().substring(0, 8);
            sender.sendMessage(Component.text(" " + (i + 1) + ". ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(island.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" (" + ownerName + ")", NamedTextColor.GRAY))
                    .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%,d pts", island.getPoints()), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text((island.getMembers().size() + 1) + " members", NamedTextColor.AQUA)));
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
    }

    private void handleBackup(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }
        java.io.File file = plugin.getIslandManager().backupIsland(island);
        if (file != null) {
            sender.sendMessage(Component.text("✅ Backup created: " + file.getName(), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("❌ Failed to create backup! Check console for details.", NamedTextColor.RED));
        }
    }

    private void handleBypass(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle bypass mode.");
            return;
        }
        Player admin = (Player) sender;
        boolean active = plugin.getIslandManager().toggleBypass(admin.getUniqueId());
        if (active) {
            admin.sendMessage(Component.text("⚠ Admin bypass mode ENABLED. You can now build/interact on any island.", NamedTextColor.YELLOW));
        } else {
            admin.sendMessage(Component.text("✅ Admin bypass mode DISABLED. Island protections are now active for you.", NamedTextColor.GREEN));
        }
    }

    private void handleAudit(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Island island = plugin.getIslandManager().getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text("❌ " + targetName + " doesn't have an island.", NamedTextColor.RED));
            return;
        }
        java.util.List<me.rspaae.grandseas.model.AuditLog> logs =
                plugin.getIslandManager().getAuditLog(island.getOwner());
        if (logs.isEmpty()) {
            sender.sendMessage(Component.text("ℹ No audit entries found for " + targetName + "'s island.", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text("  Audit Log: " + island.getName(), NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        int shown = Math.min(10, logs.size());
        for (int i = 0; i < shown; i++) {
            me.rspaae.grandseas.model.AuditLog log = logs.get(i);
            NamedTextColor color = switch (log.getAction()) {
                case BUILD -> NamedTextColor.GREEN;
                case BREAK -> NamedTextColor.RED;
                case VISIT -> NamedTextColor.AQUA;
                case COOP_ADD, COOP_REMOVE, COOP_EXPIRE -> NamedTextColor.YELLOW;
                default -> NamedTextColor.GRAY;
            };
            sender.sendMessage(Component.text(" [" + log.getFormattedTime() + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(log.getActorName() + " ", NamedTextColor.WHITE))
                    .append(Component.text(log.getAction().name(), color))
                    .append(Component.text(log.getDetail().isEmpty() ? "" : " — " + log.getDetail(), NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text("  GrandSeas Admin Commands", NamedTextColor.RED, TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text(" /isadmin tp <player>", NamedTextColor.YELLOW).append(Component.text(" — Teleport to island", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin delete <player>", NamedTextColor.YELLOW).append(Component.text(" — Delete an island", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin info <player>", NamedTextColor.YELLOW).append(Component.text(" — Island details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin setpoints <player> <n>", NamedTextColor.YELLOW).append(Component.text(" — Set island points", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin eco <give|take|set> <p> <n>", NamedTextColor.YELLOW).append(Component.text(" — Admin island bank", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin list [page]", NamedTextColor.YELLOW).append(Component.text(" — List all islands", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin backup <player>", NamedTextColor.YELLOW).append(Component.text(" — Backup island data", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin bypass", NamedTextColor.YELLOW).append(Component.text(" — Toggle protection bypass", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin audit <player>", NamedTextColor.YELLOW).append(Component.text(" — View island audit log", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin reload", NamedTextColor.YELLOW).append(Component.text(" — Reload config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("grandseas.admin")) return completions;

        if (args.length == 1) {
            String[] subs = {"tp", "delete", "info", "setpoints", "eco", "list", "backup", "bypass", "audit", "reload"};
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("eco")) {
                String[] subs = {"give", "take", "set"};
                for (String sub : subs) {
                    if (sub.startsWith(args[1].toLowerCase())) completions.add(sub);
                }
            } else if (!args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("bypass")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("eco")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
