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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text("  GrandSeas Admin Commands", NamedTextColor.RED, TextDecoration.BOLD));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
        sender.sendMessage(Component.text(" /isadmin tp <player>", NamedTextColor.YELLOW).append(Component.text(" — Teleport to island", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin delete <player>", NamedTextColor.YELLOW).append(Component.text(" — Delete an island", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin info <player>", NamedTextColor.YELLOW).append(Component.text(" — Island details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin setpoints <player> <n>", NamedTextColor.YELLOW).append(Component.text(" — Set island points", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin eco <give|take|set> <p> <n>", NamedTextColor.YELLOW).append(Component.text(" — Admin island bank", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(" /isadmin reload", NamedTextColor.YELLOW).append(Component.text(" — Reload config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("grandseas.admin")) return completions;

        if (args.length == 1) {
            String[] subs = {"tp", "delete", "info", "setpoints", "eco", "reload"};
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("eco")) {
                String[] subs = {"give", "take", "set"};
                for (String sub : subs) {
                    if (sub.startsWith(args[1].toLowerCase())) completions.add(sub);
                }
            } else if (!args[0].equalsIgnoreCase("reload")) {
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
